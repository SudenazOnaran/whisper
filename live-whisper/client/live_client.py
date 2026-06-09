"""
Phase 2: Live microphone client.
Captures mic audio, streams to WhisperLive, prints and stores transcripts.
"""
import argparse
import threading
import time
from pathlib import Path

import client._bootstrap  # noqa: F401
from whisper_live.client import TranscriptionClient

from client.metrics_monitor import MetricsMonitor
from client.transcript_store import TranscriptStore

PROJECT_ROOT = Path(__file__).resolve().parent.parent


def main() -> None:
    parser = argparse.ArgumentParser(description="WhisperLive microphone test client")
    parser.add_argument("--server", default="localhost")
    parser.add_argument("--port", type=int, default=9090)
    parser.add_argument("--lang", default=None, help="Language code, e.g. tr, en")
    parser.add_argument("--model", default="small")
    parser.add_argument("--lecture-id", default=None)
    parser.add_argument("--output-dir", default=str(PROJECT_ROOT / "transcripts"))
    parser.add_argument(
        "--vad",
        action="store_true",
        help="Enable VAD (sessizlik filtresi). Mikrofonda genelde kapalı tutun.",
    )
    args = parser.parse_args()

    store = TranscriptStore(
        output_dir=Path(args.output_dir),
        lecture_id=args.lecture_id,
        language=args.lang or "auto",
    )
    metrics = MetricsMonitor()

    def callback(text: str, segments: list) -> None:
        for seg in segments:
            seg_text = seg.get("text", "").strip()
            if not seg_text:
                continue
            if seg.get("completed"):
                metrics.on_segment(seg)
                store.add_segment(seg)
                print(f"\n[LIVE] [{seg['start']}s] {seg_text}", flush=True)
            else:
                store.add_segment(seg, require_completed=False)
                print(f"\r[...] {seg_text}", end="", flush=True)

    client = TranscriptionClient(
        args.server,
        args.port,
        lang=args.lang,
        model=args.model,
        use_vad=args.vad,
        no_speech_thresh=0.8,
        same_output_threshold=2,
        transcription_callback=callback,
        log_transcription=False,
        save_output_recording=True,
        output_recording_filename=str(PROJECT_ROOT / "audio" / "live-recording.wav"),
    )

    def poll_resources() -> None:
        while getattr(client, "recording", True):
            m = metrics.sample_resources()
            print(
                f"\n[METRICS] CPU {m['cpu_percent']}% | RAM {m['ram_mb']} MB",
                flush=True,
            )
            time.sleep(30)

    print(f"Connecting to ws://{args.server}:{args.port}")
    print(f"VAD: {'acik' if args.vad else 'kapali'} | Cikti: {store.json_path}")
    print("Mikrofona NET konusun (hoparlorden gelen ses yetmez). Ctrl+C ile durdurun.\n")

    monitor_thread = threading.Thread(target=poll_resources, daemon=True)
    monitor_thread.start()

    try:
        client()
    except KeyboardInterrupt:
        print("\nStopping...")
    finally:
        store.save()
        results_dir = PROJECT_ROOT / "results"
        metrics.save(results_dir / f"{store.lecture_id}_metrics.json")
        print(f"\nTranscript saved: {store.json_path}")
        print(f"Metrics: {metrics.summary()}")


if __name__ == "__main__":
    main()
