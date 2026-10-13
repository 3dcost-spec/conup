package com.cmx.mynote.calendar

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.util.TimeZone
import java.util.UUID

/**
 * 마이노트 비서 0단계 — Android CalendarContract 왕복 실측.
 *
 * 전제: 폰에 DAVx⁵를 설치하고 네이버 CalDAV 계정(caldav.calendar.naver.com, 네이버ID + 앱 비밀번호)을
 * 추가하면 네이버 캘린더가 CalendarContract.Calendars 에 ACCOUNT_TYPE="bitfire.at.davdroid" 로 나타난다.
 * 구글 캘린더는 ACCOUNT_TYPE="com.google" 로 이미 있다. 이 코드는 둘 다 같은 API로 읽고 쓴다.
 *
 * 권한: AndroidManifest 에 READ_CALENDAR, WRITE_CALENDAR 추가 + 런타임 요청.
 *
 * 사용: 설정 화면 "비서 > 캘린더 실측" 버튼 → ProbeReport 를 화면과 Dropbox .sync/assistant/probe/ 에 기록.
 * 사용자 실제 일정은 읽기만 하고 건수만 남긴다. 테스트 일정은 제목 "[마이노트 실측]" 으로 만들고 끝에 삭제한다.
 */
class CalendarProviderProbe(private val context: Context) {

    data class CalendarInfo(val id: Long, val name: String, val account: String, val accountType: String,
                            val accessLevel: Int, val syncEvents: Boolean) {
        val writable get() = accessLevel >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR
        val isNaverViaDavx5 get() = accountType == "bitfire.at.davdroid"
        val isGoogle get() = accountType == "com.google"
    }
    data class Step(val step: String, val ok: Boolean, val info: Map<String, Any?> = emptyMap())
    data class ProbeReport(val steps: MutableList<Step> = mutableListOf()) { val pass get() = steps.all { it.ok } }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun listCalendars(): List<CalendarInfo> {
        val proj = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME, CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.SYNC_EVENTS)
        val out = mutableListOf<CalendarInfo>()
        context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, proj, null, null, null)?.use { c ->
            while (c.moveToNext()) out += CalendarInfo(c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: "",
                c.getString(3) ?: "", c.getInt(4), c.getInt(5) == 1)
        }
        return out
    }

    /** 앞으로 14일 일정 건수만 (내용은 기록하지 않음) */
    fun countUpcoming(calendarId: Long, days: Int = 14): Int {
        val now = System.currentTimeMillis()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(now.toString()).appendPath((now + days * 86_400_000L).toString()).build()
        context.contentResolver.query(uri, arrayOf(CalendarContract.Instances._ID),
            "${CalendarContract.Instances.CALENDAR_ID}=?", arrayOf(calendarId.toString()), null)?.use { return it.count }
        return -1
    }

    /** 생성 → 읽기 → 수정 → (keep=false면) 삭제. 반환: 보고서 */
    fun roundTrip(calendarId: Long, keep: Boolean = false): ProbeReport {
        val r = ProbeReport()
        val cr = context.contentResolver
        val start = System.currentTimeMillis() + 86_400_000L
        val uid = "mynote-probe-${UUID.randomUUID()}"
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, "[마이노트 실측] 캘린더 왕복 테스트")
            put(CalendarContract.Events.EVENT_LOCATION, "서울중앙지방법원")
            put(CalendarContract.Events.DESCRIPTION, "마이노트 비서 0단계 자동 생성. 삭제해도 됩니다.")
            put(CalendarContract.Events.DTSTART, start)
            put(CalendarContract.Events.DTEND, start + 3_600_000L)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.UID_2445, uid)   // iCal UID → PC CalDAV 와 같은 키
        }
        val eventUri = try { cr.insert(CalendarContract.Events.CONTENT_URI, values) } catch (e: Exception) {
            r.steps += Step("create_event", false, mapOf("error" to e.toString())); return r }
        if (eventUri == null) { r.steps += Step("create_event", false, mapOf("error" to "insert returned null")); return r }
        val eventId = ContentUris.parseId(eventUri)
        r.steps += Step("create_event", true, mapOf("eventId" to eventId, "uid" to uid))

        // 알림 D-0 30분 전
        runCatching {
            cr.insert(CalendarContract.Reminders.CONTENT_URI, ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, eventId); put(CalendarContract.Reminders.MINUTES, 30)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT) })
        }.onSuccess { r.steps += Step("add_reminder", it != null) }.onFailure { r.steps += Step("add_reminder", false, mapOf("error" to it.toString())) }

        // 읽기
        var title: String? = null; var dirty: Int = -1; var syncId: String? = null
        cr.query(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId),
            arrayOf(CalendarContract.Events.TITLE, CalendarContract.Events.DIRTY, CalendarContract.Events._SYNC_ID), null, null, null)?.use {
            if (it.moveToFirst()) { title = it.getString(0); dirty = it.getInt(1); syncId = it.getString(2) } }
        r.steps += Step("read_back", title?.startsWith("[마이노트 실측]") == true, mapOf("title" to title, "dirty" to dirty, "syncId" to syncId))

        // 수정
        val n = cr.update(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId),
            ContentValues().apply { put(CalendarContract.Events.TITLE, "[마이노트 실측] 캘린더 왕복 테스트 (수정됨)") }, null, null)
        r.steps += Step("update_event", n == 1)

        if (keep) r.steps += Step("delete_event", true, mapOf("skipped" to true, "note" to "DAVx⁵ 동기화 후 네이버 웹/PC caldav_probe --list-only 에서 보이는지 확인"))
        else r.steps += Step("delete_event", cr.delete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId), null, null) == 1)
        return r
    }
}
