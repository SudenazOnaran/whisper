import json
import uuid
from datetime import datetime, timezone
from pathlib import Path


class TranscriptStore:
    """Persist completed transcript segments to JSON (text only, no audio)."""

    def __init__(
        self,
        output_dir: Path,
        lecture_id: str | None = None,
        language: str = "auto",
    ):
        self.lecture_id = lecture_id or str(uuid.uuid4())
        self.language = language
        self.segments: list[dict] = []
        self._seen: set[tuple] = set()
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)
        self.json_path = self.output_dir / f"{self.lecture_id}.json"

    def _segment_key(self, seg: dict) -> tuple:
        return (seg.get("start"), seg.get("end"), seg.get("text", "").strip())

    def add_segment(self, seg: dict, require_completed: bool = True) -> bool:
        if require_completed and not seg.get("completed", False):
            return False
        if not seg.get("text", "").strip():
            return False

        key = self._segment_key(seg)
        if key in self._seen:
            return False

        self._seen.add(key)
        self.segments.append(
            {
                "timestamp": datetime.now(timezone.utc).isoformat(),
                "start": float(seg.get("start", 0)),
                "end": float(seg.get("end", 0)),
                "text": seg.get("text", "").strip(),
            }
        )
        self.save()
        return True

    def save(self) -> None:
        payload = {
            "lecture_id": self.lecture_id,
            "language": self.language,
            "segments": self.segments,
        }
        with open(self.json_path, "w", encoding="utf-8") as f:
            json.dump(payload, f, ensure_ascii=False, indent=2)

    def make_callback(self, on_print: bool = True):
        def callback(_text: str, segments: list) -> None:
            for seg in segments:
                if self.add_segment(seg) and on_print:
                    print(f"[{seg['start']}s] {seg['text'].strip()}")

        return callback
