// record.tsx — Canlı kayıt: her 15 saniyede chunk gönderir, metin aşamalı birikir
import { useEffect, useRef } from "react";
import { useState } from "react";
import {
  View, Text, TouchableOpacity, TextInput, ScrollView,
  Animated, ActivityIndicator,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { router } from "expo-router";
import { useChunkedRecording, CHUNK_SECONDS } from "../src/hooks/useChunkedRecording";
import { useApp } from "../src/context/AppContext";
import * as NoteStore from "../src/storage/noteStore";

function formatTime(s: number) {
  const m = Math.floor(s / 60);
  return `${String(m).padStart(2, "0")}:${String(s % 60).padStart(2, "0")}`;
}

export default function RecordScreen() {
  const { settings, refreshNotes, setLiveResult } = useApp();
  const langArg = settings.selectedLanguage === "auto" ? null : settings.selectedLanguage;

  const {
    isRecording, seconds, permissionGranted, lines, processing, error,
    requestPermission, startRecording, stopRecording,
  } = useChunkedRecording(settings.serverUrl, settings.selectedModel, langArg);

  const [title, setTitle] = useState("");
  const scrollRef = useRef<ScrollView>(null);
  const pulseAnim = useRef(new Animated.Value(1)).current;

  useEffect(() => { requestPermission(); }, [requestPermission]);

  // Nabız animasyonu
  useEffect(() => {
    let loop: Animated.CompositeAnimation;
    if (isRecording) {
      loop = Animated.loop(
        Animated.sequence([
          Animated.timing(pulseAnim, { toValue: 1.15, duration: 700, useNativeDriver: true }),
          Animated.timing(pulseAnim, { toValue: 1, duration: 700, useNativeDriver: true }),
        ])
      );
      loop.start();
    } else {
      pulseAnim.setValue(1);
    }
    return () => loop?.stop();
  }, [isRecording, pulseAnim]);

  // Yeni satır gelince aşağı kaydır
  useEffect(() => {
    if (lines.length > 0) {
      setTimeout(() => scrollRef.current?.scrollToEnd({ animated: true }), 100);
    }
  }, [lines]);

  const handleStop = async () => {
    const finalLines = await stopRecording();
    const allLines = finalLines.length > 0 ? finalLines : lines;

    if (allLines.length === 0) {
      router.back();
      return;
    }

    const fullText = allLines.map((l) => l.text).join(" ");
    const noteTitle = title.trim() || `Canlı Kayıt ${new Date().toLocaleTimeString("tr-TR")}`;

    const note = {
      id: `note_${Date.now()}`,
      title: noteTitle,
      fullText,
      segments: allLines.map((l, i) => ({
        time: l.timestamp,
        text: l.text,
        start: i * 15,
        end: (i + 1) * 15,
      })),
      language: settings.selectedLanguage === "auto" ? "auto" : settings.selectedLanguage,
      languageProbability: 1,
      sourceType: "live" as const,
      createdAt: Date.now(),
    };

    await NoteStore.saveNote(note);
    await refreshNotes();
    setLiveResult(noteTitle, fullText);
    router.replace("/result" as never);
  };

  const fullText = lines.map((l) => l.text).join(" ");

  return (
    <SafeAreaView className="flex-1 bg-dark-bg">
      {/* Üst çubuk */}
      <View className="flex-row items-center px-2 py-3" style={{ backgroundColor: "#1C1B1F" }}>
        <TouchableOpacity
          onPress={() => { if (!isRecording) router.back(); }}
          className="p-2 mr-2"
        >
          <Text className="text-xl" style={{ color: isRecording ? "#424242" : "#D0BCFF" }}>←</Text>
        </TouchableOpacity>
        <Text className="text-lg font-bold text-white">Canlı Kayıt</Text>
        {processing && (
          <View className="flex-row items-center ml-3">
            <ActivityIndicator size="small" color="#D0BCFF" />
            <Text className="text-xs ml-2" style={{ color: "#D0BCFF" }}>Not yazılıyor...</Text>
          </View>
        )}
      </View>

      {/* İzin yok */}
      {!permissionGranted && (
        <View className="mx-4 mt-4 rounded-2xl p-5 items-center" style={{ backgroundColor: "#1C1B1F" }}>
          <Text className="text-4xl mb-3">🎙️</Text>
          <Text className="text-base font-bold text-white mb-2">Mikrofon İzni Gerekli</Text>
          <TouchableOpacity onPress={requestPermission} className="rounded-full px-6 py-3 mt-2" style={{ backgroundColor: "#D0BCFF" }}>
            <Text className="font-bold" style={{ color: "#121212" }}>İzin Ver</Text>
          </TouchableOpacity>
        </View>
      )}

      {/* Kayıt başlığı */}
      <TextInput
        value={title}
        onChangeText={setTitle}
        placeholder="Kayıt adı (isteğe bağlı)"
        placeholderTextColor="#9E9E9E"
        editable={!isRecording}
        className="mx-4 mt-4 rounded-xl px-4 py-3 text-sm text-white border"
        style={{ backgroundColor: "#1C1B1F", borderColor: "#424242" }}
      />

      {/* Zamanlayıcı + nabız */}
      <View className="items-center py-4">
        <Animated.View
          style={{
            width: 100, height: 100, borderRadius: 50,
            backgroundColor: "#1C1B1F",
            borderWidth: 3,
            borderColor: isRecording ? "#F28B82" : "#424242",
            alignItems: "center", justifyContent: "center",
            transform: [{ scale: pulseAnim }],
          }}
        >
          <Text className="text-xs font-bold tracking-widest"
            style={{ color: isRecording ? "#F28B82" : "#9E9E9E" }}>
            {isRecording ? "KAYIT" : "HAZIR"}
          </Text>
          <Text className="text-2xl font-bold text-white">{formatTime(seconds)}</Text>
        </Animated.View>

        {isRecording && (
          <Text className="text-xs mt-2" style={{ color: processing ? "#D0BCFF" : "#9E9E9E" }}>
            {processing
              ? "⚡ Yazıya dönüştürülüyor..."
              : `Her ${CHUNK_SECONDS} sn'de gönderiliyor • tiny model`}
          </Text>
        )}
      </View>

      {/* Aşamalı not alanı */}
      <ScrollView
        ref={scrollRef}
        className="flex-1 mx-4 mb-2"
        showsVerticalScrollIndicator={false}
      >
        {lines.length === 0 && !isRecording && (
          <View className="items-center py-8 px-4">
            <Text className="text-gray-500 text-sm text-center mb-3">
              Kaydı başlat — her {CHUNK_SECONDS} sn'de not otomatik yazılır
            </Text>
            <View className="rounded-xl p-3 w-full" style={{ backgroundColor: "#1C1B1F" }}>
              <Text className="text-xs text-center" style={{ color: "#9E9E9E" }}>
                ⚡ Her {CHUNK_SECONDS} sn'de otomatik gönderilir{"\n"}
                tiny model → ~2-4 sn işlem süresi
              </Text>
            </View>
          </View>
        )}

        {lines.map((line, i) => (
          <View key={i} className="rounded-xl p-3 mb-2 flex-row" style={{ backgroundColor: "#1C1B1F" }}>
            <View className="rounded px-2 py-1 mr-3 self-start" style={{ backgroundColor: "#381E72" }}>
              <Text className="text-xs font-bold" style={{ color: "#D0BCFF" }}>{line.timestamp}</Text>
            </View>
            <Text className="flex-1 text-sm leading-6" style={{ color: "#E3E3E3" }}>{line.text}</Text>
          </View>
        ))}

        {processing && (
          <View className="rounded-xl p-3 mb-2 flex-row items-center" style={{ backgroundColor: "#1C1B1F" }}>
            <ActivityIndicator size="small" color="#D0BCFF" style={{ marginRight: 10 }} />
            <Text className="text-sm" style={{ color: "#9E9E9E" }}>Yazıya dönüştürülüyor...</Text>
          </View>
        )}
      </ScrollView>

      {/* Hata */}
      {error && (
        <Text className="text-xs text-center px-4 mb-2" style={{ color: "#F28B82" }}>{error}</Text>
      )}

      {/* Buton */}
      <View className="px-4 pb-4">
        {!isRecording ? (
          <TouchableOpacity
            onPress={() => startRecording()}
            disabled={!permissionGranted}
            className="rounded-full py-4 items-center"
            style={{ backgroundColor: permissionGranted ? "#D0BCFF" : "#424242" }}
            activeOpacity={0.8}
          >
            <Text className="text-base font-bold" style={{ color: permissionGranted ? "#121212" : "#9E9E9E" }}>
              🎙️  Kaydı Başlat
            </Text>
          </TouchableOpacity>
        ) : (
          <TouchableOpacity
            onPress={handleStop}
            className="rounded-full py-4 items-center"
            style={{ backgroundColor: "#F28B82" }}
            activeOpacity={0.8}
          >
            <Text className="text-base font-bold text-white">
              ⏹  Durdur ve Notu Kaydet
            </Text>
          </TouchableOpacity>
        )}
      </View>
    </SafeAreaView>
  );
}
