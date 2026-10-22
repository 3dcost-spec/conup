package com.cmx.mynote.assistant;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 일일 리포트(DailyIssueReport) "## ① 중요 일정·약속·마감" 절에서 일정 후보를 뽑는다.
 * [확정] 항목만 캘린더 '제안'으로 만들고, [확인 필요]·날짜 불명 항목은 질문 목록으로 돌린다.
 * 리포트 본문 형식이 바뀌어도 줄 단위로만 읽으므로 실패 시 빈 목록을 돌려준다(예외 없음).
 */
public final class ReportScheduleExtractor {

    public static final class Item {
        public String raw, status, label, sourceRefs;
        public KrDateTimeParser.Result when;
        public boolean proposeToCalendar;   // true = 캘린더 제안, false = 사용자에게 확인
        @Override public String toString() { return "Item{" + status + " propose=" + proposeToCalendar + " " + when + " refs=" + sourceRefs + "}"; }
    }

    private static final Pattern SECTION = Pattern.compile("(?m)^##\\s*①[^\\n]*\\n([\\s\\S]*?)(?=^##\\s|\\z)");
    private static final Pattern LINE = Pattern.compile("^\\s*[-*•]\\s*(?:\\[(확정|잠정|확인\\s*필요)\\]\\s*)?(.+)$");
    private static final Pattern REFS = Pattern.compile("(\\[자료\\d+\\](?:\\s*,?\\s*\\[자료\\d+\\])*)|근거\\s*:\\s*(.+)$");

    private ReportScheduleExtractor() {}

    public static List<Item> extract(String reportMarkdown, LocalDateTime reportDayNow) {
        List<Item> out = new ArrayList<>();
        if (reportMarkdown == null) return out;
        Matcher sec = SECTION.matcher(reportMarkdown);
        if (!sec.find()) return out;
        for (String line : sec.group(1).split("\\n")) {
            Matcher lm = LINE.matcher(line);
            if (!lm.find()) continue;
            String body = lm.group(2).trim();
            if (body.isEmpty() || body.matches("^(없음|해당\\s*없음|-)$")) continue;
            Item it = new Item();
            it.raw = line.trim();
            it.status = lm.group(1) == null ? "미표기" : lm.group(1).replaceAll("\\s", " ");
            Matcher rf = REFS.matcher(body);
            StringBuilder refs = new StringBuilder();
            while (rf.find()) refs.append(rf.group().trim()).append(' ');
            it.sourceRefs = refs.toString().trim();
            String core = REFS.matcher(body).replaceAll("").replaceAll("\\s*[—–]\\s*$", "").trim();
            it.label = core;
            it.when = KrDateTimeParser.parse(core, reportDayNow);
            it.proposeToCalendar = "확정".equals(it.status) && it.when.isUsable() && !it.sourceRefs.isEmpty();
            out.add(it);
        }
        return out;
    }
}
