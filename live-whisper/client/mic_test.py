"""Mikrofon cihazlarını listeler ve 5 sn test kaydı alır."""
import struct
import wave
from pathlib import Path

import pyaudio

PROJECT_ROOT = Path(__file__).resolve().parent.parent
OUT = PROJECT_ROOT / "audio" / "mic-test.wav"


def main() -> None:
    p = pyaudio.PyAudio()
    print("=== Mikrofon cihazları ===\n")
    default_in = p.get_default_input_device_info()
    print(f"Varsayılan giriş: [{default_in['index']}] {default_in['name']}\n")

    for i in range(p.get_device_count()):
        info = p.get_device_info_by_index(i)
        if info["maxInputChannels"] > 0:
            mark = " <-- VARSAYILAN" if i == default_in["index"] else ""
            print(f"  [{i}] {info['name']}{mark}")

    rate = 16000
    chunk = 4096
    seconds = 5
    print(f"\n5 saniye kayıt alınıyor... Konuşun!\n")

    stream = p.open(
        format=pyaudio.paInt16,
        channels=1,
        rate=rate,
        input=True,
        frames_per_buffer=chunk,
    )
    frames = []
    for _ in range(int(rate / chunk * seconds)):
        frames.append(stream.read(chunk))
    stream.stop_stream()
    stream.close()
    p.terminate()

    raw = b"".join(frames)
    samples = struct.unpack(f"<{len(raw)//2}h", raw)
    peak = max(abs(s) for s in samples)
    rms = (sum(s * s for s in samples) / len(samples)) ** 0.5

    OUT.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(OUT), "wb") as wf:
        wf.setnchannels(1)
        wf.setsampwidth(2)
        wf.setframerate(rate)
        wf.writeframes(raw)

    print(f"Kayıt: {OUT}")
    print(f"Peak seviye: {peak} / 32767")
    print(f"RMS seviye:  {rms:.1f}")
    if peak < 500:
        print("\nUYARI: Ses çok düşük! Yanlış mikrofon veya sessiz kayıt.")
    elif peak < 3000:
        print("\nUYARI: Ses zayıf. Mikrofona daha yakın konuşun.")
    else:
        print("\nOK: Mikrofon ses alıyor.")


if __name__ == "__main__":
    main()
