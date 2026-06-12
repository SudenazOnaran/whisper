# Whisper Notes Android

Kotlin + Jetpack Compose Android istemcisi. Uygulama iki akış sunar:

- Canlı derste mikrofon kaydı ile anlık transkript
- Ses/video dosyası yükleyip transkript

Backend dosyaları geçici işler; Android tarafında yalnızca oluşturulan notlar uygulama özel alanındaki `notes.json` dosyasında kalıcı tutulur.

## Yerel Test

Android proje dizininden tek komut:

```powershell
powershell -ExecutionPolicy Bypass -File ..\scripts\run-phone-debug.ps1
```

Bu komut backend'i başlatır, `adb reverse tcp:8000 tcp:8000` kurar, debug uygulamasını telefona yükler ve açar.

## Build

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

## Play Store AAB

Release build için gerçek HTTPS backend URL'i ve keystore bilgileri ortam değişkenlerinden verilmelidir.

```powershell
$env:KEYSTORE_PATH="$PWD\release.jks"
$env:KEYSTORE_PASSWORD="..."
$env:KEY_ALIAS="whisper-notes"
$env:KEY_PASSWORD="..."
$env:WHISPER_SERVER_URL="https://your-production-server.example"

.\gradlew.bat bundleRelease
```

Keystore ve şifreleri repoya eklemeyin. Play Console için gizlilik politikası ve Data Safety formunda mikrofon kullanımı ile sesin transkripsiyon sunucusuna gönderildiği açıkça belirtilmelidir.
