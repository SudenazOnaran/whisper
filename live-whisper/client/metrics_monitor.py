import json
import time
from dataclasses import dataclass, field
from pathlib import Path

import psutil


@dataclass
class MetricsMonitor:
    """Track CPU, RAM, latency and stability during a live session."""

    samples: list[dict] = field(default_factory=list)
    latencies: list[float] = field(default_factory=list)
    errors: list[str] = field(default_factory=list)
    _last_segment_time: float | None = None
    _start_time: float = field(default_factory=time.time)

    def on_segment(self, seg: dict) -> None:
        now = time.time()
        if self._last_segment_time is not None:
            self.latencies.append(now - self._last_segment_time)
        self._last_segment_time = now

    def sample_resources(self) -> dict:
        proc = psutil.Process()
        data = {
            "elapsed_s": round(time.time() - self._start_time, 2),
            "cpu_percent": psutil.cpu_percent(interval=0.1),
            "ram_mb": round(proc.memory_info().rss / 1024 / 1024, 1),
            "system_ram_percent": psutil.virtual_memory().percent,
        }
        self.samples.append(data)
        return data

    def record_error(self, message: str) -> None:
        self.errors.append(message)

    def summary(self) -> dict:
        avg_latency = (
            round(sum(self.latencies) / len(self.latencies), 3)
            if self.latencies
            else None
        )
        cpu_vals = [s["cpu_percent"] for s in self.samples]
        ram_vals = [s["ram_mb"] for s in self.samples]
        return {
            "duration_s": round(time.time() - self._start_time, 2),
            "segment_count": len(self.latencies) + (1 if self._last_segment_time else 0),
            "avg_segment_interval_s": avg_latency,
            "cpu_avg_percent": round(sum(cpu_vals) / len(cpu_vals), 1) if cpu_vals else None,
            "cpu_max_percent": max(cpu_vals) if cpu_vals else None,
            "ram_avg_mb": round(sum(ram_vals) / len(ram_vals), 1) if ram_vals else None,
            "ram_max_mb": max(ram_vals) if ram_vals else None,
            "error_count": len(self.errors),
            "errors": self.errors,
        }

    def save(self, path: Path) -> None:
        path.parent.mkdir(parents=True, exist_ok=True)
        with open(path, "w", encoding="utf-8") as f:
            json.dump(
                {"summary": self.summary(), "samples": self.samples},
                f,
                indent=2,
            )
