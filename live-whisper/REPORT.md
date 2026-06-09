# WhisperLive PoC — Benchmark & Feasibility Report

**Proje:** Üniversite ders asistanı mobil uygulaması için gerçek zamanlı transkripsiyon değerlendirmesi  
**Tarih:** 2026-06-05  
**Platform:** Windows 11, Python 3.12, CPU (int8), Whisper `small`  
**Repo:** [collabora/WhisperLive](https://github.com/collabora/WhisperLive)

---

## 1. Installation Experience

| Adım | Durum | Not |
|------|-------|-----|
| Repo klonlama | ✅ Başarılı | `vendor/WhisperLive` |
| `pip install -e vendor/WhisperLive` | ❌ Başarısız | Windows `[WinError 206]` — ONNX test verisi yol uzunluğu |
| Minimal bağımlılık + PYTHONPATH | ✅ Başarılı | PoC workaround |
| Sunucu başlatma | ✅ Başarılı | Port 9090 LISTENING |
| İstemci bağlantısı | ✅ Başarılı | WebSocket + faster_whisper backend |

**Kurulum zorluk puanı:** 6/10 (Windows'ta tam pip kurulumu sorunlu; vendor workaround ile çalışıyor)

---

## 2. Test Sonuçları

### 2.1 İngilizce (Phase 3 — dosya simülasyonu)

**Dosya:** Python Tutorial MP3 (~11 dk)  
**Ayar:** `--lang en --model small`

| Metrik | Değer |
|--------|-------|
| İlk segment gecikmesi | ~30 sn (model yükleme dahil) |
| Segment aralığı (ortalama) | ~5–7 sn |
| Transkripsiyon kalitesi | **İyi** — teknik terimler doğru (variable, string, index) |
| Kararlılık | ✅ Kesinti yok |
| JSON kayıt | ✅ Anlık persist çalışıyor |

Örnek çıktı:
> "Welcome back Aliens, my name is Da Vin Reddy and let's continue with this series on Python."

### 2.2 Türkçe

**Kaynak:** Ana projede `faster-whisper` batch testleri + WhisperLive mimarisi

| Metrik | Değerlendirme |
|--------|---------------|
| Dil tespiti (otomatik) | ⚠️ Riskli — Galce/İngilizce karışıklığı görüldü |
| `--lang tr` ile | ✅ Batch modda iyi sonuç |
| Canlı mod tahmini | `--lang tr` zorunlu; kalite batch'e yakın |
| Teknik terimler | Orta — İngilizce terimler karışık geçebilir |

**Öneri:** Türkçe derslerde her zaman `lang=tr` gönderin.

### 2.3 Karışık Türkçe-İngilizce

| Durum | Değerlendirme |
|-------|---------------|
| Otomatik dil | ❌ Önerilmez |
| Sabit `lang=tr` | Türkçe ağırlıklı derslerde kabul edilebilir |
| Sabit `lang=en` | İngilizce ağırlıklı derslerde kabul edilebilir |
| Gerçek code-switching | ⚠️ Whisper sınırlı — terimler yanlış dilde yazılabilir |

---

## 3. Performans Metrikleri

### Ölçülen (İngilizce canlı simülasyon, CPU)

| Metrik | Değer |
|--------|-------|
| Ortalama segment gecikmesi | ~5–7 sn |
| CPU kullanımı | %40–80 (4 thread) |
| RAM (istemci) | ~200–400 MB |
| RAM (sunucu + model) | ~1.5–2 GB |
| RTF (canlı) | ~0.3–0.5 (batch'ten daha iyi) |

### 30 / 60 Dakika Uzun Süre Testi

| Test | Durum | Not |
|------|-------|-----|
| 30 dk | 🔶 Hazır (script) | `Sudenaz's Meeting Notes` (~35 dk) ile çalıştırılabilir |
| 60 dk | 🔶 Hazır (script) | `max_connection_time=7200` ayarlandı |
| Bu raporda tam koşu | Kısmi | 7+ dk İngilizce simülasyon başarılı; tam 30/60 dk manuel test gerekir |

**Uzun süre riskleri:**
- Varsayılan WhisperLive `max_connection_time=300` (5 dk) — PoC'de 7200'ye çıkarıldı
- Bellek sızıntısı: uzun oturumlarda izlenmeli
- WebSocket kopması: mobil ağda kritik

---

## 4. Phase 5 — Mobile App Feasibility

### Flutter Mobil Uygulama

| Konu | Değerlendirme |
|------|---------------|
| Mimari uyum | ✅ Uygun — WebSocket ile ses stream |
| İstemci | Flutter `web_socket_channel` + `record` paketi |
| Referans | WhisperLive'in [iOS client](https://github.com/collabora/WhisperLive/tree/main/Audio-Transcription-iOS) mevcut |
| Latency | 3–7 sn — ders notu için kabul edilebilir, altyazı için yavaş |

**Akış:**
```
Flutter App → mikrofon PCM → WebSocket → WhisperLive Server → segment JSON → Firebase
```

### Firebase Backend

| Bileşen | Rol |
|---------|-----|
| Firebase Realtime DB / Firestore | Segment'leri anlık kaydet |
| Cloud Functions | Opsiyonel — segment post-processing |
| WhisperLive sunucusu | **Firebase'de değil** — ayrı VM/GPU sunucu gerekir |

> WhisperLive sunucu tarafı ağır (model inference). Firebase Functions'ta çalıştırılamaz.

### Avantajlar

- Açık kaynak, aktif geliştirme (4k+ star)
- WebSocket protokolü mobil için uygun
- `faster_whisper` backend — batch PoC ile tutarlı
- VAD, word timestamps, çoklu istemci desteği
- iOS referans client mevcut

### Dezavantajlar

- Gerçek zamanlı ≠ anlık (3–7 sn gecikme)
- CPU'da ölçeklenme zayıf (öğrenci başına sunucu yükü)
- Windows kurulumu zahmetli
- Türkçe için dil zorlama şart
- `max_connection_time` ve bağlantı yönetimi yapılandırma gerektirir
- Karışık dil desteği sınırlı

### Beklenen Sunucu Maliyeti (tahmini)

| Senaryo | Sunucu | Aylık maliyet (tahmini) |
|---------|--------|-------------------------|
| PoC / 10 eşzamanlı | 1× CPU 8 vCPU | $40–80 |
| Üretim / 50 eşzamanlı | 1× GPU T4 + load balancer | $200–400 |
| Üretim / 200 eşzamanlı | 2–4× GPU + k8s | $800–2000 |

*Not: GPU ile RTF ~0.05–0.1; aynı donanımda 5–10× daha fazla öğrenci.*

### Ölçeklenebilirlik

| Darboğaz | Etki |
|----------|------|
| Model inference | Her aktif stream CPU/GPU tüketir |
| `max_clients=4` (varsayılan) | 4 eşzamanlı ders = yeni sunucu gerekir |
| WebSocket bağlantı süresi | 60+ dk ders = uzun bağlantı yönetimi |
| Ağ kalitesi | Mobil kampüs WiFi kopmaları transcript boşlukları yaratır |

**Ölçekleme stratejisi:** GPU sunucu havuzu + bağlantı başına oturum yönlendirme + segment checkpoint (Firebase).

---

## 5. Production Readiness Score

| Kriter | Puan (1–10) |
|--------|-------------|
| Kurulum kolaylığı | 5 |
| Türkçe kalite | 7 (lang=tr ile) |
| İngilizce kalite | 8 |
| Karışık dil | 4 |
| Latency | 6 |
| Uzun süre kararlılık | 6 (yapılandırma ile) |
| Mobil entegrasyon | 7 |
| Ölçeklenebilirlik | 5 (CPU) / 8 (GPU) |
| Maliyet verimliliği | 6 |

### **Genel Production Readiness: 6.5 / 10**

**Sonuç:** WhisperLive, üniversite ders asistanı için **uygun bir aday** — ancak:
1. Türkçe için `lang=tr` zorunlu
2. GPU sunucu şart (50+ öğrenci için)
3. Mobil → WebSocket → sunucu mimarisi; Firebase yalnızca transcript depolama
4. 30/60 dk kararlılık testleri üretim öncesi tamamlanmalı

---

## 6. Proje Dosya Yapısı

```
live-whisper/
├── SETUP.md              # Kurulum rehberi
├── REPORT.md             # Bu rapor
├── start_server.py       # Sunucu başlatıcı
├── client/
│   ├── live_client.py    # Mikrofon canlı client
│   ├── lecture_simulation.py  # Ders simülasyonu
│   ├── transcript_store.py    # JSON persist
│   └── metrics_monitor.py     # CPU/RAM/latency
├── transcripts/          # Kayıtlı transkriptler
├── results/              # Metrik JSON'ları
└── vendor/WhisperLive/   # Upstream repo
```

---

## 7. Sonraki Adımlar

1. Tam 30 ve 60 dakikalık Türkçe/İngilizce simülasyon testlerini çalıştırın
2. GPU sunucuda aynı testleri tekrarlayın
3. Flutter prototip: WebSocket + segment → Firestore
4. Ağ kopması / reconnect senaryosunu test edin
5. `medium` model ile Türkçe doğruluk karşılaştırması
