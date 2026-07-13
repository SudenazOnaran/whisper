// whisperClient.ts — backend/server.py ile aynı sözleşme
// POST /transcribe, GET /health
import { TranscriptResult } from "../types/note";

export async function checkServer(baseUrl: string): Promise<boolean> {
  try {
    const res = await fetch(`${baseUrl}/health`, { signal: AbortSignal.timeout(5000) });
    return res.ok;
  } catch {
    return false;
  }
}

export async function transcribeFile(
  fileUri: string,
  fileName: string,
  mimeType: string,
  language: string | null,
  modelSize: string,
  baseUrl: string
): Promise<TranscriptResult> {
  const form = new FormData();

  // React Native FormData — uri formatında dosya ekle
  form.append("file", {
    uri: fileUri,
    name: fileName,
    type: mimeType,
  } as unknown as Blob);

  form.append("model_size", modelSize);
  if (language && language !== "auto") {
    form.append("language", language);
  }

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 300_000); // 5 dk timeout

  try {
    const res = await fetch(`${baseUrl}/transcribe`, {
      method: "POST",
      body: form,
      signal: controller.signal,
    });

    const json = await res.json();

    if (!json.success) {
      throw new Error(json.error ?? "Sunucu hatası");
    }

    const segments = (json.segments ?? []).map((s: Record<string, unknown>) => ({
      time: String(s.time ?? "00:00"),
      text: String(s.text ?? ""),
      start: Number(s.start ?? 0),
      end: Number(s.end ?? 0),
    }));

    return {
      fullText: String(json.full_text ?? ""),
      segments,
      language: String(json.language ?? "bilinmiyor"),
      languageProbability: Number(json.language_probability ?? 0),
    };
  } finally {
    clearTimeout(timeout);
  }
}

// MIME türü yardımcısı
export function getMimeType(uri: string): string {
  const ext = uri.split(".").pop()?.toLowerCase() ?? "";
  const map: Record<string, string> = {
    mp4: "video/mp4",
    mkv: "video/x-matroska",
    avi: "video/x-msvideo",
    mov: "video/quicktime",
    webm: "video/webm",
    mp3: "audio/mpeg",
    m4a: "audio/mp4",
    flac: "audio/flac",
    ogg: "audio/ogg",
    wav: "audio/wav",
    caf: "audio/x-caf",
  };
  return map[ext] ?? "audio/wav";
}
