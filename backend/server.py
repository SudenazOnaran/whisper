"""
WhisperNotes Backend Server
- POST /transcribe  : dosya yükle → faster-whisper ile transkript
- WS   /ws/live     : canlı ses akışı → gerçek zamanlı transkript
"""

import asyncio
import io
import os
import subprocess
import tempfile
import wave
from pathlib import Path
from typing import Optional

import uvicorn
from fastapi import FastAPI, File, Form, UploadFile, WebSocket, WebSocketDisconnect
from fastapi.middleware.cors import CORSMiddleware
from faster_whisper import WhisperModel

app = FastAPI(title="WhisperNotes Server", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

_models: dict = {}

VIDEO_EXTENSIONS = {".mp4", ".mkv", ".avi", ".mov", ".webm"}


def get_model(size: str = "small") -> WhisperModel:
    if size not in _models:
        print(f"[Server] Whisper modeli yükleniyor: '{size}'...")
        _models[size] = WhisperModel(size, device="cpu", compute_type="int8")
        print(f"[Server] Model hazır: {size}")
    return _models[size]


def normalize_to_wav(input_path: str) -> tuple[str, bool]:
    """
    Her dosyayı 16kHz mono WAV'a normalize et.
    - Video → ses çıkar + normalize
    - M4A/MP3/OGG → WAV'a çevir (Whisper PCM ister, AAC decoder yükü atar)
    - WAV → 16kHz mono değilse yeniden örnekle
    Döner: (wav_path, should_delete)
    """
    suffix = Path(input_path).suffix.lower()
    # Zaten 16kHz WAV olduğunu bilemeyiz → her zaman normalize et
    out = tempfile.mktemp(suffix=".wav")
    cmd = [
        "ffmpeg", "-y", "-i", input_path,
        "-vn",                   # video kanalını at
        "-acodec", "pcm_s16le",  # 16-bit PCM
        "-ar", "16000",          # 16 kHz — Whisper'ın beklediği örnekleme hızı
        "-ac", "1",              # mono
        out
    ]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if result.returncode != 0:
        # FFmpeg başarısız → orijinali dene
        print(f"[Warn] FFmpeg normalize başarısız, orijinal kullanılıyor: {result.stderr[:200]}")
        return input_path, False
    return out, True


@app.on_event("startup")
async def startup():
    print("[Server] Başlatılıyor... Whisper modelleri önceden yükleniyor")
    get_model("tiny")   # canlı kayıt için — hızlı (~1-2 sn/chunk)
    get_model("small")  # dosya yükleme için — dengeli kalite
    print("[Server] Hazır! Port 8000")


@app.get("/health")
async def health():
    return {"status": "ok", "models_loaded": list(_models.keys())}


@app.post("/transcribe")
async def transcribe(
    file: UploadFile = File(...),
    language: Optional[str] = Form(default=None),
    model_size: str = Form(default="small"),
):
    """
    Ses/video dosyası yükle, transkript al.
    """
    suffix = Path(file.filename or "audio.wav").suffix.lower() or ".wav"
    tmp_input = tempfile.mktemp(suffix=suffix)
    audio_path = None
    should_delete_audio = False

    try:
        # Dosyayı geçici konuma kaydet
        content = await file.read()
        with open(tmp_input, "wb") as f:
            f.write(content)

        print(f"[Transcribe] Dosya alındı: {file.filename} ({len(content)//1024} KB)")

        # Her dosyayı 16kHz mono WAV'a normalize et (M4A, MP3, video dahil)
        audio_path, should_delete_audio = normalize_to_wav(tmp_input)

        # Whisper parametreleri
        model = get_model(model_size)
        kwargs: dict = {
            "beam_size": 5,
            # VAD: kısa chunklarda agresif VAD sesi kesebilir.
            # Telefon kayıtları için daha yumuşak eşik kullanıyoruz.
            "vad_filter": True,
            "vad_parameters": {
                "min_silence_duration_ms": 500,   # 300→500 ms: kısa sessizlikte kesmez
                "threshold": 0.3,                  # 0.5→0.3: daha düşük eşik, daha fazla ses yakalar
                "speech_pad_ms": 400,              # konuşma başı/sonu dolgusu
            },
        }
        if language and language not in ("auto", ""):
            kwargs["language"] = language

        print(f"[Transcribe] İşleniyor... dil={language or 'oto'}, model={model_size}")
        segments_gen, info = model.transcribe(audio_path, **kwargs)

        segments = []
        full_parts = []
        for seg in segments_gen:
            text = seg.text.strip()
            if text:
                mins = int(seg.start // 60)
                secs = int(seg.start % 60)
                segments.append({
                    "time": f"{mins:02d}:{secs:02d}",
                    "text": text,
                    "start": round(seg.start, 2),
                    "end": round(seg.end, 2),
                })
                full_parts.append(text)

        full_text = "\n".join(full_parts)
        print(f"[Transcribe] Tamamlandı. Dil: {info.language}, {len(segments)} segment")

        return {
            "success": True,
            "language": info.language,
            "language_probability": round(info.language_probability, 4),
            "full_text": full_text,
            "segments": segments,
        }

    except Exception as e:
        print(f"[Transcribe] HATA: {e}")
        return {"success": False, "error": str(e), "full_text": "", "segments": []}

    finally:
        if os.path.exists(tmp_input):
            os.unlink(tmp_input)
        if should_delete_audio and audio_path and os.path.exists(audio_path):
            os.unlink(audio_path)


@app.websocket("/ws/live")
async def live_transcription(websocket: WebSocket):
    """
    Canlı ses akışı WebSocket.
    Telefon 16kHz mono 16-bit PCM gönderir,
    sunucu 3 saniyelik chunk'lar halinde transkript döndürür.
    """
    await websocket.accept()
    model = get_model("small")

    SAMPLE_RATE = 16000
    BYTES_PER_SAMPLE = 2  # 16-bit
    CHUNK_SECONDS = 3
    CHUNK_SIZE = SAMPLE_RATE * BYTES_PER_SAMPLE * CHUNK_SECONDS

    audio_buffer = bytearray()
    chunk_index = 0

    print("[Live] WebSocket bağlantısı açıldı")

    try:
        while True:
            try:
                data = await asyncio.wait_for(websocket.receive_bytes(), timeout=15.0)
                audio_buffer.extend(data)

                while len(audio_buffer) >= CHUNK_SIZE:
                    chunk = bytes(audio_buffer[:CHUNK_SIZE])
                    audio_buffer = audio_buffer[CHUNK_SIZE:]
                    chunk_index += 1

                    # Chunk'ı WAV formatına çevir ve işle
                    with tempfile.NamedTemporaryFile(delete=False, suffix=".wav") as tmp:
                        with wave.open(tmp.name, "wb") as wf:
                            wf.setnchannels(1)
                            wf.setsampwidth(BYTES_PER_SAMPLE)
                            wf.setframerate(SAMPLE_RATE)
                            wf.writeframes(chunk)
                        tmp_path = tmp.name

                    try:
                        segs, info = model.transcribe(
                            tmp_path,
                            beam_size=3,
                            vad_filter=True,
                            vad_parameters={
                                "min_silence_duration_ms": 500,
                                "threshold": 0.3,
                                "speech_pad_ms": 400,
                            },
                        )
                        texts = [s.text.strip() for s in segs if s.text.strip()]
                        if texts:
                            combined = " ".join(texts)
                            await websocket.send_json({
                                "type": "transcript",
                                "text": combined,
                                "language": info.language,
                                "chunk": chunk_index,
                            })
                    finally:
                        if os.path.exists(tmp_path):
                            os.unlink(tmp_path)

            except asyncio.TimeoutError:
                # Kalan buffer'ı işle
                if len(audio_buffer) > SAMPLE_RATE * BYTES_PER_SAMPLE:
                    with tempfile.NamedTemporaryFile(delete=False, suffix=".wav") as tmp:
                        with wave.open(tmp.name, "wb") as wf:
                            wf.setnchannels(1)
                            wf.setsampwidth(BYTES_PER_SAMPLE)
                            wf.setframerate(SAMPLE_RATE)
                            wf.writeframes(bytes(audio_buffer))
                        tmp_path = tmp.name
                    audio_buffer.clear()
                    try:
                        segs, info = model.transcribe(
                            tmp_path, beam_size=3, vad_filter=True,
                            vad_parameters={"min_silence_duration_ms": 500, "threshold": 0.3}
                        )
                        texts = [s.text.strip() for s in segs if s.text.strip()]
                        if texts:
                            await websocket.send_json({
                                "type": "transcript",
                                "text": " ".join(texts),
                                "language": info.language,
                                "chunk": chunk_index + 1,
                            })
                    finally:
                        if os.path.exists(tmp_path):
                            os.unlink(tmp_path)
                continue

    except WebSocketDisconnect:
        print("[Live] WebSocket bağlantısı kapatıldı")
    except Exception as e:
        print(f"[Live] Hata: {e}")


if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8000, log_level="info")
