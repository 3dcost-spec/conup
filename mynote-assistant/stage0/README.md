# 마이노트 비서 — 0단계 실측 작업지침

최종 갱신 2026-10-13 · Claude
결정(2026-10-13, 기상이 형): 네이버 캘린더는 **DAVx⁵** 경로 · 비서 창구 주력은 **카톡 나와의 채팅** · MVP 8개(A1·A2·A3·A6·B1·B2·C1·D1) 확정.

## 1. 목표
네이버 캘린더 일정 1건이 **폰(DAVx⁵) ↔ 네이버 서버 ↔ PC(CalDAV)** 세 곳을 왕복하는 것을 증명한다. 이게 되면 1단계(캘린더 MVP) 착수.

## 2. 형이 할 일 (폰, 10분)
1. 네이버 → 내정보 → 보안설정 → **2단계 인증** 켜기 → **애플리케이션 비밀번호** 발급 (용도: 캘린더/DAVx⁵). 이 비밀번호를 PC 실측에도 쓴다.
2. Play 스토어에서 **DAVx⁵** 설치 (유료, 또는 F-Droid 무료판 `at.bitfire.davdroid`).
3. DAVx⁵ → 계정 추가 → "URL과 사용자 이름으로 로그인" → 기본 URL `https://caldav.calendar.naver.com`, 사용자 이름 네이버ID, 비밀번호 = 앱 비밀번호 → 계정 이름 "네이버 캘린더".
4. 계정 열고 **CalDAV** 탭에서 동기화할 캘린더 체크 → 동기화 1회 실행.
5. 폰 기본 캘린더 앱(삼성/구글 캘린더)에서 네이버 일정이 보이는지 확인. 보이면 통과.
   - 안 보이면: DAVx⁵ 배터리 최적화 제외, 캘린더 앱 "표시할 캘린더"에서 DAVx⁵ 계정 켜기.

## 3. PC 실측 (형 PC, 5분)
```powershell
cd "H:\공유 드라이브\CMX_워크센터\00_AI파이선\16_마이노트\90_작업기록\비서_0단계_20261013\pc"
pip install caldav icalendar
$env:CALDAV_USER="네이버ID"; $env:CALDAV_PASS="앱비밀번호"
python caldav_probe.py --list-only                 # 1차: 조회만
python caldav_probe.py --keep --out probe_naver.json   # 2차: 테스트 일정 생성, 삭제 안 함
```
→ 폰 DAVx⁵ 수동 동기화 → 폰 캘린더에 "[마이노트 실측] 캘린더 왕복 테스트" 가 보이면 **PC→서버→폰 통과**.
→ 폰에서 그 일정 제목을 바꾸고 동기화 → `python caldav_probe.py --list-only` 로 바뀐 제목 확인되면 **폰→서버→PC 통과**.
→ `probe_naver.json` 을 이 폴더에 두면 내가 읽고 다음 단계 판단.

## 4. Android 코드 실측 (코덱스, 1시간)
`android/README.md` 참조. `CalendarProviderProbe.kt` 를 앱에 넣고 설정 화면 버튼으로 실행. DAVx⁵ 캘린더가 `accountType=bitfire.at.davdroid` 로 보이고 `roundTrip` 합격이면 통과.

## 5. 합격 기준 (전부 만족해야 1단계 착수)
| # | 항목 | 판정 |
|---|---|---|
| P1 | PC `caldav_probe.py` 네이버 실서버 8단계 ok | |
| P2 | sync-token 지원 (`token_present=true`). 아니면 ETag 폴링으로 설계 변경 | |
| P3 | PC 생성 일정이 폰에 보임 (DAVx⁵) | |
| P4 | 폰 수정이 PC에서 읽힘 | |
| P5 | 앱 `CalendarProviderProbe.roundTrip` ok | |
| P6 | 삭제 전파 (폰 삭제 → PC 조회에서 사라짐) | |
| P7 | 쓰기 지연 측정: create/update ms 기록 (504·타임아웃 발생 여부) | |

## 6. 이 세션에서 한 것 / 안 한 것
- 한 것: `pc/caldav_probe.py` 작성, 로컬 Radicale CalDAV 서버로 8단계 왕복 **합격** (list·pick·read·sync-token·create·read_back(ETag)·update·delete). `android/CalendarProviderProbe.kt` 작성. `skills/calendar-unified/SKILL.md` 초안.
- 안 한 것: 네이버 실서버 접속(이 세션은 네이버 도메인 차단 + 자격증명 없음). Android 빌드(SDK 없음). 둘 다 형 PC/코덱스에서.

## 7. 다음
P1~P7 결과 받으면 → 1단계: `CalendarEvent` Room 엔티티 + `CalendarSyncWorker` + `kr-datetime-parse` 스킬 + `report-to-calendar`(리포트 ①→캘린더) + 앱 "비서" 탭 + 카톡 창구 연결.
