// useRecording.ts — expo-audio SDK 54 kayıt hook'u
import { useState, useRef, useCallback } from "react";
import { AudioModule, useAudioRecorder, RecordingPresets } from "expo-audio";

export interface RecordingState {
  isRecording: boolean;
  seconds: number;
  uri: string | null;
  permissionGranted: boolean;
  error: string | null;
}

export function useRecording() {
  const recorder = useAudioRecorder(RecordingPresets.HIGH_QUALITY);
  const [seconds, setSeconds] = useState(0);
  const [uri, setUri] = useState<string | null>(null);
  const [permissionGranted, setPermissionGranted] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const timerRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const requestPermission = useCallback(async (): Promise<boolean> => {
    try {
      const status = await AudioModule.requestRecordingPermissionsAsync();
      const granted = status.granted;
      setPermissionGranted(granted);
      return granted;
    } catch (e) {
      setError("Mikrofon izni alınamadı.");
      return false;
    }
  }, []);

  const startRecording = useCallback(async (): Promise<boolean> => {
    try {
      setError(null);
      setUri(null);
      setSeconds(0);

      const granted = permissionGranted || (await requestPermission());
      if (!granted) return false;

      await recorder.prepareToRecordAsync();
      recorder.record();

      timerRef.current = setInterval(() => {
        setSeconds((s) => s + 1);
      }, 1000);

      return true;
    } catch (e) {
      setError("Kayıt başlatılamadı: " + String(e));
      return false;
    }
  }, [recorder, permissionGranted, requestPermission]);

  const stopRecording = useCallback(async (): Promise<string | null> => {
    try {
      if (timerRef.current) {
        clearInterval(timerRef.current);
        timerRef.current = null;
      }

      await recorder.stop();
      const recordedUri = recorder.uri ?? null;
      setUri(recordedUri);
      return recordedUri;
    } catch (e) {
      setError("Kayıt durdurulamadı: " + String(e));
      return null;
    }
  }, [recorder]);

  return {
    isRecording: recorder.isRecording ?? false,
    seconds,
    uri,
    permissionGranted,
    error,
    requestPermission,
    startRecording,
    stopRecording,
  };
}
