# Android 실측 — CalendarProviderProbe

1. `AndroidManifest.xml`
   ```xml
   <uses-permission android:name="android.permission.READ_CALENDAR"/>
   <uses-permission android:name="android.permission.WRITE_CALENDAR"/>
   ```
2. `CalendarProviderProbe.kt` 를 `app/src/main/java/com/cmx/mynote/calendar/` 에 넣는다.
3. 설정 화면(`SettingsScreen.kt`)에 "비서 > 캘린더 실측" 버튼 1개 추가 → 권한 요청 → `listCalendars()` 결과 표시 →
   네이버(DAVx⁵) 캘린더 선택 → `roundTrip(id, keep=true)` → 보고서를 화면에 표시하고
   `MarkdownWriter`/`DropboxClient` 경로로 `.sync/assistant/probe/android_<일시>.json` 업로드.
4. 합격 기준: `listCalendars()` 에 `accountType=bitfire.at.davdroid` 캘린더가 보이고, `roundTrip` 전 단계 ok,
   DAVx⁵ 수동 동기화 후 네이버 캘린더 웹과 PC `caldav_probe.py --list-only` 양쪽에서 "[마이노트 실측]" 일정이 보임.
5. 확인 후 폰에서 테스트 일정을 삭제하고 다시 동기화해 삭제가 전파되는지도 확인한다.

주의: 이 코드는 이 세션에서 컴파일하지 않았다(Android SDK 없음). 코덱스 쪽에서 `./gradlew.bat assembleDebug` 로 빌드한다.
