# 마이노트 AI 비서화 기획안 (v0.2)

> 작성 2026-10-13 · 대상: `00_AI파이선/16_마이노트` (Android `com.cmx.mynote` + PC `cmx_mynote`)
> 관계: 오늘자 **「마이노트 AI브레인 개발계약 v1.2」(오늘·리포트·브레인)** 위에 얹는 **4번째 축 "비서(에이전트)"** 기획.
> v1.2를 바꾸지 않는다. v1.2가 "기록을 모으고 정리하는 쪽"이면, 이 문서는 "그 기록으로 **먼저 말 걸고 대신 움직이는 쪽**"이다.

---

## 0. 한 줄 요약

마이노트가 이미 모으는 통화·사진·메모·회의·리포트에 **캘린더(네이버·구글·톡캘린더)** 를 붙이고,
PC 파이썬 쪽에 **비서 런타임(에이전트 + 스킬 + 메모리 + 하트비트)** 을 세워서,
형은 **앱 채팅·카톡 나와의 채팅·음성**으로 말만 하면 일정·기한·사건·현장이 처리되게 한다.

---

## 1. 현재 코드에서 확인한 것 (Dropbox 정본 기준)

| 영역 | 확인 내용 | 비서화에 쓰는 방법 |
|---|---|---|
| Android 스택 | Kotlin · Jetpack Compose · Room · WorkManager · Gemini(`gemini-3.1-pro-preview`) · ML Kit OCR · Dropbox PKCE 업로드 | 앱은 **수집·표시·알림 창구**. 무거운 에이전트 루프는 PC로 |
| Room 엔티티 | `Card`(source: call/voice/photo/screenshot/text/share/meeting/document) · `Report`(daily/weekly) · `Segment` · `Bookmark` · `ActionItem(dueDate)` · `CaseRef(caseNo, court)` | **캘린더 엔티티가 없다.** `ActionItem.dueDate`만 있고 일정 개념이 없음 → `CalendarEvent` 가산형 추가 |
| 워커 | `CallImportWorker`(15분) · `SharedSyncWorker`(15분, Dropbox `.sync/`) · `ProcessCardWorker` · `ReportWorker`(일일 21:00, 주간 월 08:00) · `MaintenanceWorker`(6h) | 같은 패턴으로 `CalendarSyncWorker`·`AssistantPushWorker` 추가 |
| 일일 리포트 | `DailyIssueReport` 7항목 고정(① 중요 일정·약속·마감이 최상단), `[자료N]` 출처 검증 | ①번 항목이 **비서의 일정 등록 후보 원천**. 여기서 뽑힌 일정을 캘린더로 보낸다 |
| PC | `cmx_mynote/{app,core,sync,digest,brain,ai,documents,sharing,hotkey,capture}.py`, reportlab 번들. 진입점 `08_유틸리티/00공통_NN_마이노트.py` | **비서 런타임을 여기에 둔다.** 파이썬이라 Claude Agent SDK·caldav 라이브러리 바로 사용 |
| 동기화 | Dropbox `.sync/cards/*-pc.json`, `.sync/reports/`, `.sync/candidates/` 파일 교환, 기록 ID + updatedAt | 비서 명령·결과도 같은 방식(`.sync/assistant/`)으로 교환 |
| 지식 | `1000_Obsidian` 볼트, `raw → candidate → review → verified` | 비서 메모리의 "검증된 사실" 출처. AI가 verified 승격 금지 원칙 유지 |
| 동의 게이트 | 통화는 `local_only` 기본, Gemini 전송·Dropbox 동기화는 날짜 범위 명시 동의 | 비서의 모든 외부 전송(캘린더 쓰기·메일·카톡)도 **같은 동의 모델**로 |
| 미구현 | 캘린더 연동, 대화형 비서 창구, 먼저 말 걸기(프로액티브), 외부 메시지 채널, 모델 추상화(Gemini 고정) | 이 문서의 범위 |

---

## 2. 비서가 제공하는 서비스 목록 (형이 고를 메뉴)

### A. 일정·시간 (1순위, MVP)

| # | 서비스 | 예시 | 비서가 하는 일 | 기존 코드 연결 |
|---|---|---|---|---|
| A1 | 자연어 일정 등록 | "다음주 화요일 오후 2시 서울중앙지법 현장검증" | 날짜·시간·장소·사건 파싱 → `CalendarEvent` 생성 → 네이버/구글 캘린더 반영 → 이동시간 역산 출발 알림 | 새 엔티티 + `CalendarSyncWorker` |
| A2 | 리포트 ①번 → 캘린더 자동 제안 | 통화에서 "14일 10시 검측" | 일일 리포트 ① 항목을 `[확정]`만 캘린더 후보로. `[확인 필요]`는 묻는다 | `DailyIssueReport` 출력 파싱 |
| A3 | 일정 조회·요약 | "이번 주 뭐 있어?" | 전 캘린더 합산 → 하루/주 요약, 겹침·이동 불가 구간 경고 | PC `digest.py` 확장 |
| A4 | 변경·취소 협상 | "목요일 미팅 금요일로" | 일정 수정 + 상대에게 보낼 변경 문구 초안(발송은 형 승인) | 동의 게이트 |
| A5 | 빈 시간 찾기 | "현장조사 3시간 비는 날" | free/busy 계산 → 후보 3개 | caldav free-busy |
| A6 | 아침 브리핑 07:30 | 자동 | 오늘 일정·마감·어제 미처리·이동 안내 → 앱 푸시 + 카톡 | `morning` 스킬 로직, `ReportWorker` 패턴 |
| A7 | 저녁 정리 | 기존 21:00 일일 리포트에 합류 | 리포트 생성 후 "내일 준비물·출발시각" 한 줄 푸시 | `ReportWorker` 완료 훅 |
| A8 | 상대 기한 리마인더 | "감정서 제출 2주 전에" | 기준 일정 역산 D-14/D-7/D-1 체인 | `ActionItem.dueDate` + 캘린더 알람 |

### B. 사건·감정 비서

| # | 서비스 | 비서가 하는 일 | 연결 |
|---|---|---|---|
| B1 | 사건 타임라인 | `CaseRef`별 단계(감정신청→현장조사→제출→보완→사실조회)·기한 추적, 법원 기한을 캘린더 자동 생성 | `CaseRef` 확장(단계·기한) |
| B2 | 기한 하트비트 | 30분마다 D-7/D-3/D-1·지연 점검, 위험할 때만 먼저 카톡 | PC 루프 |
| B3 | 현장조사 D-1 패키지 | 체크리스트·준비물·장비·동선 자동 생성 | `appraisal-site-investigation-plan`, `appraisal-survey-checklist` |
| B4 | 현장 음성→수량 | 녹음 카드 → 수량표 초안 | `voice-to-quantity-takeoff` (이미 Card.audioPath 있음) |
| B5 | 감정료·공문 | "이 사건 감정료" → 산출, 공문 초안 | `appraisal-fee-estimate`, `official-letter` |
| B6 | 감정서 착수 | 자료 폴더 지정 → 초안 파이프라인, 진행은 작업지침.md | `appraisal-draft`, `cmx-project-worklog` |
| B7 | 법령·판례 | "하자담보책임기간 근거" | `korean-law-search`, `11_법령MCP_미니서버` |
| B8 | 회의 → 할 일 → 일정 | 회의록 액션아이템을 승인 후 `ActionItem`+캘린더로 | 기존 `MeetingDetails`, `ActionItem` |

### C. 커뮤니케이션

| # | 서비스 | 비서가 하는 일 |
|---|---|---|
| C1 | 카톡 나와의 채팅 = 비서 창구 | 카톡에 적으면 처리하고 같은 방에 답장 (PlayMCP, 이미 계정 연결됨) |
| C2 | 메일 분류·답장 초안 | M365/Gmail을 사건·현장별 분류, 초안 작성. 발송은 승인 |
| C3 | 연락처·관계 메모리 | 통화 상대(`Card.source=call`)를 인물 엔티티로 축적, "○○ 소장" 하면 마지막 통화·약속을 꺼냄 |
| C4 | 상황별 문구 | 일정 변경 통보·자료 요청·기한 연장 신청 초안 |

### D. 기록·메모리

| # | 서비스 | 비서가 하는 일 |
|---|---|---|
| D1 | 메모 → 일정·할 일 감지 | 텍스트/음성 카드에서 "15일까지 도면" 감지 → 제안 (v1.2 할 일 중복검사 재사용) |
| D2 | 비서 장기 메모리 | 선호(호칭·문체·자주 가는 법원)·결정 이력을 `MEMORY.md` + 일별 노트로. 볼트 `verified` 지식은 읽기만 |
| D3 | 회상 질의 | "작년 수원 누수 사건 감정서" → 브레인 검색(v1.2 §7.3) 결과를 대화로 |

### E. 현장·콘업

| # | 서비스 | 연결 |
|---|---|---|
| E1 | 콘업 견적 즉시 발행 | `conup-quote` |
| E2 | 납품서·실명부 사진 → 콘업 입력 | `conup-photo-input` (사진 카드 재사용) |
| E3 | 현장 공정 일정 ↔ 감정 일정 충돌 체크 | 콘업 일정을 캘린더 읽기 어댑터로 |

### F. 문서

| # | 서비스 | 연결 |
|---|---|---|
| F1 | 문서 생성 허브 | `docx`/`xlsx`/`pptx`/`pdf`/`docx-hwp-safe` |
| F2 | 제출 전 최종 게이트 | `appraisal-final-gate`, `appraisal-quad-consistency-audit` |

### G. 후순위

G1 감정료 입금·미수·세금계산서 기한 / G2 출장 경비·이동거리 자동 기록 / G3 연속 외근 3일 넘으면 일정 조정 제안

**MVP 묶음(추천)**: A1 · A2 · A3 · A6 · B1 · B2 · C1 · D1. 이 8개면 "비서"로 느껴진다.

---

## 3. 아키텍처 — v1.2 위에 얹는 비서 레이어

```
 채널        앱 채팅(새 탭 "비서") │ 카톡 나와의 채팅(PlayMCP) │ 음성(기존 녹음→STT) │ 텔레그램(백업)
              └────────────────┬──────────────────────┘
 Android     AssistantScreen · AssistantPushWorker(알림) · CalendarSyncWorker(15분)
             Room: + CalendarEvent, + AssistantMessage, CaseRef 확장
              │  Dropbox .sync/assistant/{inbox,outbox}/*.json  (기존 .sync/cards 방식 그대로)
              ▼
 PC(파이썬)  cmx_mynote/assistant/
             ├ gateway.py     채널 수신/응답, 세션, 승인 게이트, 하트비트(30분), 크론(07:30/21:00)
             ├ agent.py       Claude Agent SDK 루프: 의도 → 스킬 선택 → 도구 → 확인 → 보고
             ├ skills/        SKILL.md (기존 70개 + 신규 7개)
             ├ memory/        MEMORY.md · daily/YYYY-MM-DD.md · cases.sqlite
             └ connectors/    calendar_naver(CalDAV) · calendar_google(API) · calendar_kakao(톡캘린더)
                              mail_m365 · mail_gmail · kakao_playmcp · dropbox · obsidian(읽기)
```

### 설계 원칙 (v1.2 원칙에 추가되는 것만)

1. **에이전트 루프는 PC, 앱은 창구**: 앱 Gemini 호출은 지금처럼 카드 요약용으로 두고, 대화·판단·다중 도구 호출은 PC 파이썬에서. 폰 배터리·권한 문제 회피.
2. **모델 2단**: 판단·대화는 `claude-fable-5-1`, 분류·날짜 파싱 같은 소형 작업은 `claude-haiku-5-5`. 앱 쪽 Gemini는 유지(교체 비용 없음). 모델명은 설정값.
3. **캘린더는 iCal 하나로 정규화**: 내부 `CalendarEvent`는 VEVENT 필드 그대로(uid, dtstart, dtend, summary, location, rrule, alarm, source_card_id, case_no). 각 서비스는 어댑터.
4. **승인 게이트 = v1.2 동의 모델 확장**: 캘린더 생성·내부 메모는 자동. 외부 발송(메일·카톡 타인·일정 초대)·삭제·외부 캘린더 쓰기 첫 회는 "할까요?" 1회. 동의는 범위·대상·기간을 명시한 기록으로 남긴다.
5. **먼저 말 걸되 조용히**: 하트비트는 조건 충족 시에만 푸시. 조용 시간 22:00~07:00은 D-1만.
6. **도구는 필요한 것만 로드**: 스킬 70개를 항상 올리지 않고 의도별로 MCP/스킬 서브셋만.
7. **지식 승격 불변**: 비서는 볼트 `verified`를 읽고 `80_학습인박스`에 후보만 쓴다.

---

## 4. 캘린더 연동 전략

| 캘린더 | 경로 | 확인 상태 | 판단 |
|---|---|---|---|
| **네이버 캘린더(개인)** | ① CalDAV `caldav.calendar.naver.com` (네이버 ID + 2단계 인증 앱 비밀번호) ② **Android 폰에 DAVx⁵(오픈소스 CalDAV 동기화 앱)로 네이버 계정 추가 → 안드로이드 `CalendarContract`로 앱이 직접 읽기/쓰기** | ①은 커뮤니티 가이드 확인, 공식 범위 미확인. 네이버 개발자센터 공식 캘린더 API 존재 여부는 이 세션에서 접속 차단으로 **미확인** | **②가 1순위.** API 키·OAuth 없이 폰 안에서 끝나고, 구글 캘린더도 같은 `CalendarContract`로 한 번에 처리된다. PC는 ①로 같은 CalDAV를 읽는다 |
| 구글 캘린더 | Android `CalendarContract`(폰) / Google Calendar API 또는 CalDAV(PC) | 공식 | free/busy·초대·알림 가장 풍부. 동기화 허브 후보 |
| 카카오 톡캘린더 | 톡캘린더 REST API(서브캘린더 조회·생성·수정·삭제, 할당량 확대는 권한 신청) | 공식 문서 존재, 일정 생성 권한 신청 필요 여부 확인 필요 | 카톡 알림과 묶기 좋음. 2단계 |
| 네이버웍스 | CalDAV, 관리자 설정 + Basic 이상 | 공식 | 콘업 법인 계정이면 이 경로 |
| 아웃룩 | M365 커넥터(이미 연결) | 공식 | 즉시 가능 |

**동기화**: 폰은 `CalendarContract` 변경 옵저버 + 15분 워커. PC는 CalDAV sync-token(RFC 6578)/ETag. 충돌은 "마지막 수정 우선"이 아니라 **비서가 형에게 묻는다**(v1.2 §9 "사용자 수정 우선" 원칙과 동일).

**한국어 날짜 파싱**: 1차 `chrono-node`+`datecapture`(또는 파이썬 `dateparser`)로 규칙 파싱, 2차 Haiku로 "다다음주 수요일 오후 2시 반", "추석 끝나고 첫 평일", "변론기일 전날" 같은 표현 + 법원 기일 용어 인식, 3차 규칙으로 과거 날짜·주말·조용시간 검증. 불확실하면 `[확인 필요]`로 묻는다(v1.2 T10 원칙).

---

## 5. 오픈소스·스킬 조사

### 5.1 차용할 오픈소스

| 프로젝트 | 용도 | 판단 |
|---|---|---|
| [OpenClaw](https://github.com/openclaw/openclaw) (MIT) | 자가 호스팅 AI 비서: 게이트웨이·채널·SKILL.md·3층 파일 메모리·하트비트·크론. 카카오 PlayMCP가 2026-05 OpenClaw 연동 지원 발표 | **아키텍처 참조 1순위.** 통째로 쓰기보다 gateway/heartbeat/memory 구조를 `cmx_mynote/assistant/`에 이식. SKILL.md 형식이 Claude 스킬과 동일 |
| [DAVx⁵](https://github.com/bitfireAT/davx5-ose) (GPLv3) | Android CalDAV/CardDAV 동기화 앱 | 네이버 캘린더를 폰 `CalendarContract`로 끌어오는 다리. 앱에 포함하지 않고 별도 설치(라이선스 분리) |
| [python caldav 3.x](https://pypi.org/project/caldav/) (GPL3/Apache2) | PC CalDAV 클라이언트 | PC 어댑터. 네이버·구글·네이버웍스 공통 |
| [nspady/google-calendar-mcp](https://github.com/nspady/google-calendar-mcp) (~1.1k★) | 구글 캘린더 MCP | PC 구글 어댑터. 2026-03 이후 커밋 정체 보고, 포크 전제 |
| [PhilflowIO/dav-mcp](https://glama.ai/mcp/servers/PhilflowIO/dav-mcp) · [madbonez/caldav-mcp](https://github.com/madbonez/caldav-mcp) | CalDAV MCP | python caldav 직접 사용이 더 단순. MCP는 Claude Code/Cowork에서 쓸 때 |
| [elie222/inbox-zero](https://github.com/elie222/inbox-zero) | 메일 비서·미팅 브리프 | C2 분류 규칙·브리프 UX 참고 |
| [theexperiencecompany/gaia](https://github.com/theexperiencecompany/gaia) · [Khoj](https://github.com/khoj-ai/khoj) | 프로액티브 비서, 자동화·문서검색 | "먼저 말 걸기"·자동화 UX 참고 |
| [chrono-node](https://github.com/wanasit/chrono) + [datecapture](https://npmjs.com/package/datecapture) / [dateparser](https://pypi.org/project/dateparser/) | 자연어 날짜 1차 파서 | §4 참조 |
| [PyKakao](https://github.com/WooilJeong/PyKakao) | 카카오 메시지 API | "나에게 보내기"는 **알림이 안 울림** + 2026-07 장애 사례. 푸시는 PlayMCP/앱 알림/알림톡 |

### 5.2 보유 스킬 → 비서 부품

`morning`(A6) · `cmx-project-worklog`(B6) · `appraisal-site-investigation-plan`/`appraisal-survey-checklist`(B3) · `voice-to-quantity-takeoff`/`conup-photo-input`(B4·E2) · `appraisal-fee-estimate`/`official-letter`(B5) · `korean-law-search`(B7) · `construction-meeting-minutes`(B8) · `conup-quote`(E1) · `docx`·`xlsx`·`pptx`·`pdf`·`docx-hwp-safe`·`google-workspace`(F1) · `appraisal-final-gate`·`appraisal-quad-consistency-audit`(F2) · `user-profile-kisang`(메모리 시드)

### 5.3 신규 스킬 7개

| 스킬 | 역할 |
|---|---|
| `calendar-unified` | 다중 캘린더 읽기/쓰기/free-busy, iCal 정규화, 충돌 질의 |
| `kr-datetime-parse` | 한국어 날짜·시간·반복 + 법원 기일 용어(변론기일·현장검증·감정기일) |
| `case-timeline` | `CaseRef` 단계·기한, 법원 기한 역산 |
| `deadline-heartbeat` | 30분 점검, 알림 정책(조용 시간·중복 억제) |
| `report-to-calendar` | 일일 리포트 ① 항목 → `[확정]`만 캘린더 후보, `[확인 필요]`는 질문 |
| `kakao-concierge` | 카톡 창구 입출력 규약(짧게, 번호 선택지) |
| `assistant-memory` | MEMORY.md·일별 노트 읽기/쓰기/통합, 볼트 verified 읽기 전용 |

---

## 6. 데이터 모델 추가 (가산형, v1.2 §10 원칙)

```kotlin
@Entity(tableName="calendar_events")
data class CalendarEvent(
  @PrimaryKey val uid: String,            // iCal UID = 외부 캘린더 UID
  val calendarId: String,                 // naver|google|kakao|local
  val dtStart: Long, val dtEnd: Long, val allDay: Boolean,
  val summary: String, val location: String?, val description: String?,
  val rrule: String?, val alarmMinutes: Int?,
  val caseNo: String?, val sourceCardId: String?,   // 출처: 리포트/통화/메모
  val status: String,     // proposed(비서 제안) | confirmed | synced | cancelled
  val etag: String?, val updatedAt: Long, val origin: String  // phone|pc|external
)
@Entity(tableName="assistant_messages")
data class AssistantMessage(
  @PrimaryKey val id: String, val channel: String, // app|kakao|voice
  val role: String, val text: String, val createdAt: Long,
  val pendingApproval: String?   // 승인 대기 액션 JSON
)
// CaseRef 확장: stage, nextDeadline, nextDeadlineKind, court
```
PC: `memory/cases.sqlite`(사건·인물·결정), `MEMORY.md`, `daily/`. 교환은 `.sync/assistant/inbox|outbox/*.json`.

---

## 7. 프로액티브 동작

- 07:30 아침 브리핑(크론) · 21:00 일일 리포트 후 저녁 한 줄(기존 `ReportWorker` 훅)
- 30분 하트비트: 기한 D-7/D-3/D-1, 캘린더 충돌, 리포트 `[확인 필요]` 미처리, 답장 안 한 메일
- 조용 시간 22:00~07:00: D-1만
- 모든 푸시는 "근거 카드 링크" 동반(v1.2 "출처 없는 문장 금지")

---

## 8. 로드맵

| 단계 | 기간 | 산출물 | 완료 기준 |
|---|---|---|---|
| **0. 실측** | 1주 | DAVx⁵+네이버 CalDAV로 폰 `CalendarContract` 읽기/쓰기 실측, PC python-caldav 실측, 톡캘린더·PlayMCP 권한 신청, 네이버 개발자센터 캘린더 API 유무 확인 | 네이버 일정 1건 양방향 왕복 성공 |
| **1. 캘린더 MVP** | 3주 | `CalendarEvent` + `CalendarSyncWorker` + `kr-datetime-parse` + `report-to-calendar` + 앱 "비서" 탭 | A1·A2·A3. 리포트 ① → 캘린더 등록 왕복 |
| **2. 비서 런타임** | 3주 | PC `cmx_mynote/assistant/` (gateway·agent·memory) + 하트비트 + 아침 브리핑 + 카톡 창구 | A6·B1·B2·C1·D1 |
| **3. 커뮤니케이션** | 2주 | 메일 분류·초안, 인물 메모리, 승인 게이트 UI | C2·C3·C4 |
| **4. 현장·문서** | 2주 | 감정 스킬 연결, 콘업 연동, 최종 게이트 | B3~B8·E·F |
| **5. 고도화** | 지속 | 재무·건강·ClawHub 호환 스킬 마켓 | G |

**v1.2 P-단계와의 순서**: v1.2 P1~P3(기록 통합·홈·리포트)가 먼저. 비서 1단계는 P3 리포트가 ①항목을 안정적으로 내놓은 뒤 시작한다. 0단계 실측은 지금 병행 가능.

---

## 9. 리스크·확인 필요

| 항목 | 리스크 | 대응 |
|---|---|---|
| 네이버 공식 API | 존재 여부 미확인(세션에서 developers.naver.com 차단) | 형이 직접 확인. 없으면 DAVx⁵+CalDAV 확정 |
| CalDAV 안정성 | 비공식 안내 수준, 쓰기 타임아웃 사례 | 0단계 실측, 재시도 큐(기존 WorkManager 백오프 패턴) |
| DAVx⁵ 의존 | 별도 앱 설치 필요 | 설치 안내 1회. 구글 캘린더는 DAVx⁵ 없이도 됨 |
| PlayMCP 베타 | 정책·가용성 변동 | 텔레그램 백업 채널(OpenClaw 방식) |
| 개인정보 | 통화·사건 자료의 외부 전송 | v1.2 §16 동의 모델 그대로. 비서도 `local_only` 기본 |
| 모델 이중화 | 앱 Gemini + PC Claude | 카드 요약은 Gemini 유지, 대화·판단만 Claude. 비용·일관성 모니터링 |
| PC 상시 가동 | 하트비트는 PC가 켜져 있어야 | 1차는 PC 상시, 2차 소형 서버/클라우드 세션(Claude Code Routine)로 이전 |

---

## 10. 형이 결정할 것

1. 네이버 캘린더 연결: **DAVx⁵ 설치 후 폰 캘린더 통합(추천)** vs 네이버 공식 API 확인 후 결정
2. 비서 창구 주력: 카톡 나와의 채팅 vs 앱 "비서" 탭
3. MVP 8개(A1·A2·A3·A6·B1·B2·C1·D1) 확정 여부
4. 비서 런타임 위치: PC 파이썬(추천, 지금 구조와 맞음) vs 별도 서버

결정되면 0단계(DAVx⁵·CalDAV 실측 스크립트 + `calendar-unified` 스킬 초안)부터 바로 들어간다.

---

## 참고 출처

- 마이노트 정본: `00_AI파이선/16_마이노트` (AGENTS.md, docs/ARCHITECTURE.md, docs/마이노트_AI브레인_개발계약_20261013.md v1.2, Entities.kt, WorkScheduler.kt, DailyIssueReport.kt, pc/cmx_mynote/sync.py)
- 네이버 캘린더 CalDAV: [extrememanual.net](https://extrememanual.net/41175), [small.dalgu.app](https://small.dalgu.app/ko/scheduler/guide/naver-calendar-sync-iphone) · 네이버웍스: [guide.whattime.co.kr](https://guide.whattime.co.kr/prepare/connect/naver_works)
- 카카오 톡캘린더 API: [developers.kakao.com](https://developers.kakao.com/docs/latest/ko/talkcalendar/common) · PlayMCP: [보도자료](https://www.kakaocorp.com/page/detail/11674?lang=KOR), [OpenClaw 연동](https://www.kakaocorp.com/page/detail/12012?lang=KOR) · 나에게 보내기 제한: [devtalk](https://devtalk.kakao.com/t/topic/125990), [장애 사례](https://devtalk.kakao.com/t/api-100-400-2/150538)
- OpenClaw: [GitHub](https://github.com/openclaw/openclaw), [핵심 개념](https://clawdocs.org/getting-started/core-concepts), [아키텍처 분석](https://www.heyuan110.com/posts/ai/2026-02-14-openclaw-architecture-deep-dive/)
- 캘린더 MCP·라이브러리: [dav-mcp](https://glama.ai/mcp/servers/PhilflowIO/dav-mcp), [caldav-mcp](https://github.com/madbonez/caldav-mcp), [google-calendar-mcp 리뷰](https://chatforest.com/reviews/google-calendar-mcp-server/), [python caldav](https://pypi.org/project/caldav/), [tsdav](https://github.com/calcom/tsdav), [datecapture](https://npmjs.com/package/datecapture)
- 비서 오픈소스: [inbox-zero](https://github.com/elie222/inbox-zero), [gaia](https://github.com/theexperiencecompany/gaia), [자가호스팅 비서 목록](https://openalternative.co/collections/self-hosted/categories/ai-personal-assistants)
- Claude 에이전트 스케줄·메모리: [kjetilfuras.com](https://kjetilfuras.com/run-claude-agent-on-schedule/), [hatchworks.com](https://hatchworks.com/?p=35376)
