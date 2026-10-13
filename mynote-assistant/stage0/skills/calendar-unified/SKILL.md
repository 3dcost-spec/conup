---
name: calendar-unified
description: >
  마이노트 비서의 캘린더 통합 스킬(초안 v0.1). 네이버(DAVx⁵→CalendarContract / PC CalDAV)·구글·톡캘린더를
  하나의 iCal(VEVENT) 모델로 읽고 쓰고, 빈 시간을 찾고, 충돌을 알린다. '일정 등록', '일정 추가', '캘린더에 넣어',
  '이번 주 일정', '빈 시간', '언제 비어', '일정 옮겨', '미뤄', '취소해', '현장검증 잡아', '기일 등록',
  'calendar', 'schedule' 등의 키워드가 나오거나, 일일 리포트 ① 항목에서 [확정] 일정이 나오면 사용할 것.
  날짜·시간 파싱은 kr-datetime-parse 스킬에 위임하고, 이 스킬은 캘린더 읽기/쓰기/질의만 담당한다.
---

# calendar-unified (초안 v0.1)

## 0. 원칙

1. **내부 모델은 iCal VEVENT 하나.** 필드: `uid, calendar_id, dtstart, dtend, all_day, summary, location, description, rrule, alarm_minutes, case_no, source_card_id, status, etag, updated_at, origin`.
   Android `CalendarEvent` 엔티티·PC `cases.sqlite`·외부 캘린더 모두 이 필드로 매핑한다.
2. **UID는 한 번 정하면 어디서나 같다.** 폰에서 만들면 `UID_2445`, PC에서 만들면 CalDAV `uid`. 형식 `mynote-<uuid>`.
3. **쓰기 전에 되묻는 경우**: 날짜·시간이 `[확인 필요]`일 때, 기존 일정과 겹칠 때, 과거 날짜일 때, 외부 캘린더 첫 쓰기일 때(동의 1회).
4. **지어내지 않는다.** 출처 카드(`source_card_id`)가 없는 일정은 사용자가 직접 말한 것만 만든다.
5. **충돌은 사용자가 결정.** 폰·PC·외부가 서로 다르면 "마지막 수정 우선"으로 덮지 않고 세 버전을 보여주고 묻는다.

## 1. 어댑터

| 어댑터 | 읽기 | 쓰기 | 비고 |
|---|---|---|---|
| `android_provider` | `CalendarContract.Instances` | `Events.insert/update/delete` + `Reminders` | 네이버(DAVx⁵)·구글 공통. 폰이 정본 창구 |
| `caldav` (PC) | `python-caldav` `calendar.search()` + sync-token | `save_event` / `event.save()` / `delete` | 네이버 `caldav.calendar.naver.com`, 구글 CalDAV, 네이버웍스 |
| `kakao_talkcalendar` | 톡캘린더 REST | 서브캘린더 생성·수정·삭제 | 2단계. 권한 신청 후 |

## 2. 동작

### 2.1 일정 등록
입력: 자연어 문장 또는 리포트 ① 항목 1줄 (+ 출처 카드 ID)
1. `kr-datetime-parse`로 `{dtstart, dtend, all_day, confidence, ambiguity}` 얻기. `confidence < 0.8`이면 되묻기.
2. 장소·사건번호(`CaseRef` 패턴)·참석자 추출. 사건번호가 있으면 `case_no` 연결.
3. 같은 날 ±2시간 겹침 검사 → 겹치면 두 일정을 보여주고 "그래도 넣을까요?".
4. 대상 캘린더 선택: 기본은 설정의 "주 캘린더"(네이버). 사용자가 "구글에" 하면 그쪽.
5. 생성 후 한 줄 보고: `등록: 10/21(화) 14:00 서울중앙지법 현장검증 (근거: 통화 김○○ 소장 10/13)`.
6. 리포트 `[확정]` 항목에서 온 일정은 `status=proposed`로 만들고 사용자가 승인하면 `confirmed`.

### 2.2 조회·요약
- "오늘/내일/이번 주/다음 주/N일": 모든 캘린더를 합쳐 시간순. 겹침은 ⚠, 이동시간 부족(법원 간 60분 미만)은 🚗 표시.
- 출력은 짧게. 카톡 창구에서는 하루 5줄, 주간은 요일별 1~2줄.

### 2.3 변경·취소
- "목요일 미팅 금요일로": 대상 일정 특정(제목·시간 유사도) → 후보 2개 이상이면 번호로 묻기 → 수정 → 변경 전후 한 줄 보고.
- 취소는 삭제하지 않고 `status=cancelled` + 외부 캘린더에서는 삭제. 되돌리기 가능하게 로컬에 남긴다.

### 2.4 빈 시간 찾기
- 입력: 소요시간, 기간, 조건(평일/오전/법원 근처). free/busy = 전 캘린더 합집합의 여집합. 조용 시간·점심(12~13시)은 제외. 후보 3개를 번호로 제시.

### 2.5 동기화·충돌
- 폰: `ContentObserver` + 15분 `CalendarSyncWorker`. PC: CalDAV sync-token(없으면 ETag 비교).
- 로컬 `updated_at`과 외부 `etag`가 모두 바뀌었으면 충돌 → 사용자에게.

## 3. 출력 규약 (카톡 창구)
```
📅 10/21(화)
09:30 수원지법 변론기일 (2026가합1234)
14:00 서울중앙지법 현장검증 ⚠ 09:30 건과 이동 90분 필요
—
1) 그대로 둠  2) 현장검증 15:00으로  3) 다른 날 찾기
```

## 4. 금지
- 외부 캘린더 전체 삭제·일괄 수정 명령 실행 금지.
- 사용자 동의 없이 타인 초대(ATTENDEE) 추가 금지.
- 일정 본문(description)에 통화 전사 원문 복사 금지. 요약 1줄 + 카드 링크만.

## 5. 0단계 실측 스크립트
- PC: `stage0/pc/caldav_probe.py` (로컬 Radicale 서버로 8단계 왕복 합격 확인됨. 네이버 실서버는 형 계정으로 실행 필요)
- Android: `stage0/android/CalendarProviderProbe.kt`
