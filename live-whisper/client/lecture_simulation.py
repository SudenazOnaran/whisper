"""
Phase 3: Lecture simulation via audio file streaming.
Simulates a live lecture by streaming a pre-recorded file to WhisperLive.
"""
import argparse
import sys
import threading
import time
from pathlib import Path

import client._bootstrap  # noqa: F401
from whisper_live.client import TranscriptionClient

from client.metrics_monitor import MetricsMonitor
from client.transcript_store import TranscriptStore

PROJECT_ROOT = Path(__file__).resolve().parent.parent
PARENT_AUDIO = PROJECT_ROOT.parent / "audio"


def main() -> None:
    parser = argparse.ArgumentParser(description="WhisperLive lecture simulation")
    parser.add_argument("--server", default="localhost")
    parser.add_argument("--port", type=int, default=9090)
    parser.add_argument("--input", required=True, help="Audio file to stream")
    parser.add_argument("--lang", default=None, help="tr, en, or omit for auto")
    parser.add_argument("--model", default="small")
    parser.add_argument("--mode", choices=["tr", "en", "mixed", "auto"], default="auto")
    parser.add_argument("--lecture-id", default=None)
    parser.add_argument("--output-dir", default=str(PROJECT_ROOT / "transcripts"))
    parser.add_argument(
        "--max-duration",
        type=int,
        default=0,
        help="Stop after N seconds (0 = full file). Use 1800/3600 for 30/60 min tests.",
    )
    parser.add_argument(
        "--vad",
        action="store_true",
        help="Enable VAD filter (usually keep off for mic/quiet audio).",
    )
    args = parser.parse_args()

    audio_path = Path(args.input)
    if not audio_path.exists():
        print(f"File not found: {audio_path}")
        sys.exit(1)

    lang = args.lang
    if lang is None and args.mode != "auto":
        lang = args.mode if args.mode != "mixed" else None

    store = TranscriptStore(
        output_dir=Path(args.output_dir),
        lecture_id=args.lecture_id or f"sim-{args.mode}-{int(time.time())}",
        language=lang or args.mode,
    )
    metrics = MetricsMonitor()
    start = time.time()

    def callback(_text: str, segments: list) -> None:
        for seg in segments:
            if seg.get("completed") and store.add_segment(seg):
                metrics.on_segment(seg)
                print(f"[{seg['start']}s] {seg['text'].strip()}")

        if args.max_duration and (time.time() - start) >= args.max_duration:
            client.recording = False

    client = TranscriptionClient(
        args.server,
        args.port,
        lang=lang,
        model=args.model,
        use_vad=args.vad,
        transcription_callback=callback,
        log_transcription=True,
        mute_audio_playback=True,
    )

    def poll_resources() -> None:
        while getattr(client, "recording", True):
            metrics.sample_resources()
            time.sleep(15)

    threading.Thread(target=poll_resources, daemon=True).start()

    print(f"Simulating lecture: {audio_path.name}")
    print(f"Mode: {args.mode} | Lang: {lang or 'auto-detect'}")
    if args.max_duration:
        print(f"Max duration: {args.max_duration}s")

    try:
        client(str(audio_path))
    except KeyboardInterrupt:
        print("\nInterrupted.")
    finally:
        store.save()
        results_path = PROJECT_ROOT / "results" / f"{store.lecture_id}_metrics.json"
        metrics.save(results_path)
        summary = metrics.summary()
        print(f"\n--- Simulation complete ---")
        print(f"Transcript: {store.json_path}")
        print(f"Segments:   {len(store.segments)}")
        print(f"Duration:   {summary['duration_s']}s")
        print(f"Avg interval: {summary['avg_segment_interval_s']}s")
        print(f"CPU avg/max: {summary['cpu_avg_percent']}% / {summary['cpu_max_percent']}%")
        print(f"RAM avg/max: {summary['ram_avg_mb']} MB / {summary['ram_max_mb']} MB")


if __name__ == "__main__":
    main()
