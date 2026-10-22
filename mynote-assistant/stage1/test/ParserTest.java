import com.cmx.mynote.assistant.*;
import java.time.*;
import java.util.*;

public class ParserTest {
    static int pass = 0, fail = 0;
    // 기준: 2026-10-22(목) 14:00
    static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 22, 14, 0);

    static void check(String in, String date, String start, String end, boolean usable, String titleContains) {
        KrDateTimeParser.Result r = KrDateTimeParser.parse(in, NOW);
        boolean ok = Objects.equals(date, r.date == null ? null : r.date.toString())
                  && Objects.equals(start, r.start == null ? null : r.start.toString())
                  && Objects.equals(end, r.end == null ? null : r.end.toString())
                  && usable == r.isUsable()
                  && (titleContains == null || r.title.contains(titleContains));
        if (ok) pass++; else { fail++; System.out.println("FAIL: " + in + "\n   got " + r); }
    }

    public static void main(String[] a) {
        check("다음주 화요일 오후 2시 서울중앙지법 현장검증", "2026-10-27", "14:00", null, true, "서울중앙지법 현장검증");
        check("내일 10시 반 수원지법 변론기일", "2026-10-23", "10:30", null, true, "수원지법 변론기일");
        check("모레 오전 9시 현장조사 3시간", "2026-10-24", "09:00", "12:00", true, "현장조사");
        check("10월 30일 14:00 감정서 제출", "2026-10-30", "14:00", null, true, "감정서 제출");
        check("11월 3일 감정료 입금 확인", "2026-11-03", null, null, true, "감정료 입금 확인");
        check("2026-11-12 오후 3시~5시 콘업 교육", "2026-11-12", "15:00", "17:00", true, "콘업 교육");
        check("다다음주 수요일 저녁 7시 회식", "2026-11-04", "19:00", null, true, "회식");
        check("이번주 금요일 오후 1시 미팅", "2026-10-23", "13:00", null, true, "미팅");
        check("3시에 김소장 통화", null, "15:00", null, false, "김소장 통화");          // 날짜 없음 → 되묻기
        check("금요일 4시 자료 전달", "2026-10-23", "16:00", null, false, "자료 전달");   // 오전/오후 미표기 → 되묻기
        check("2일 후 오전 11시 도면 수령", "2026-10-24", "11:00", null, true, "도면 수령");
        check("어제 오후 2시 회의", "2026-10-21", "14:00", null, false, "회의");          // 과거 → 되묻기
        check("오늘 오전 9시 점검", "2026-10-22", "09:00", null, false, "점검");          // 이미 지난 시간
        check("11/5 오후 2시 사실조회 회신", "2026-11-05", "14:00", null, true, "사실조회 회신");
        check("2월 30일 회의", null, null, null, false, null);                           // 없는 날짜
        check("다음주 월요일 종일 현장", "2026-10-26", null, null, true, "현장");
        check("목요일 오후 4시 감리 회의", "2026-10-22", "16:00", null, false, "감리 회의"); // 오늘 목요일 → 애매
        check("다음 주 수요일 정오 점심", "2026-10-28", "12:00", null, true, null);
        check("일정 없음", null, null, null, false, null);
        check("25일 오후 2시 기일", "2026-10-25", "14:00", null, false, "기일");          // 일자만 → 월 가정 질문

        String report = String.join("\n",
            "# 일일 리포트 2026-10-22",
            "## ① 중요 일정·약속·마감",
            "- [확정] 10월 27일 10:00 현장 검측 — 근거: [자료1], [자료3]",
            "- [확인 필요] 자료 전달 약속 — 날짜 미확인 [자료2]",
            "- [확정] 다음주 수요일 오후 3시 감정기일 [자료4]",
            "- [확정] 금요일 4시 도면 회신 [자료5]",
            "- [확정] 11월 3일 감정료 청구",
            "## ② 오늘의 핵심 브리핑",
            "- [확정] 10월 30일 이건 다른 절이라 무시");
        List<ReportScheduleExtractor.Item> items = ReportScheduleExtractor.extract(report, NOW);
        boolean[] expect = {true, false, true, false, false};
        if (items.size() != 5) { fail++; System.out.println("FAIL extractor size " + items.size() + " " + items); }
        else for (int i = 0; i < 5; i++) {
            if (items.get(i).proposeToCalendar == expect[i]) pass++;
            else { fail++; System.out.println("FAIL extractor #" + i + " " + items.get(i)); }
        }
        if (ReportScheduleExtractor.extract("본문만 있음", NOW).isEmpty()) pass++; else fail++;
        if (ReportScheduleExtractor.extract(null, NOW).isEmpty()) pass++; else fail++;

        System.out.println("PASS " + pass + " / FAIL " + fail);
        if (fail > 0) System.exit(1);
    }
}
