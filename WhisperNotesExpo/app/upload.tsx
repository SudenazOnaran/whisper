// upload.tsx — Dosya yükleme ekranı
import { useState } from "react";
import {
  View, Text, TouchableOpacity, TextInput, ScrollView, Alert,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { router } from "expo-router";
import * as DocumentPicker from "expo-document-picker";
import { useApp } from "../src/context/AppContext";
import { getMimeType } from "../src/api/whisperClient";
import type { Language, ModelSize } from "../src/types/note";

function TopBar({ title, onBack }: { title: string; onBack: () => void }) {
  return (
    <View className="flex-row items-center px-2 py-3" style={{ backgroundColor: "#1C1B1F" }}>
      <TouchableOpacity onPress={onBack} className="p-2 mr-2">
        <Text className="text-xl" style={{ color: "#D0BCFF" }}>←</Text>
      </TouchableOpacity>
      <Text className="text-lg font-bold text-white">{title}</Text>
    </View>
  );
}

function SelectButton({
  label, selected, onPress,
}: { label: string; selected: boolean; onPress: () => void }) {
  return (
    <TouchableOpacity
      onPress={onPress}
      className="flex-1 py-2.5 items-center rounded-lg mr-2"
      style={{ backgroundColor: selected ? "#D0BCFF" : "#2B2930" }}
    >
      <Text
        className="text-xs font-semibold"
        style={{ color: selected ? "#121212" : "#E3E3E3" }}
        numberOfLines={1}
      >
        {label}
      </Text>
    </TouchableOpacity>
  );
}

export default function UploadScreen() {
  const { settings, updateLanguage, updateModel, processFile } = useApp();
  const [pickedUri, setPickedUri] = useState<string | null>(null);
  const [pickedName, setPickedName] = useState("");
  const [pickedMime, setPickedMime] = useState("audio/wav");
  const [title, setTitle] = useState("");

  const pickFile = async () => {
    try {
      const result = await DocumentPicker.getDocumentAsync({
        type: ["audio/*", "video/*"],
        copyToCacheDirectory: true,
      });
      if (!result.canceled && result.assets[0]) {
        const asset = result.assets[0];
        setPickedUri(asset.uri);
        setPickedName(asset.name);
        setPickedMime(asset.mimeType ?? getMimeType(asset.name));
        if (!title) setTitle(asset.name.replace(/\.[^.]+$/, ""));
      }
    } catch {
      Alert.alert("Hata", "Dosya seçilemedi.");
    }
  };

  const handleSubmit = async () => {
    if (!pickedUri) return;
    router.push("/processing");
    const ok = await processFile(pickedUri, pickedName, pickedMime, title || pickedName, "upload");
    if (ok) {
      router.replace("/result");
    } else {
      router.back();
    }
  };

  return (
    <SafeAreaView className="flex-1 bg-dark-bg">
      <TopBar title="Dosya Yükle" onBack={() => router.back()} />

      <ScrollView className="flex-1 px-5 pt-5" showsVerticalScrollIndicator={false}>
        <Text className="text-lg font-bold text-white mb-1">Ses veya Video Seç</Text>
        <Text className="text-xs text-gray-400 mb-4">
          Desteklenen: MP3, WAV, M4A, FLAC, OGG, MP4, MKV
        </Text>

        {/* Dosya seçici alan */}
        <TouchableOpacity
          onPress={pickFile}
          className="rounded-2xl p-8 items-center mb-4 border-2"
          style={{
            backgroundColor: "#1C1B1F",
            borderColor: pickedUri ? "#D0BCFF" : "#424242",
          }}
          activeOpacity={0.7}
        >
          <Text className="text-4xl mb-3">{pickedUri ? "🎵" : "📂"}</Text>
          <Text
            className="text-sm text-center font-medium"
            style={{ color: pickedUri ? "#E3E3E3" : "#9E9E9E" }}
            numberOfLines={2}
          >
            {pickedUri ? pickedName : "Dosya seçmek için dokun"}
          </Text>
          {pickedUri && (
            <Text className="text-xs text-gray-500 mt-1">Değiştirmek için tekrar dokun</Text>
          )}
        </TouchableOpacity>

        {pickedUri && (
          <>
            {/* Not başlığı */}
            <TextInput
              value={title}
              onChangeText={setTitle}
              placeholder="Not başlığı"
              placeholderTextColor="#9E9E9E"
              className="rounded-xl px-4 py-3 text-sm text-white mb-4 border"
              style={{ backgroundColor: "#1C1B1F", borderColor: "#424242" }}
            />

            {/* Model seçimi */}
            <View className="rounded-2xl p-4 mb-4" style={{ backgroundColor: "#1C1B1F" }}>
              <Text className="text-xs font-bold mb-3" style={{ color: "#E8DEF8" }}>
                Model Seçimi
              </Text>
              <View className="flex-row">
                <SelectButton
                  label="Small (Hızlı)"
                  selected={settings.selectedModel === "small"}
                  onPress={() => updateModel("small" as ModelSize)}
                />
                <SelectButton
                  label="Medium (Hassas)"
                  selected={settings.selectedModel === "medium"}
                  onPress={() => updateModel("medium" as ModelSize)}
                />
              </View>

              <Text className="text-xs font-bold mt-4 mb-3" style={{ color: "#E8DEF8" }}>
                Dil
              </Text>
              <View className="flex-row">
                {(
                  [
                    ["auto", "Otomatik"],
                    ["tr", "Türkçe"],
                    ["en", "İngilizce"],
                  ] as [Language, string][]
                ).map(([code, lbl]) => (
                  <SelectButton
                    key={code}
                    label={lbl}
                    selected={settings.selectedLanguage === code}
                    onPress={() => updateLanguage(code)}
                  />
                ))}
              </View>
            </View>

            {/* Gönder butonu */}
            <TouchableOpacity
              onPress={handleSubmit}
              className="rounded-full py-4 items-center mb-8"
              style={{ backgroundColor: "#D0BCFF" }}
              activeOpacity={0.8}
            >
              <Text className="text-base font-bold" style={{ color: "#121212" }}>
                ⬆️  Transkript Al
              </Text>
            </TouchableOpacity>
          </>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}
