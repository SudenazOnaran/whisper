// export.ts — Android AllScreens.kt exportText() ile aynı format
import { SavedNote } from "../types/note";

export function exportText(note: SavedNote): string {
  const dateStr = formatNoteDate(note.createdAt);
  return `${note.title.trim() || "İsimsiz Not"}\n${dateStr}\n\n${note.fullText}`;
}

export function formatNoteDate(timestamp: number): string {
  const d = new Date(timestamp);
  const day = String(d.getDate()).padStart(2, "0");
  const month = String(d.getMonth() + 1).padStart(2, "0");
  const year = d.getFullYear();
  const hours = String(d.getHours()).padStart(2, "0");
  const minutes = String(d.getMinutes()).padStart(2, "0");
  return `${day}.${month}.${year} ${hours}:${minutes}`;
}

export function sourceLabel(sourceType: string): string {
  switch (sourceType) {
    case "live":
      return "Canlı kayıt";
    case "upload":
      return "Dosya";
    default:
      return "Not";
  }
}

export function wordCount(text: string): number {
  return text.trim() ? text.trim().split(/\s+/).length : 0;
}
