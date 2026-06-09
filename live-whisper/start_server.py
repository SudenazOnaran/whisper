"""
Start WhisperLive server with lecture-friendly defaults.
"""
import subprocess
import sys
from pathlib import Path

VENDOR = Path(__file__).resolve().parent / "vendor" / "WhisperLive"
RUN_SERVER = VENDOR / "run_server.py"

if str(VENDOR) not in sys.path:
    sys.path.insert(0, str(VENDOR))


def main() -> None:
    cmd = [
        sys.executable,
        str(RUN_SERVER),
        "--port", "9090",
        "--backend", "faster_whisper",
        "--max_clients", "4",
        "--max_connection_time", "7200",  # 2 hours for long lectures
        "--omp_num_threads", "4",
    ]
    print("Starting WhisperLive server...")
    print(" ".join(cmd))
    subprocess.run(cmd, cwd=str(VENDOR))


if __name__ == "__main__":
    main()
