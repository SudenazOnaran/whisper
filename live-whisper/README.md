# live-whisper — Bağımsız WhisperLive Deney Klasörü

> **Bu klasör ana projeyle bağlantısız bir deneydir.**
> Mobil uygulamalar `backend/server.py` üzerinden çalışır.

## Ana proje mimarisi

```
backend/server.py  (port 8000)
├── POST /transcribe  ← Dosya yükleme  (WhisperNotesApp + Expo Go)
└── WS   /ws/live     ← Canlı PCM     (WhisperNotesApp Android, gerçek zamanlı)
```

## Bu klasör ne?

WhisperLive (Collabora) kütüphanesini test etmek için oluşturulmuş PoC.
Port **9090** üzerinde çalışır, ana projeden bağımsızdır.

## Kullanmak istersen

```powershell
# WhisperLive kütüphanesini klonla
git clone --depth 1 https://github.com/collabora/WhisperLive.git vendor/WhisperLive

# Bağımlılıkları kur
pip install PyAudio soundfile librosa scipy

# Sunucuyu başlat (port 9090)
python start_server.py

# İstemciyi başlat (ayrı terminalde)
python -m client.live_client --lang tr --lecture-id test-001
```

Detaylar: `SETUP.md`
