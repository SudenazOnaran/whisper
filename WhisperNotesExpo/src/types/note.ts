// Not modeli — Android NoteStore.kt ile aynı şema
export interface Segment {
  time: string;   // "MM:SS"
  text: string;
  start: number;
  end: number;
}

export interface TranscriptResult {
  fullText: string;
  segments: Segment[];
  language: string;
  languageProbability: number;
}

export interface SavedNote {
  id: string;           // "note_{timestamp}"
  title: string;
  fullText: string;
  segments: Segment[];
  language: string;
  languageProbability: number;
  sourceType: "upload" | "live" | "unknown";
  createdAt: number;    // epoch ms
}

export type ModelSize = "small" | "medium";
export type Language = "auto" | "tr" | "en";

export interface AppSettings {
  serverUrl: string;
  selectedLanguage: Language;
  selectedModel: ModelSize;
}
