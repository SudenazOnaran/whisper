import sys
from pathlib import Path

VENDOR = Path(__file__).resolve().parent.parent / "vendor" / "WhisperLive"
if str(VENDOR) not in sys.path:
    sys.path.insert(0, str(VENDOR))
