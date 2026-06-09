import argparse
import time
from pathlib import Path

from utils import get_audio_duration, prepare_audio, run_transcription, save_transcripts


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Transcribe audio/video files with faster-whisper"
    )
    parser.add_argument("--input", required=True, help="Path to input audio or video file")
    parser.add_argument(
        "--device",
        default="cpu",
        choices=["cpu", "cuda"],
        help="Inference device (default: cpu)",
    )
    parser.add_argument("--model", default="small", help="Whisper model size (default: small)")
    parser.add_argument(
        "--compute-type",
        default="int8",
        help="Compute type, e.g. int8, float16 (default: int8)",
    )
    parser.add_argument(
        "--language",
        default=None,
        help="Force language code, e.g. tr, en (default: auto-detect)",
    )
    parser.add_argument(
        "--output-name",
        default="output",
        help="Output file base name without extension (default: output)",
    )
    args = parser.parse_args()

    input_path = Path(args.input)
    if not input_path.exists():
        raise FileNotFoundError(f"Input file not found: {input_path}")

    project_root = Path(__file__).parent
    audio_dir = project_root / "audio"
    transcripts_dir = project_root / "transcripts"
    audio_dir.mkdir(exist_ok=True)
    transcripts_dir.mkdir(exist_ok=True)

    print(f"Input: {input_path}")
    audio_path = prepare_audio(input_path, audio_dir)

    audio_duration = get_audio_duration(audio_path)
    print(f"Audio duration: {audio_duration:.2f}s ({audio_duration / 60:.2f} min)")

    start = time.perf_counter()
    result = run_transcription(
        audio_path,
        device=args.device,
        model_size=args.model,
        compute_type=args.compute_type,
        language=args.language,
    )
    processing_time = time.perf_counter() - start

    save_transcripts(result, transcripts_dir, output_name=args.output_name)

    rtf = processing_time / audio_duration if audio_duration > 0 else 0.0

    print("\n--- Performance ---")
    print(f"Detected language: {result['language']} (prob: {result['language_probability']})")
    print(f"Audio duration:    {audio_duration:.2f}s")
    print(f"Processing time:   {processing_time:.2f}s")
    print(f"Real-time factor:  {rtf:.4f}  (lower is faster)")


if __name__ == "__main__":
    main()
