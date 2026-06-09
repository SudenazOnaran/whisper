# live-whisper — WhisperLive PoC

Üniversite ders asistanı mobil uygulaması için gerçek zamanlı transkripsiyon değerlendirme projesi.

## Hızlı Başlangıç

```powershell
# Terminal 1 — Sunucu
python start_server.py

# Terminal 2 — Canlı mikrofon (Türkçe)
python -m client.live_client --lang tr --lecture-id my-lecture

# Terminal 2 — Dosya simülasyonu (İngilizce)
python -m client.lecture_simulation --input "..\audio\dosya.mp3" --lang en --lecture-id test-en
```

## Dokümantasyon

- [SETUP.md](SETUP.md) — Kurulum adımları
- [REPORT.md](REPORT.md) — Benchmark ve mobil uygulama fizibilite raporu

## Özellikler

- WhisperLive WebSocket sunucusu (`faster_whisper` backend)
- Canlı mikrofon client
- Ders simülasyonu (dosya streaming)
- JSON transcript persistence (ses kaydedilmez)
- CPU/RAM/latency metrikleri
