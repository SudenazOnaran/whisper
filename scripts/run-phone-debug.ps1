$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$backendDir = Join-Path $root "backend"
$androidDir = Join-Path $root "WhisperNotesApp"

function Resolve-Adb {
    $cmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($cmd) {
        return $cmd.Source
    }

    $candidates = @(
        "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
        "$env:ANDROID_HOME\platform-tools\adb.exe",
        "$env:ANDROID_SDK_ROOT\platform-tools\adb.exe"
    )

    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path $candidate)) {
            return $candidate
        }
    }

    throw "adb bulunamadi. Android Studio > SDK Manager > Android SDK Platform-Tools kurulu olmali."
}

$adb = Resolve-Adb

Write-Host "Whisper Notes telefon testi baslatiliyor..."

$devices = & $adb devices | Select-String -Pattern "device$"
if (-not $devices) {
    throw "Bagli Android cihaz bulunamadi. USB hata ayiklamayi acip telefonu onaylayin."
}

$healthOk = $false
try {
    $response = Invoke-WebRequest -Uri "http://127.0.0.1:8000/health" -UseBasicParsing -TimeoutSec 2
    $healthOk = $response.StatusCode -eq 200
} catch {
    $healthOk = $false
}

if (-not $healthOk) {
    Write-Host "Backend 8000 portunda calismiyor, yeni pencere aciliyor..."
    Start-Process powershell -ArgumentList @(
        "-NoExit",
        "-ExecutionPolicy", "Bypass",
        "-Command",
        "cd `"$backendDir`"; python server.py"
    )

    Write-Host "Backend hazir olana kadar bekleniyor..."
    $deadline = (Get-Date).AddMinutes(4)
    do {
        Start-Sleep -Seconds 3
        try {
            $response = Invoke-WebRequest -Uri "http://127.0.0.1:8000/health" -UseBasicParsing -TimeoutSec 3
            $healthOk = $response.StatusCode -eq 200
        } catch {
            $healthOk = $false
        }
    } while (-not $healthOk -and (Get-Date) -lt $deadline)

    if (-not $healthOk) {
        throw "Backend hazir olmadi. Acilan penceredeki Python hatasini kontrol edin."
    }
}

Write-Host "ADB reverse kuruluyor..."
& $adb reverse tcp:8000 tcp:8000 | Out-Null

Write-Host "Debug uygulamasi telefona yukleniyor..."
Push-Location $androidDir
try {
    $env:WHISPER_SERVER_URL = "http://127.0.0.1:8000"
    .\gradlew.bat --no-configuration-cache clean installDebug
} finally {
    Pop-Location
}

Write-Host "Uygulama aciliyor..."
& $adb shell am start -n com.whisper.notes/com.example.MainActivity | Out-Null

Write-Host "Hazir. Telefon ekraninda Whisper Notes acilmali."
