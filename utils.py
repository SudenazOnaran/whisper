import json
import subprocess
import wave
from pathlib import Path

from faster_whisper import WhisperModel
from tqdm import tqdm

VIDEO_EXTENSIONS = {".mp4", ".mkv", ".avi", ".mov", ".webm"}
AUDIO_EXTENSIONS = {".mp3", ".wav", ".m4a", ".flac", ".ogg"}


def prepare_audio(input_path: Path, audio_dir: Path) -> Path:
    input_path = Path(input_path).resolve()
    suffix = input_path.suffix.lower()

    if suffix in VIDEO_EXTENSIONS:
        output_path = audio_dir / "temp.wav"
        extract_audio(input_path, output_path)
        return output_path

    if suffix in AUDIO_EXTENSIONS:
        return input_path

    raise ValueError(f"Unsupported file format: {suffix}")


def extract_audio(video_path: Path, output_path: Path) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    cmd = [
        "ffmpeg",
        "-y",
        "-i",
        str(video_path),
        "-vn",
        "-acodec",
        "pcm_s16le",
        "-ar",
        "16000",
        "-ac",
        "1",
        str(output_path),
    ]
    try:
        result = subprocess.run(cmd, capture_output=True, text=True)
    except FileNotFoundError as exc:
        raise RuntimeError(
            "FFmpeg not found. Install FFmpeg and add it to PATH for video input."
        ) from exc
    if result.returncode != 0:
        raise RuntimeError(f"FFmpeg failed:\n{result.stderr}")
    print(f"Extracted audio to: {output_path}")


def _duration_via_ffprobe(audio_path: Path) -> float | None:
    cmd = [
        "ffprobe",
        "-v",
        "error",
        "-show_entries",
        "format=duration",
        "-of",
        "default=noprint_wrappers=1:nokey=1",
        str(audio_path),
    ]
    try:
        result = subprocess.run(cmd, capture_output=True, text=True)
    except FileNotFoundError:
        return None
    if result.returncode == 0 and result.stdout.strip():
        return float(result.stdout.strip())
    return None


def _duration_via_pyav(audio_path: Path) -> float | None:
    try:
        import av

        with av.open(str(audio_path)) as container:
            if container.duration:
                return float(container.duration) / float(av.time_base)
            stream = container.streams.audio[0]
            if stream.duration is not None:
                return float(stream.duration * stream.time_base)
    except Exception:
        return None
    return None


def get_audio_duration(audio_path: Path) -> float:
    duration = _duration_via_ffprobe(audio_path)
    if duration is not None:
        return duration

    duration = _duration_via_pyav(audio_path)
    if duration is not None:
        return duration

    if audio_path.suffix.lower() == ".wav":
        with wave.open(str(audio_path), "rb") as wf:
            return wf.getnframes() / float(wf.getframerate())

    raise RuntimeError("Could not determine audio duration")


def run_transcription(
    audio_path: Path,
    device: str = "cpu",
    model_size: str = "small",
    compute_type: str = "int8",
    language: str | None = None,
) -> dict:
    print(f"Loading model '{model_size}' on {device} ({compute_type})...")
    model = WhisperModel(model_size, device=device, compute_type=compute_type)

    transcribe_kwargs: dict = {
        "beam_size": 5,
        "word_timestamps": True,
    }
    if language:
        transcribe_kwargs["language"] = language
        print(f"Language forced: {language}")
    else:
        print("Language: auto-detect")

    print("Transcribing...")
    segments_gen, info = model.transcribe(str(audio_path), **transcribe_kwargs)

    segments = []
    for segment in tqdm(segments_gen, desc="Transcribing", unit="seg"):
        segments.append(
            {
                "start": round(segment.start, 2),
                "end": round(segment.end, 2),
                "text": segment.text.strip(),
            }
        )

    return {
        "language": info.language,
        "language_probability": round(info.language_probability, 4),
        "segments": segments,
    }


def save_transcripts(result: dict, output_dir: Path, output_name: str = "output") -> None:
    output_dir.mkdir(parents=True, exist_ok=True)

    json_path = output_dir / f"{output_name}.json"
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(
            {"language": result["language"], "segments": result["segments"]},
            f,
            ensure_ascii=False,
            indent=2,
        )

    txt_path = output_dir / f"{output_name}.txt"
    text = "\n".join(seg["text"] for seg in result["segments"])
    txt_path.write_text(text, encoding="utf-8")

    print(f"Saved: {json_path}")
    print(f"Saved: {txt_path}")
