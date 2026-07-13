// useChunkedRecording.ts — Düzeltilmiş versiyon
// Stale closure hatası giderildi, ref tabanlı state, 8 saniyelik chunk
import { useState, useRef, useCallback, useEffect } from "react";
import { AudioModule, useAudioRecorder, RecordingPresets } from "expo-audio";
import { transcribeFile, getMimeType } from "../api/whisperClient";

export const CHUNK_SECONDS = 5; // 5 sn = ~3-6 sn işlem, tiny modelle neredeyse anlık

export interface ChunkLine {
  timestamp: string;
  text: string;
}

// Canlı kayıtta tiny model kullan — hız öncelikli
const LIVE_MODEL = "tiny";

export function useChunkedRecording(
  serverUrl: string,
  _modelSize: string,  // live'da kullanılmaz, tiny sabit
  language: string | null
) {
  const modelSize = LIVE_MODEL;
  const recorder = useAudioRecorder(RecordingPresets.HIGH_QUALITY);

  const [isRecording, setIsRecording] = useState(false);
  const [seconds, setSeconds] = useState(0);
  const [permissionGranted, setPermissionGranted] = useState(false);
  const [lines, setLines] = useState<ChunkLine[]>([]);
  const [processing, setProcessing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  // modelSize artık LIVE_MODEL sabitinden geliyor, dışarıdan gelmez

  // Refs — async callback'lerde stale closure olmaz
  const activeRef = useRef(false);
  const secondsRef = useRef(0);
  const linesRef = useRef<ChunkLine[]>([]);
  const busyRef = useRef(false); // aynı anda iki chunk gönderilmesini engelle
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const chunkTimerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  // Params ref — interval callback'in her zaman güncel değerleri görmesi için
  const paramsRef = useRef({ serverUrl, modelSize, language });
  useEffect(() => {
    paramsRef.current = { serverUrl, modelSize, language };
  }, [serverUrl, modelSize, language]);

  const requestPermission = useCallback(async (): Promise<boolean> => {
    try {
      const status = await AudioModule.requestRecordingPermissionsAsync();
      setPermissionGranted(status.granted);
      return status.granted;
    } catch {
      setError("Mikrofon izni alınamadı.");
      return false;
    }
  }, []);

  // Kaydı durdur → yükle → yeniden başlat
  const flushChunk = useCallback(async (isFinal = false) => {
    if (!activeRef.current && !isFinal) return;
    if (busyRef.current) return; // önceki chunk hala işleniyorsa atla
    busyRef.current = true;

    try {
      await recorder.stop();
      const uri = recorder.uri ?? null;

      if (uri) {
        setProcessing(true);
        const { serverUrl, modelSize, language } = paramsRef.current;
        const fileName = uri.split("/").pop() ?? "chunk.m4a";
        const mimeType = getMimeType(fileName);

        const result = await transcribeFile(
          uri, fileName, mimeType, language, modelSize, serverUrl
        );
        const text = result.fullText.trim();

        if (text) {
          const s = secondsRef.current;
          const ts = `${String(Math.floor(s / 60)).padStart(2, "0")}:${String(s % 60).padStart(2, "0")}`;
          const newLine: ChunkLine = { timestamp: ts, text };
          linesRef.current = [...linesRef.current, newLine];
          setLines([...linesRef.current]);
        }
        setProcessing(false);
      }
    } catch (e) {
      setProcessing(false);
      if (activeRef.current) {
        // Hata olsa bile devam et
        console.warn("[Chunk] hata:", e);
      }
    } finally {
      busyRef.current = false;
    }

    // Kayıt hala aktifse yeniden başlat
    if (activeRef.current) {
      try {
        await recorder.prepareToRecordAsync();
        recorder.record();
      } catch (e) {
        setError("Kayıt yeniden başlatılamadı.");
        activeRef.current = false;
        setIsRecording(false);
      }
    }
  }, [recorder]);

  const startRecording = useCallback(async (): Promise<boolean> => {
    setError(null);
    setLines([]);
    linesRef.current = [];
    secondsRef.current = 0;
    setSeconds(0);
    busyRef.current = false;

    const granted = permissionGranted || (await requestPermission());
    if (!granted) return false;

    try {
      await recorder.prepareToRecordAsync();
      recorder.record();
      activeRef.current = true;
      setIsRecording(true);

      // Saniye sayacı
      timerRef.current = setInterval(() => {
        secondsRef.current += 1;
        setSeconds((s) => s + 1);
      }, 1000);

      // Chunk zamanlayıcısı — her CHUNK_SECONDS saniyede bir gönder
      chunkTimerRef.current = setInterval(() => {
        flushChunk(false);
      }, CHUNK_SECONDS * 1000);

      return true;
    } catch (e) {
      setError("Kayıt başlatılamadı: " + String(e));
      return false;
    }
  }, [recorder, permissionGranted, requestPermission, flushChunk]);

  const stopRecording = useCallback(async (): Promise<ChunkLine[]> => {
    activeRef.current = false;
    setIsRecording(false);

    if (timerRef.current) { clearInterval(timerRef.current); timerRef.current = null; }
    if (chunkTimerRef.current) { clearInterval(chunkTimerRef.current); chunkTimerRef.current = null; }

    // Son parçayı işle
    if (!busyRef.current) {
      await flushChunk(true);
    } else {
      // Önceki chunk bitene kadar bekle
      await new Promise<void>((resolve) => {
        const poll = setInterval(() => {
          if (!busyRef.current) { clearInterval(poll); resolve(); }
        }, 200);
      });
    }

    return linesRef.current;
  }, [flushChunk]);

  useEffect(() => {
    return () => {
      activeRef.current = false;
      if (timerRef.current) clearInterval(timerRef.current);
      if (chunkTimerRef.current) clearInterval(chunkTimerRef.current);
    };
  }, []);

  return {
    isRecording,
    seconds,
    permissionGranted,
    lines,
    processing,
    error,
    requestPermission,
    startRecording,
    stopRecording,
  };
}
