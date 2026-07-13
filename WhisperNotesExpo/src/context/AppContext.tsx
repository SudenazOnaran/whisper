// AppContext.tsx — Global uygulama state yönetimi
import React, { createContext, useContext, useState, useCallback, useEffect } from "react";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { SavedNote, TranscriptResult, AppSettings, Language, ModelSize } from "../types/note";
import * as NoteStore from "../storage/noteStore";

const DEFAULT_SERVER_URL =
  process.env.EXPO_PUBLIC_SERVER_URL ?? "http://127.0.0.1:8000";

// ── İşlem adımları ────────────────────────────────────────────────
export interface ProcessingStep {
  label: string;
  done: boolean;
  active: boolean;
}

function buildSteps(activeIndex: number): ProcessingStep[] {
  const labels = [
    "Dosya hazırlanıyor",
    "Sunucuya yükleniyor",
    "Whisper ile transkript alınıyor",
    "Notlar oluşturuldu",
  ];
  return labels.map((label, i) => ({
    label,
    done: i < activeIndex,
    active: i === activeIndex,
  }));
}

// ── Context tipi ──────────────────────────────────────────────────
interface AppContextValue {
  // Ayarlar
  settings: AppSettings;
  updateServerUrl: (url: string) => Promise<void>;
  updateLanguage: (lang: Language) => void;
  updateModel: (model: ModelSize) => void;

  // İşlem durumu
  isProcessing: boolean;
  processingSteps: ProcessingStep[];
  processingTitle: string;
  error: string | null;
  clearError: () => void;

  // Sonuç
  result: TranscriptResult | null;
  resultTitle: string;

  // Notlar
  savedNotes: SavedNote[];
  refreshNotes: () => Promise<void>;
  deleteNote: (id: string) => Promise<void>;

  // İşlemler
  processFile: (
    fileUri: string,
    fileName: string,
    mimeType: string,
    title: string,
    sourceType: "upload" | "live"
  ) => Promise<boolean>;
  openNote: (note: SavedNote) => void;
  // Canlı kayıt: chunk biriktirme sonucu direkt set et
  setLiveResult: (title: string, fullText: string) => void;
}

const AppContext = createContext<AppContextValue | null>(null);

export function AppProvider({ children }: { children: React.ReactNode }) {
  const [settings, setSettings] = useState<AppSettings>({
    serverUrl: DEFAULT_SERVER_URL,
    selectedLanguage: "auto",
    selectedModel: "small",
  });
  const [isProcessing, setIsProcessing] = useState(false);
  const [processingSteps, setProcessingSteps] = useState<ProcessingStep[]>(buildSteps(0));
  const [processingTitle, setProcessingTitle] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<TranscriptResult | null>(null);
  const [resultTitle, setResultTitle] = useState("");
  const [savedNotes, setSavedNotes] = useState<SavedNote[]>([]);

  // Ayarları AsyncStorage'dan yükle
  useEffect(() => {
    const load = async () => {
      try {
        const url = await AsyncStorage.getItem("serverUrl");
        const lang = await AsyncStorage.getItem("language");
        const model = await AsyncStorage.getItem("model");
        setSettings((prev) => ({
          serverUrl: url ?? prev.serverUrl,
          selectedLanguage: (lang as Language) ?? prev.selectedLanguage,
          selectedModel: (model as ModelSize) ?? prev.selectedModel,
        }));
      } catch {}

      const notes = await NoteStore.loadNotes();
      setSavedNotes(notes);
    };
    load();
  }, []);

  const updateServerUrl = useCallback(async (url: string) => {
    const cleaned = url.trimEnd().replace(/\/$/, "");
    setSettings((prev) => ({ ...prev, serverUrl: cleaned }));
    await AsyncStorage.setItem("serverUrl", cleaned);
  }, []);

  const updateLanguage = useCallback((lang: Language) => {
    setSettings((prev) => ({ ...prev, selectedLanguage: lang }));
    AsyncStorage.setItem("language", lang);
  }, []);

  const updateModel = useCallback((model: ModelSize) => {
    setSettings((prev) => ({ ...prev, selectedModel: model }));
    AsyncStorage.setItem("model", model);
  }, []);

  const clearError = useCallback(() => setError(null), []);

  const refreshNotes = useCallback(async () => {
    const notes = await NoteStore.loadNotes();
    setSavedNotes(notes);
  }, []);

  const deleteNote = useCallback(async (id: string) => {
    const updated = await NoteStore.deleteNote(id);
    setSavedNotes(updated);
  }, []);

  const openNote = useCallback((note: SavedNote) => {
    setResultTitle(note.title);
    setResult(note.toTranscriptResult?.() ?? {
      fullText: note.fullText,
      segments: note.segments,
      language: note.language,
      languageProbability: note.languageProbability,
    });
  }, []);

  const setLiveResult = useCallback((title: string, fullText: string) => {
    setResultTitle(title);
    setResult({
      fullText,
      segments: [],
      language: "tr",
      languageProbability: 1,
    });
  }, []);

  // Asıl transkripsiyon akışı (hem upload hem live için)
  const processFile = useCallback(
    async (
      fileUri: string,
      fileName: string,
      mimeType: string,
      title: string,
      sourceType: "upload" | "live"
    ): Promise<boolean> => {
      setError(null);
      setIsProcessing(true);
      setProcessingTitle(title || "Ses dosyası");
      setProcessingSteps(buildSteps(0));

      try {
        // lazy import - döngüsel bağımlılığı önlemek için
        const { transcribeFile } = await import("../api/whisperClient");

        setProcessingSteps(buildSteps(1));
        const langArg =
          settings.selectedLanguage === "auto" ? null : settings.selectedLanguage;

        setProcessingSteps(buildSteps(2));
        const transcriptResult = await transcribeFile(
          fileUri,
          fileName,
          mimeType,
          langArg,
          settings.selectedModel,
          settings.serverUrl
        );

        setProcessingSteps(buildSteps(3));

        const text = transcriptResult.fullText.trim();
        if (text) {
          const note: SavedNote = {
            id: `note_${Date.now()}`,
            title: title.trim() || "İsimsiz Not",
            fullText: text,
            segments: transcriptResult.segments,
            language: transcriptResult.language,
            languageProbability: transcriptResult.languageProbability,
            sourceType,
            createdAt: Date.now(),
          };
          const updated = await NoteStore.saveNote(note);
          setSavedNotes(updated);
        }

        setResult(transcriptResult);
        setResultTitle(title || "İsimsiz Not");
        setIsProcessing(false);
        return true;
      } catch (e) {
        const msg = e instanceof Error ? e.message : String(e);
        setError(
          `Hata: ${msg}\n\nSunucu adresini kontrol edin: ${settings.serverUrl}`
        );
        setIsProcessing(false);
        return false;
      }
    },
    [settings]
  );

  return (
    <AppContext.Provider
      value={{
        settings,
        updateServerUrl,
        updateLanguage,
        updateModel,
        isProcessing,
        processingSteps,
        processingTitle,
        error,
        clearError,
        result,
        resultTitle,
        savedNotes,
        refreshNotes,
        deleteNote,
        processFile,
        openNote,
        setLiveResult,
      }}
    >
      {children}
    </AppContext.Provider>
  );
}

export function useApp(): AppContextValue {
  const ctx = useContext(AppContext);
  if (!ctx) throw new Error("useApp must be used inside AppProvider");
  return ctx;
}
