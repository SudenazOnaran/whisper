# WhisperNotes — Sistem Mimarisi ve Teknik Analiz

## 1. Genel Bakış

Bu uygulama, herhangi bir bulut API'si veya abonelik ücreti ödemeden **tamamen kendi bilgisayarında** çalışan bir yapay zeka sesi-metne dönüştürme sistemidir. Model internetten bir kez indirilir, sonrasında tamamen **çevrimdışı** çalışır.

```
Telefon (Expo Go / Android)
        │
        │  HTTP POST /transcribe  (ses dosyası)
        │  HTTP GET  /health
        ▼
Bilgisayar — FastAPI Backend (Python)
        │
        │  Python kütüphanesi çağrısı (process içi)
        ▼
faster-whisper (CTranslate2 motoru)
        │
        │  GGML/CT2 format model dosyaları
        ▼
Whisper modeli (openai/whisper-small veya medium)
```

---

## 2. Ses Verisi Nereden Geliyor?

Modeli biz yazmadık, açık kaynaklı bir repodan aldık: **openai/whisper**.

### Orijinal kaynak
- **OpenAI Whisper** → `github.com/openai/whisper` (Eylül 2022'de yayınlandı)
- MIT lisansı altında açık kaynak — ticari kullanım dahil serbestçe kullanılabilir

### Biz ne kullandık?
Orijinal Whisper yerine **faster-whisper** (`github.com/SYSTRAN/faster-whisper`) kullandık.

Bu, Whisper'ın optimize edilmiş bir yeniden uygulamasıdır:
- **CTranslate2** çalışma zamanı üzerinde çalışır (NVIDIA'nın TensorRT benzeri bir CPU/GPU çıkarım motoru)
- Orijinal PyTorch Whisper'a göre **4× daha hızlı**, **%70 daha az RAM** kullanır
- Model ağırlıkları **int8** (8-bit tam sayı) olarak kuantize edilmiştir — doğruluk kaybı minimumdur

### Model dosyaları nerede saklanır?
İlk `transcribe` çağrısında HuggingFace'den indirilir:
```
~/.cache/huggingface/hub/models--Systran--faster-whisper-small/
~/.cache/huggingface/hub/models--Systran--faster-whisper-medium/
```

Sonrasında internet bağlantısı gerekmez.

---

## 3. Backend Akışı (server.py Adım Adım)

### 3.1 Sunucu başlangıcı
```python
get_model("small")  # Uygulama başlarken small modeli RAM'e yükle
```
Model bir kez yüklenir, sonraki isteklerde RAM'den servis edilir.
`small` için yaklaşık **~500 MB RAM** kullanılır.

### 3.2 Dosya yükleme akışı (`POST /transcribe`)

```
Telefon  ──► multipart/form-data ──► FastAPI
              (ses/video dosyası,
               language, model_size)
```

**Adım 1 — Geçici dosyaya yaz**
```python
content = await file.read()
with open(tmp_input, "wb") as f:
    f.write(content)
```
Dosya RAM'den disk'e yazılır (geçici klasör).

**Adım 2 — Video mu? FFmpeg ile sesi çıkar**
```python
# .mp4, .mkv, .avi, .mov, .webm ise:
ffmpeg -i input.mp4 -vn -acodec pcm_s16le -ar 16000 -ac 1 output.wav
```
- `-vn` → görüntüyü at
- `-acodec pcm_s16le` → 16-bit ham PCM
- `-ar 16000` → 16 kHz örnekleme (Whisper'ın beklediği format)
- `-ac 1` → mono

**Adım 3 — Whisper ile transkript**
```python
model.transcribe(audio_path, beam_size=5, vad_filter=True, language="tr")
```
- `beam_size=5` → 5 olasılık dalı aynı anda hesaplanır, en iyisi seçilir
- `vad_filter=True` → Ses Aktivite Tespiti (VAD) — sessiz bölümler atlanır, hız artar
- `language` → belirtilmezse otomatik tespit edilir

**Adım 4 — Segmentlere böl ve döndür**
```json
{
  "success": true,
  "language": "tr",
  "language_probability": 0.9987,
  "full_text": "Bugün derste işlenen konu...",
  "segments": [
    {"time": "00:03", "text": "Bugün derste işlenen konu", "start": 3.2, "end": 6.8},
    {"time": "00:07", "text": "diferansiyel denklemler", "start": 7.1, "end": 9.5}
  ]
}
```

**Adım 5 — Geçici dosyaları sil**
```python
os.unlink(tmp_input)   # Orijinal dosya
os.unlink(audio_path)  # FFmpeg çıktısı (varsa)
```
Ses verisi sunucuda **hiçbir zaman kalıcı olarak saklanmaz**.

---

## 4. Model Karşılaştırması

| Model | Parametre | Disk (CT2 int8) | RAM | Hız (1 dk ses, CPU) | Doğruluk |
|-------|-----------|-----------------|-----|---------------------|----------|
| `tiny` | 39M | ~40 MB | ~150 MB | ~5-10 sn | Düşük |
| `base` | 74M | ~75 MB | ~250 MB | ~10-20 sn | Orta-düşük |
| **`small`** | **244M** | **~240 MB** | **~500 MB** | **~20-40 sn** | **İyi** |
| `medium` | 769M | ~750 MB | ~1.5 GB | ~60-120 sn | Çok iyi |
| `large-v3` | 1550M | ~1.5 GB | ~3 GB | ~180-300 sn | En iyi |

> Uygulamada **small** varsayılan, **medium** seçilebilir.
> Large modeller bu projede aktif değil ama `get_model("large-v3")` ile eklenebilir.

---

## 5. Performans Gerçekçi Beklentileri

### CPU'da (mevcut kurulum)
| Senaryo | Süre |
|---------|------|
| 1 dakikalık ders kaydı, small | 20–45 saniye |
| 1 dakikalık ders kaydı, medium | 1–2 dakika |
| 15 saniyelik chunk (canlı kayıt), small | 5–12 saniye |
| Video dosyası (MP4, 5 dk), small | 2–4 dakika |

> **Hız RTX/GTX GPU'da 4–8× artar.** GPU varsa `device="cuda"` ve `compute_type="float16"` ile dramatik hızlanma sağlanır.

### Doğruluk
- Türkçe transkripsiyon: **%90–95** (small), **%95–98** (medium) — temiz ses için
- Arka plan gürültüsü, aksanlı konuşma, birden fazla kişi → doğruluk düşer
- `vad_filter=True` sessiz kısımları atlayarak hem hızlanır hem yanlış pozitif azalır

---

## 6. Canlı Kayıt: İki Yöntem

### Yöntem A — WebSocket Akışı (Android native app)
```
Telefon ──► 16kHz PCM chunk ──► WebSocket /ws/live ──► 3 saniyelik parçalar işlenir
```
- Gerçek anlamda anlık: her 3 saniyede metin üretilir
- Düşük gecikme
- **Expo Go'da desteklenmez** (native AudioRecord API gerektirir)

### Yöntem B — Chunk Upload (Expo Go — mevcut uygulama)
```
Telefon ──► 15 sn kayıt ──► dur ──► /transcribe ──► metin ──► yeniden başla
```
- Her 15 saniyede bir parça gönderilir
- Expo Go ile %100 uyumlu
- Gecikme: ses süresi + işlem süresi (~5–15 sn)
- Kelime başı gecikme yok, cümle/paragraf bazlı akış

---

## 7. Veri Gizliliği

| Ne? | Nerede kalıyor? |
|-----|-----------------|
| Ses dosyası | **Hiçbir yerde.** İşlendikten sonra silinir. |
| Transkript metni | Sadece telefon yerel deposunda (`notes.json`) |
| Model ağırlıkları | Bilgisayar cache'i (`~/.cache/huggingface/`) |
| Kullanıcı verisi | Hiçbir sunucuya gönderilmez |

Tüm işlem **yerel ağda** gerçekleşir. OpenAI veya başka bir servis kullanılmaz.

---

## 8. Lisans Durumu

| Bileşen | Lisans | Kısıt |
|---------|--------|-------|
| OpenAI Whisper (model) | MIT | Yok — ticari kullanım serbest |
| faster-whisper | MIT | Yok |
| CTranslate2 | MIT | Yok |
| FastAPI | MIT | Yok |
| React Native / Expo | MIT | Yok |
| NativeWind | MIT | Yok |

Play Store veya App Store'a yayınlamak için ek lisans gerekmez.

---

## 9. Mimari Özeti

```
┌─────────────────────────────────────────────────────────┐
│                   Telefon (Expo Go)                     │
│  React Native + Expo Router + NativeWind                │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │
│  │ Kayıt    │ │ Yükleme  │ │ Sonuç    │ │  Notlar  │  │
│  │ (15s     │ │ (dosya   │ │ (metin + │ │(notes.   │  │
│  │  chunk)  │ │  seçici) │ │ segment) │ │ json)    │  │
│  └────┬─────┘ └────┬─────┘ └──────────┘ └──────────┘  │
└───────┼────────────┼────────────────────────────────────┘
        │ HTTP POST  │ HTTP POST
        │ /transcribe│ /transcribe
┌───────▼────────────▼────────────────────────────────────┐
│              Bilgisayar — FastAPI (Python)               │
│  uvicorn + CORS + multipart parser                      │
│  ┌─────────────────────────────────────────────────┐   │
│  │  FFmpeg (video → WAV, 16kHz mono)               │   │
│  │  faster-whisper (CTranslate2 + int8 model)      │   │
│  │  VAD filtresi + beam search (beam=5)             │   │
│  └─────────────────────────────────────────────────┘   │
│  Model: ~/.cache/huggingface/  (HF'den bir kez indirildi)│
└─────────────────────────────────────────────────────────┘
```

---

## 10. Geliştirme Önerileri

| Öncelik | Değişiklik | Kazanım |
|---------|-----------|---------|
| Yüksek | GPU desteği (`device="cuda"`) | 4–8× hız |
| Yüksek | `large-v3` model seçeneği | +%3-5 doğruluk |
| Orta | Chunk süresini ayarlanabilir yap (5/10/15/30s) | Kullanıcı tercihi |
| Orta | Diarization (kim konuştu) — `pyannote.audio` | Çoklu konuşmacı |
| Düşük | Yerel model (on-device) — `whisper.rn` | İnternet gerekmez |
| Düşük | Railway/Render'da deploy | Uzaktan erişim |
