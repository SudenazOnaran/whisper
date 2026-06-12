# Whisper Notes

Ses ve video dosyalarını yapay zeka ile anında metne dönüştüren Android uygulaması.  
Backend: **faster-whisper** · Android: **Kotlin / Jetpack Compose**

---

## Proje Yapısı

```
faster-whisper/
├── backend/          ← Python API sunucusu
│   ├── server.py
│   ├── requirements.txt
│   ├── Dockerfile
│   ├── railway.toml  ← Railway deploy config
│   └── render.yaml   ← Render deploy config
└── WhisperNotesApp/  ← Android uygulaması
```

---

## 1. Backend — Yerel Çalıştırma

```bash
cd backend
pip install -r requirements.txt
python server.py
```

Sunucu `http://0.0.0.0:8000` adresinde başlar.

| Endpoint       | Açıklama                        |
|----------------|---------------------------------|
| `GET /health`  | Sunucu sağlık kontrolü          |
| `POST /transcribe` | Dosya yükle → transkript al |
| `WS /ws/live`  | Canlı ses akışı                 |

---

## 2. Backend — Cloud Deploy (Railway)

```bash
# Railway CLI ile
npm install -g @railway/cli
railway login
cd backend
railway up
```

Ya da [railway.app](https://railway.app) → New Project → Deploy from GitHub.

Render için: [render.com](https://render.com) → New Web Service → `render.yaml` otomatik algılanır.

---

## 3. Android Uygulaması — Build ve Test

### Debug (test için)

```powershell
cd WhisperNotesApp

# Debug APK
.\gradlew.bat assembleDebug

# Telefona yükle
.\gradlew.bat installDebug
```

### Telefonu bağlayıp doğrudan test

USB hata ayıklamayı açıp telefonu onayladıktan sonra proje kökünden:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\run-phone-debug.ps1
```

### Release (Play Store için)

Play Store için APK yerine AAB üretin. Keystore şifrelerini repoya veya README'ye yazmayın; sadece yerel ortam değişkeni ya da güvenli secret store kullanın.

```powershell
cd WhisperNotesApp
$env:KEYSTORE_PATH="$PWD\release.jks"
$env:KEYSTORE_PASSWORD="..."
$env:KEY_ALIAS="whisper-notes"
$env:KEY_PASSWORD="..."
$env:WHISPER_SERVER_URL="https://your-production-server.example"

.\gradlew.bat bundleRelease
```

AAB: `app/build/outputs/bundle/release/app-release.aab`

### Emülatörde Test

```bash
# Sunucuyu başlat, sonra ADB yönlendirme kur
python backend/server.py
adb reverse tcp:8000 tcp:8000
cd WhisperNotesApp
.\gradlew.bat installDebug
```

---

## 4. Play Store için Gereksinimler

- [ ] Backend'i public HTTPS sunucuya deploy et (Railway/Render)
- [ ] `WHISPER_SERVER_URL` ortam değişkenini gerçek sunucu URL'iyle ayarla
- [ ] Release AAB üret (`.\gradlew.bat bundleRelease`)
- [ ] Play Console'da uygulama oluştur
- [ ] AAB dosyasını yükle
- [ ] Mikrofon ve sesin sunucuya gönderilmesi için gizlilik politikası/Data Safety formunu tamamla

> **Not:** `release.jks` dosyasını güvenli yerde sakla ve şifresini paylaşma. Kaybolursa Play Store güncellemesi yapılamaz.

---

## Desteklenen Formatlar

Audio: `mp3` `wav` `m4a` `flac` `ogg`  
Video: `mp4` `mkv` `avi` `mov` `webm`

## Desteklenen Diller

`tr` Türkçe · `en` İngilizce · `auto` Otomatik algılama (100+ dil)
