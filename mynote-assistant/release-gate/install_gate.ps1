# 마이노트 설치 관문 — 이 스크립트를 통과한 APK만 폰에 들어간다.
# 사용: 16_마이노트 폴더에서  powershell -ExecutionPolicy Bypass -File .\tools\install_gate.ps1
# 실패하면 설치하지 않고 멈춘다. 설치 후 문제가 생기면 직전 APK로 자동 되돌린다.
param(
  [string]$Root = (Get-Location).Path,
  [int]$SmokeSeconds = 45
)
$ErrorActionPreference = "Stop"
$pkg   = "com.cmx.mynote"
$apk   = Join-Path $Root "app\build\outputs\apk\debug\app-debug.apk"
$keep  = Join-Path $Root "_apk\_stable"
$log   = Join-Path $Root ("90_작업기록\install_gate_" + (Get-Date -Format "yyyyMMdd_HHmm") + ".log")
New-Item -ItemType Directory -Force -Path $keep | Out-Null
function Say($m){ $m | Tee-Object -FilePath $log -Append }
function Fail($m){ Say "[중단] $m"; exit 1 }

Say "== 1. 버전 확인 (개발본 차단)"
$gradle = Get-Content (Join-Path $Root "app\build.gradle.kts") -Raw
if ($gradle -match 'versionName\s*=\s*"([^"]+)"') { $ver = $Matches[1] } else { Fail "versionName 을 못 찾음" }
Say "versionName=$ver"
if ($ver -match '(?i)dev|alpha|test|exp') { Fail "개발본($ver)은 운영폰 설치 금지" }

Say "== 2. 테스트 + 빌드"
& "$Root\gradlew.bat" testDebugUnitTest assembleDebug --console=plain *>> $log
if ($LASTEXITCODE -ne 0) { Fail "테스트 또는 빌드 실패 → $log" }
if (-not (Test-Path $apk)) { Fail "APK 없음" }

Say "== 3. 폰 확인"
$dev = (adb devices) -match "`tdevice$"
if (-not $dev) { Fail "adb devices 에 연결된 폰 없음" }
$before = (adb shell dumpsys package $pkg | Select-String "versionName").ToString().Trim()
Say "설치 전: $before"

Say "== 4. 롤백용 현재 APK 백업"
$path = (adb shell pm path $pkg) -replace "package:",""
if ($path) {
  $rb = Join-Path $keep ("rollback_" + (Get-Date -Format "yyyyMMdd_HHmm") + ".apk")
  adb pull $path.Trim() $rb *>> $log
  Say "롤백 APK: $rb"
} else { $rb = $null; Say "기존 설치 없음" }

Say "== 5. 설치 (데이터 보존)"
adb install -r $apk *>> $log
if ($LASTEXITCODE -ne 0) { Fail "설치 실패 (서명 불일치 가능). 앱 삭제하지 말 것" }

Say "== 6. 스모크: 실행 후 $SmokeSeconds 초 동안 크래시·전사 오류 감시"
adb logcat -c
adb shell monkey -p $pkg -c android.intent.category.LAUNCHER 1 *>> $log
Start-Sleep -Seconds $SmokeSeconds
$lc = adb logcat -d
$lc | Out-File -Append $log
$bad = $lc | Select-String -Pattern "FATAL EXCEPTION.*$pkg|AndroidRuntime.*$pkg|Workspace.*denied|SerializationException|MissingFieldException"
$alive = adb shell pidof $pkg
if ($bad -or -not $alive) {
  Say "[스모크 실패]"; $bad | ForEach-Object { Say $_.Line }
  if ($rb) { adb install -r $rb *>> $log; Say "[롤백 완료] 이전 버전으로 되돌림" }
  exit 1
}

Say "== 7. 통과 → 안정본 보관"
Copy-Item $apk (Join-Path $keep ("mynote_" + $ver + ".apk")) -Force
Say "[통과] $ver 설치 완료. 전사는 폰에서 음성메모 1건으로 직접 확인할 것."
