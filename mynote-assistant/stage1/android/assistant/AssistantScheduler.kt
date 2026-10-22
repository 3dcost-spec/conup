package com.cmx.mynote.assistant

import java.text.Normalizer
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

/**
 * 리포트 ① 절 → 일정 제안, 자연어 한 줄 → 일정 제안, 승인 → 폰 캘린더 쓰기.
 * 외부 전송 없음. AI 호출 없음(규칙 해석만). 애매한 건 제안하지 않고 질문 목록으로 돌려준다.
 */
class AssistantScheduler(private val dao: CalendarEventDao, private val writer: CalendarWriter) {

    private val zone = ZoneId.of("Asia/Seoul")

    data class Outcome(val proposed: Int, val questions: List<String>)

    /** ReportWorker 가 일일 리포트를 저장한 직후 호출 */
    suspend fun proposeFromReport(reportMarkdown: String, reportKey: String, now: LocalDateTime, cardIdsByRef: Map<Int, String>): Outcome {
        var n = 0
        val questions = mutableListOf<String>()
        for (it in ReportScheduleExtractor.extract(reportMarkdown, now)) {
            if (!it.proposeToCalendar) { questions += "${it.label} — ${if (it.when.ambiguities.isEmpty()) it.status else it.when.ambiguities.joinToString()}"; continue }
            val refs = Regex("""\[자료(\d+)]""").findAll(it.sourceRefs).mapNotNull { m -> cardIdsByRef[m.groupValues[1].toInt()] }.joinToString(",")
            if (insertProposal(it.when, sourceCards = refs, reportKey = reportKey)) n++
        }
        return Outcome(n, questions)
    }

    /** 앱 비서 탭·카톡 창구에서 받은 한 줄. 되물을 게 있으면 질문 문자열을 돌려준다 */
    suspend fun proposeFromText(text: String, now: LocalDateTime = LocalDateTime.now(zone)): String {
        val r = KrDateTimeParser.parse(text, now)
        if (r.date == null) return "날짜를 못 찾았습니다. 언제인지 알려주세요."
        if (!r.isUsable()) return "확인할 것: " + r.ambiguities.joinToString(" / ")
        return if (insertProposal(r, "", null)) "제안: ${describe(r)} — 등록할까요?" else "이미 같은 일정이 있습니다."
    }

    /** 형이 '등록' 누른 제안을 폰 캘린더에 쓴다 */
    suspend fun confirmAndWrite(uid: String, calendarId: Long): String {
        val e = dao.get(uid) ?: return "제안을 찾을 수 없습니다"
        val clash = writer.conflicts(e.dtStart, e.dtEnd)
        val (id, err) = writer.upsert(e, calendarId)
        if (id == null) { dao.update(e.copy(status = CalendarEvent.Status.CONFIRMED, targetCalendarId = calendarId, updatedAt = now())); return err ?: "실패" }
        dao.update(e.copy(status = CalendarEvent.Status.SYNCED, targetCalendarId = calendarId, providerEventId = id, updatedAt = now()))
        return "등록 완료" + if (clash.isNotEmpty()) " (겹침: ${clash.take(2).joinToString()})" else ""
    }

    suspend fun reject(uid: String) { dao.get(uid)?.let { dao.update(it.copy(status = CalendarEvent.Status.REJECTED, updatedAt = now())) } }

    private suspend fun insertProposal(r: KrDateTimeParser.Result, sourceCards: String, reportKey: String?): Boolean {
        val start = if (r.allDay || r.start == null) r.date.atStartOfDay(zone) else r.date.atTime(r.start).atZone(zone)
        val end = when {
            r.allDay || r.start == null -> start.plusDays(1)
            r.end != null -> r.date.atTime(r.end).atZone(zone)
            else -> start.plusHours(1)
        }
        val title = r.title.ifBlank { "일정" }
        val key = "${start.toLocalDateTime()}|" + Normalizer.normalize(title, Normalizer.Form.NFC).replace(Regex("\\s+"), "")
        val e = CalendarEvent(
            uid = "mynote-" + UUID.randomUUID(), dedupKey = key, title = title,
            dtStart = start.toInstant().toEpochMilli(), dtEnd = end.toInstant().toEpochMilli(), allDay = r.allDay || r.start == null,
            caseNo = Regex("""\d{4}[가-힣]{1,3}\d{2,7}""").find(title)?.value,
            sourceCardIds = sourceCards, sourceReportKey = reportKey,
        )
        return dao.insertIgnore(e) != -1L
    }

    private fun describe(r: KrDateTimeParser.Result) =
        "${r.date.monthValue}/${r.date.dayOfMonth}(${"월화수목금토일"[r.date.dayOfWeek.value - 1]}) " +
        (r.start?.let { "%02d:%02d ".format(it.hour, it.minute) } ?: "종일 ") + r.title

    private fun now() = System.currentTimeMillis()
}
