package com.cmx.mynote.assistant;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 한국어 일정 표현 → 날짜·시간. 안드로이드 의존 없음(JVM 단위 테스트 가능).
 * 규칙으로 확실한 것만 확정하고, 애매하면 ambiguities 에 사유를 남긴다. 호출부는 confidence < 0.8
 * 이거나 ambiguities 가 비어 있지 않으면 사용자에게 되묻는다. 날짜를 지어내지 않는다.
 */
public final class KrDateTimeParser {

    public static final class Result {
        public LocalDate date;          // null = 날짜 못 찾음
        public LocalTime start;         // null = 종일 또는 시간 미상
        public LocalTime end;           // null = 미상
        public boolean allDay;
        public double confidence;       // 0..1
        public final List<String> ambiguities = new ArrayList<>();
        public String title = "";       // 날짜·시간 표현을 뺀 나머지
        public boolean isUsable() { return date != null && confidence >= 0.8 && ambiguities.isEmpty(); }
        @Override public String toString() {
            return "Result{date=" + date + ", start=" + start + ", end=" + end + ", allDay=" + allDay +
                   ", conf=" + confidence + ", amb=" + ambiguities + ", title='" + title + "'}";
        }
    }

    private static final String DOW = "(월|화|수|목|금|토|일)(?:요일)?";
    private static final Pattern P_ISO   = Pattern.compile("(20\\d\\d)[-./](\\d{1,2})[-./](\\d{1,2})");
    private static final Pattern P_MD_KR = Pattern.compile("(?:(20\\d\\d)년\\s*)?(\\d{1,2})월\\s*(\\d{1,2})일");
    private static final Pattern P_MD_SL = Pattern.compile("(?<![\\d:])(\\d{1,2})/(\\d{1,2})(?![\\d/])");
    private static final Pattern P_REL   = Pattern.compile("(오늘|내일|모레|글피|어제)");
    private static final Pattern P_NDAYS = Pattern.compile("(\\d{1,3})\\s*일\\s*(?:후|뒤)");
    private static final Pattern P_NWEEK = Pattern.compile("(\\d{1,2})\\s*주\\s*(?:후|뒤)");
    private static final Pattern P_WEEK  = Pattern.compile("(이번\\s*주|금주|다음\\s*주|차주|다다음\\s*주)\\s*" + DOW);
    /** 요일 단독은 "화요일"처럼 '요일'까지 있어야 인정('25일', '기일'의 '일' 오인 방지) */
    private static final Pattern P_DOW   = Pattern.compile("(?<![가-힣\\d])(월|화|수|목|금|토|일)요일");
    private static final Pattern P_DAYONLY = Pattern.compile("(?<![월\\d])(\\d{1,2})일(?!\\s*(?:후|뒤|간|동안))");
    private static final Pattern P_HHMM  = Pattern.compile("(?<!\\d)([01]?\\d|2[0-3]):([0-5]\\d)(?!\\d)");
    private static final Pattern P_KTIME = Pattern.compile(
        "(오전|오후|아침|낮|점심|저녁|밤|새벽)?\\s*(\\d{1,2})\\s*시\\s*(?:(\\d{1,2})\\s*분|(반))?");
    private static final Pattern P_RANGE_END = Pattern.compile("(?:~|-|부터)\\s*(오전|오후|저녁|밤)?\\s*(\\d{1,2})(?:\\s*시|:(\\d\\d))\\s*(?:(\\d{1,2})\\s*분|(반))?\\s*(?:까지)?");
    private static final Pattern P_DURATION = Pattern.compile("(\\d{1,2})\\s*시간(?:\\s*(\\d{1,2})\\s*분)?\\s*(?:동안|짜리)?");
    private static final Pattern P_NOON  = Pattern.compile("(정오|자정)");
    private static final Pattern P_ALLDAY = Pattern.compile("(종일|하루\\s*종일|온종일)");

    private KrDateTimeParser() {}

    public static Result parse(String text, LocalDateTime now) {
        Result r = new Result();
        String s = text == null ? "" : text.trim();
        List<int[]> used = new ArrayList<>();
        LocalDate today = now.toLocalDate();
        double conf = 0;

        // ---------- 날짜 ----------
        Matcher m;
        if ((m = P_ISO.matcher(s)).find()) {
            r.date = safeDate(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), r);
            used.add(span(m)); conf = 1.0;
        } else if ((m = P_MD_KR.matcher(s)).find()) {
            int y = m.group(1) != null ? Integer.parseInt(m.group(1)) : today.getYear();
            r.date = safeDate(y, Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)), r);
            if (r.date != null && m.group(1) == null && r.date.isBefore(today.minusDays(30))) r.date = r.date.plusYears(1);
            used.add(span(m)); conf = 1.0;
        } else if ((m = P_WEEK.matcher(s)).find()) {
            String w = m.group(1).replaceAll("\\s", "");
            int add = w.startsWith("다다음") ? 2 : (w.startsWith("다음") || w.equals("차주")) ? 1 : 0;
            LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(add);
            r.date = monday.plusDays(dowIndex(m.group(2)));
            used.add(span(m)); conf = 1.0;
        } else if ((m = P_REL.matcher(s)).find()) {
            switch (m.group(1)) {
                case "오늘": r.date = today; break;
                case "내일": r.date = today.plusDays(1); break;
                case "모레": r.date = today.plusDays(2); break;
                case "글피": r.date = today.plusDays(3); break;
                case "어제": r.date = today.minusDays(1); break;
            }
            used.add(span(m)); conf = 1.0;
        } else if ((m = P_NDAYS.matcher(s)).find()) {
            r.date = today.plusDays(Integer.parseInt(m.group(1))); used.add(span(m)); conf = 1.0;
        } else if ((m = P_NWEEK.matcher(s)).find()) {
            r.date = today.plusWeeks(Integer.parseInt(m.group(1))); used.add(span(m)); conf = 0.9;
        } else if ((m = P_MD_SL.matcher(s)).find()) {
            r.date = safeDate(today.getYear(), Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), r);
            if (r.date != null && r.date.isBefore(today.minusDays(30))) r.date = r.date.plusYears(1);
            used.add(span(m)); conf = 0.9;
        } else if ((m = P_DOW.matcher(s)).find()) {
            DayOfWeek d = DayOfWeek.of(dowIndex(m.group(1)) + 1);
            r.date = today.with(TemporalAdjusters.nextOrSame(d));
            if (r.date.equals(today)) r.ambiguities.add("요일만 있음: 오늘인지 다음 주인지 불명");
            used.add(span(m)); conf = 0.85;
        } else if ((m = P_DAYONLY.matcher(s)).find()) {
            int d = Integer.parseInt(m.group(1));
            LocalDate cand = safeDate(today.getYear(), today.getMonthValue(), d, r);
            if (cand != null && cand.isBefore(today)) cand = safeDate(today.plusMonths(1).getYear(), today.plusMonths(1).getMonthValue(), d, r);
            r.date = cand; used.add(span(m)); conf = 0.8;
            r.ambiguities.add("일자만 있음: " + (cand == null ? "?" : cand.getMonthValue() + "월") + "로 가정");
        }

        // ---------- 시간 ----------
        if ((m = P_ALLDAY.matcher(s)).find()) { r.allDay = true; used.add(span(m)); }
        boolean pmGiven = false; String period = null;
        if ((m = P_HHMM.matcher(s)).find() && !overlaps(used, span(m))) {
            r.start = LocalTime.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
            used.add(span(m)); pmGiven = true;
            Matcher pre = Pattern.compile("(오전|오후|저녁|밤|아침|새벽)\\s*$").matcher(s.substring(0, m.start()));
            if (pre.find()) { period = pre.group(1); used.add(new int[]{pre.start(), pre.end()}); r.start = applyPeriod(r.start.getHour(), r.start.getMinute(), period); }
        } else if ((m = P_KTIME.matcher(s)).find()) {
            period = m.group(1);
            int h = Integer.parseInt(m.group(2));
            int min = m.group(3) != null ? Integer.parseInt(m.group(3)) : (m.group(4) != null ? 30 : 0);
            if (h > 24 || min > 59) { r.ambiguities.add("시간 범위 오류: " + m.group()); }
            else {
                if (period != null) { r.start = applyPeriod(h, min, period); pmGiven = true; }
                else if (h >= 13 || h == 0) { r.start = LocalTime.of(h % 24, min); pmGiven = true; }
                else if (h <= 7) { r.start = LocalTime.of(h + 12, min); r.ambiguities.add("오전/오후 미표기: " + h + "시를 오후로 가정"); }
                else if (h == 12) { r.start = LocalTime.of(12, min); }
                else { r.start = LocalTime.of(h, min); } // 8~11시는 업무시간 오전으로 본다
                used.add(span(m));
            }
        } else if ((m = P_NOON.matcher(s)).find()) {
            r.start = m.group(1).equals("정오") ? LocalTime.NOON : LocalTime.MIDNIGHT; used.add(span(m));
        }

        // 종료 시간: "~ 5시까지", "2시간"
        if (r.start != null) {
            Matcher e = P_RANGE_END.matcher(s);
            while (e.find()) {
                if (overlapsStart(used, e.start())) continue;
                int h = Integer.parseInt(e.group(2));
                int min = e.group(3) != null ? Integer.parseInt(e.group(3)) : e.group(4) != null ? Integer.parseInt(e.group(4)) : e.group(5) != null ? 30 : 0;
                LocalTime end = e.group(1) != null ? applyPeriod(h, min, e.group(1)) : LocalTime.of(h % 24, min);
                if (end.isBefore(r.start) && h < 12) end = end.plusHours(12);
                r.end = end; used.add(span(e)); break;
            }
            if (r.end == null && (m = P_DURATION.matcher(s)).find()) {
                int mins = Integer.parseInt(m.group(1)) * 60 + (m.group(2) != null ? Integer.parseInt(m.group(2)) : 0);
                r.end = r.start.plusMinutes(mins); used.add(span(m));
            }
        }

        if (r.date == null) {
            if (r.start != null) r.ambiguities.add("시간만 있고 날짜 없음");
            r.confidence = 0; r.title = cleanTitle(s, used); return r;
        }
        if (r.start == null && !r.allDay) r.allDay = true; // 시간 없으면 종일 일정으로
        if (r.date.isBefore(today)) r.ambiguities.add("과거 날짜: " + r.date);
        if (r.start != null && r.date.equals(today) && r.start.isBefore(now.toLocalTime())) r.ambiguities.add("오늘 이미 지난 시간");
        r.confidence = conf;
        r.title = cleanTitle(s, used);
        return r;
    }

    private static LocalTime applyPeriod(int h, int min, String period) {
        switch (period) {
            case "오후": case "저녁": case "밤":
                if (h < 12) h += 12; if (period.equals("밤") && h == 24) h = 0; break;
            case "낮": case "점심":
                if (h <= 6) h += 12; break;
            case "오전": case "아침": case "새벽":
                if (h == 12) h = 0; break;
        }
        return LocalTime.of(h % 24, min);
    }

    private static int dowIndex(String k) { return "월화수목금토일".indexOf(k.charAt(0)); }

    private static LocalDate safeDate(int y, int mo, int d, Result r) {
        try { return LocalDate.of(y, mo, d); } catch (Exception e) { r.ambiguities.add("없는 날짜: " + y + "-" + mo + "-" + d); return null; }
    }

    private static int[] span(Matcher m) { return new int[]{m.start(), m.end()}; }
    private static boolean overlaps(List<int[]> used, int[] sp) { for (int[] u : used) if (sp[0] < u[1] && u[0] < sp[1]) return true; return false; }
    private static boolean overlapsStart(List<int[]> used, int pos) { for (int[] u : used) if (pos >= u[0] && pos < u[1]) return true; return false; }

    private static String cleanTitle(String s, List<int[]> used) {
        List<int[]> u = new ArrayList<>(used);
        Collections.sort(u, (a, b) -> b[0] - a[0]);
        StringBuilder sb = new StringBuilder(s);
        for (int[] sp : u) if (sp[0] >= 0 && sp[1] <= sb.length() && sp[0] < sp[1]) sb.replace(sp[0], sp[1], " ");
        return sb.toString()
            .replaceAll("(?:^|\\s)(?:에|에서|까지|부터|~|-)(?=\\s|$)", " ")
            .replaceAll("\\s*(일정\\s*)?(등록|추가|넣어(줘)?|잡아(줘)?|해줘|해 줘|알려줘)\\s*$", "")
            .replaceAll("\\s+", " ").trim();
    }
}
