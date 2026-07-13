# Whisper Notes — Expo Go İstemcisi (SDK 54)

Android ve iOS'ta **Expo Go** ile sıfır kurulum test edilebilen React Native uygulaması.  
Backend olarak `../backend/server.py` (FastAPI + Faster-Whisper) kullanır.

---

## Hızlı Başlangıç

### 1. Tek komutla başlat (önerilen)

```powershell
# Proje kökünden:
.\scripts\run-expo-go.ps1
```

Script sırasıyla şunları yapar:
- Backend çalışıp çalışmadığını kontrol eder, gerekirse başlatır
- `adb reverse tcp:8000 tcp:8000` kurar (kablo bağlantısı)
- `.env` dosyasını oluşturur
- `npx expo start -c` ile Metro'yu başlatır

### 2. Manuel başlatma

```powershell
# Terminal 1 — Backend
cd backend
pip install -r requirements.txt
python server.py

# Terminal 2 — Expo
cd WhisperNotesExpo
cp .env.example .env          # Gerekirse IP'yi düzenle
adb reverse tcp:8000 tcp:8000  # Kablo bağlantısı için
npx expo start -c
```

---

## Sunucu URL Ayarı

| Bağlantı türü | URL |
|---|---|
| Kablo (adb reverse) | `http://127.0.0.1:8000` |
| Aynı Wi-Fi ağı | `http://[BİLGİSAYAR_IP]:8000` |

Uygulama içi **Ayarlar** ekranından da değiştirilebilir.

---

## Proje Yapısı

```
WhisperNotesExpo/
├── app/                     # Expo Router ekranları
│   ├── _layout.tsx          # Kök layout, AppProvider, NativeWind CSS
│   ├── index.tsx            # Ana ekran
│   ├── upload.tsx           # Dosya yükleme
│   ├── record.tsx           # Canlı kayıt
│   ├── processing.tsx       # İşlem ekranı
│   ├── result.tsx           # Sonuç + kopyala/paylaş
│   ├── notes.tsx            # Kayıtlı notlar
│   └── settings.tsx         # Sunucu URL, model, dil
├── src/
│   ├── api/whisperClient.ts # POST /transcribe, GET /health
│   ├── context/AppContext.tsx
│   ├── hooks/useRecording.ts
│   ├── storage/noteStore.ts # expo-file-system ile notes.json
│   ├── theme/colors.ts
│   ├── types/note.ts
│   └── utils/export.ts
├── metro.config.js          # Expo SDK 54 + NativeWind v4
├── babel.config.js          # babel-preset-expo + reanimated
├── tailwind.config.js       # NativeWind preset
└── global.css               # @tailwind directives
```

---

## SDK 54 Uyumluluk (expo-doctor: 18/18 ✓)

| Paket | Versiyon |
|---|---|
| expo | ~54.0.0 |
| react | 19.1.0 |
| react-native | 0.81.5 |
| expo-router | ~6.x |
| expo-audio | ~1.1.x |
| react-native-reanimated | ~4.x |
| nativewind | ^4.1.x |
| tailwindcss | 3.4.x |

---

## Özellikler

- **Dosya yükleme** — MP3, WAV, M4A, FLAC, MP4, MKV → transkript
- **Canlı kayıt** — kayıt bitince `/transcribe`'a gönderilir (Expo Go uyumlu)
- **Notlar** — `expo-file-system` üzerinde `notes.json` olarak kalıcı depolama
- **Dışa aktarma** — Pano kopyala + paylaş
- **Karanlık tema** — Android uygulamasıyla aynı renk paleti

---

## Not: Expo Go Kısıtlamaları

Expo Go ile gerçek zamanlı WebSocket yayını (streaming) desteklenmez.  
Canlı kayıt → **kayıt tamamlandıktan sonra** sunucuya yüklenir.  
Tam gerçek zamanlı akış için `expo run:android` (native build) kullanın.
