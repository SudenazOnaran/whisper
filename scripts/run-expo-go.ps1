################################################################
# run-expo-go.ps1
# Whisper Notes — Expo Go hızlı test scripti (Windows/PowerShell)
#
# Yapılanlar:
#   1. Backend sağlık kontrolü / başlatma
#   2. adb reverse tcp:8000 tcp:8000  (kablo bağlantısı için)
#   3. EXPO_PUBLIC_SERVER_URL ayarla
#   4. npx expo start -c  (cache temizli)
################################################################
$ErrorActionPreference = "Stop"

$root       = Resolve-Path (Join-Path $PSScriptRoot "..")
$backendDir = Join-Path $root "backend"
$expoDir    = Join-Path $root "WhisperNotesExpo"

# ── ADB bulma ────────────────────────────────────────────────────
function Resolve-Adb {
    if (Get-Command adb -ErrorAction SilentlyContinue) { return "adb" }

    $candidates = @(
        "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
        "$env:ANDROID_HOME\platform-tools\adb.exe",
        "$env:ANDROID_SDK_ROOT\platform-tools\adb.exe"
    )
    foreach ($c in $candidates) {
        if (Test-Path $c) { return $c }
    }

    Write-Warning "adb bulunamadi. Wi-Fi ile baglaniyorsaniz sorun yok."
    return $null
}

$adb = Resolve-Adb

# ── Backend kontrolü ─────────────────────────────────────────────
Write-Host ""
Write-Host "=== Whisper Notes - Expo Go Test Ortami ===" -ForegroundColor Cyan
Write-Host ""

$backendOk = $false
try {
    $resp = Invoke-RestMethod "http://127.0.0.1:8000/health" -TimeoutSec 3 -ErrorAction Stop
    if ($resp.status -eq "ok") {
        Write-Host "[OK] Backend zaten calisiyor." -ForegroundColor Green
        $backendOk = $true
    }
} catch { }

if (-not $backendOk) {
    Write-Host "Backend baslatiliyor..." -ForegroundColor Yellow
    Start-Process -FilePath "cmd.exe" `
        -ArgumentList "/c cd /d `"$backendDir`" && python server.py" `
        -WindowStyle Normal

    Write-Host "Backend hazirlanıyor (8 saniye)..."
    Start-Sleep -Seconds 8

    try {
        $resp = Invoke-RestMethod "http://127.0.0.1:8000/health" -TimeoutSec 5 -ErrorAction Stop
        if ($resp.status -eq "ok") {
            Write-Host "[OK] Backend baslatildi." -ForegroundColor Green
            $backendOk = $true
        }
    } catch { }

    if (-not $backendOk) {
        Write-Warning "Backend baslatilamadi! Manuel olarak baslatip tekrar deneyin."
        Write-Host "  cd $backendDir" -ForegroundColor Gray
        Write-Host "  python server.py" -ForegroundColor Gray
    }
}

# ── adb reverse (kablo bağlantısı) ───────────────────────────────
if ($adb) {
    try {
        & $adb reverse tcp:8000 tcp:8000 2>$null | Out-Null
        Write-Host "[OK] adb reverse tcp:8000 kuruldu (kablo modu)." -ForegroundColor Green
    } catch {
        Write-Warning "adb reverse basarisiz. Cihaz bagli degil ya da USB hata ayıklama kapali olabilir."
    }
} else {
    Write-Host "[INFO] adb bulunamadi, Wi-Fi modu varsayiliyor." -ForegroundColor Yellow
    Write-Host "       Ayarlar ekranindan bilgisayar IP'sini girin: http://192.168.X.X:8000" -ForegroundColor Gray
}

# ── .env oluştur ─────────────────────────────────────────────────
$envFile = Join-Path $expoDir ".env"
"EXPO_PUBLIC_SERVER_URL=http://127.0.0.1:8000" | Set-Content $envFile -Encoding UTF8
Write-Host "[OK] .env dosyasi olusturuldu (127.0.0.1:8000)." -ForegroundColor Green

# ── Expo start ───────────────────────────────────────────────────
Write-Host ""
Write-Host "Expo baslatiliyor (cache temizleniyor)..." -ForegroundColor Cyan
Write-Host "QR kodu Expo Go ile okutun." -ForegroundColor Yellow
Write-Host ""

Set-Location $expoDir
npx expo start -c
