# WhisperLive PoC — Kurulum Rehberi

## Gereksinimler

- Python 3.10+
- Windows 10/11 (test edildi)
- Mikrofon (canlı test için)
- ~2 GB disk (Whisper `small` modeli)

## Phase 1: Kurulum

### 1. Repoyu klonlayın

```powershell
cd C:\Users\DELL\Desktop\Mobil\faster-whisper\live-whisper
git clone --depth 1 https://github.com/collabora/WhisperLive.git vendor/WhisperLive
```

### 2. Bağımlılıkları yükleyin

Tam `pip install -e` Windows'ta ONNX yol uzunluğu hatası verebilir. Bu PoC'de vendor + minimal paketler kullanılır:

```powershell
pip install PyAudio soundfile librosa scipy kaldialign psutil openai-whisper
```

> **Not:** `faster-whisper`, `torch`, `websocket-client` zaten ana projede yüklüyse tekrar gerekmez.

### 3. Sunucuyu başlatın

```powershell
cd C:\Users\DELL\Desktop\Mobil\faster-whisper\live-whisper
python start_server.py
```

Başarılı olduğunda port **9090** dinler:

```powershell
netstat -an | findstr "9090"
# TCP  0.0.0.0:9090  LISTENING
```

Sunucu ayarları (`start_server.py`):
- Backend: `faster_whisper`
- Model: istemci tarafından seçilir (varsayılan `small`)
- `max_connection_time`: **7200s** (2 saat — uzun dersler için)
- `omp_num_threads`: 4

---

## Phase 2: Canlı Mikrofon Client

Yeni terminal açın (sunucu çalışırken):

```powershell
cd C:\Users\DELL\Desktop\Mobil\faster-whisper\live-whisper

# Türkçe ders
python -m client.live_client --lang tr --lecture-id lecture-tr-001

# İngilizce
python -m client.live_client --lang en --lecture-id lecture-en-001
```

Çıktılar:
- `transcripts/<lecture_id>.json`
- `results/<lecture_id>_metrics.json`

---

## Phase 3: Ders Simülasyonu (dosya streaming)

Gerçek ders kayıtlarını canlı akış gibi test eder:

```powershell
# İngilizce (90 sn kısa test)
python -m client.lecture_simulation `
  --input "..\audio\#4 Python Tutorial for Beginners _ Variables in Python [TqPzwenhMj0].mp3" `
  --lang en --mode en --lecture-id test-en

# Türkçe
python -m client.lecture_simulation `
  --input "..\audio\Video Project-kisa.m4a" `
  --lang tr --mode tr --lecture-id test-tr

# 30 dakika test (dosyayı tamamen stream eder)
python -m client.lecture_simulation `
  --input "..\audio\Sudenaz's Meeting Notes_otter_ai.mp3" `
  --lang tr --mode tr --lecture-id lecture-30min

# 60 dakika: aynı dosyayı iki kez veya daha uzun dosya ile
python -m client.lecture_simulation `
  --input "..\audio\Sudenaz's Meeting Notes_otter_ai.mp3" `
  --lang tr --lecture-id lecture-60min
```

Karışık dil için `--mode mixed` ve `--lang` belirtmeyin (otomatik tespit).

---

## Sorun Giderme

| Sorun | Çözüm |
|-------|-------|
| `ModuleNotFoundError: whisper_live` | `vendor/WhisperLive` klonlandığından emin olun; client scriptleri otomatik ekler |
| Port 9090 meşgul | Eski sunucuyu kapatın veya `--port` değiştirin |
| Mikrofon çalışmıyor | PyAudio kurulumunu kontrol edin |
| Bağlantı 5 dk'da kesiliyor | `start_server.py` içinde `max_connection_time=7200` kullanın |
| Türkçe yanlış dil | `--lang tr` zorunlu |
