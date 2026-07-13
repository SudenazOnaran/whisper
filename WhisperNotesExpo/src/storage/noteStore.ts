// noteStore.ts — Android NoteStore.kt ile aynı mantık
// expo-file-system/legacy: SDK 54'te getInfoAsync/readAsStringAsync legacy API'de
import * as FileSystem from "expo-file-system/legacy";
import { SavedNote } from "../types/note";

const NOTES_FILE = FileSystem.documentDirectory + "notes.json";

async function readRaw(): Promise<SavedNote[]> {
  try {
    const info = await FileSystem.getInfoAsync(NOTES_FILE);
    if (!info.exists) return [];
    const content = await FileSystem.readAsStringAsync(NOTES_FILE);
    const parsed = JSON.parse(content);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

async function writeRaw(notes: SavedNote[]): Promise<void> {
  const sorted = [...notes].sort((a, b) => b.createdAt - a.createdAt);
  await FileSystem.writeAsStringAsync(NOTES_FILE, JSON.stringify(sorted, null, 2));
}

export async function loadNotes(): Promise<SavedNote[]> {
  const notes = await readRaw();
  return notes.sort((a, b) => b.createdAt - a.createdAt);
}

export async function saveNote(note: SavedNote): Promise<SavedNote[]> {
  const existing = await readRaw();
  const filtered = existing.filter((n) => n.id !== note.id);
  const updated = [...filtered, note];
  await writeRaw(updated);
  return updated.sort((a, b) => b.createdAt - a.createdAt);
}

export async function deleteNote(id: string): Promise<SavedNote[]> {
  const existing = await readRaw();
  const updated = existing.filter((n) => n.id !== id);
  await writeRaw(updated);
  return updated.sort((a, b) => b.createdAt - a.createdAt);
}
