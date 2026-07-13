// settings.tsx — Sunucu URL, model, dil, sağlık kontrolü
import { useState } from "react";
import {
  View, Text, TouchableOpacity, TextInput, ScrollView, Alert,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { router } from "expo-router";
import { useApp } from "../src/context/AppContext";
import { checkServer } from "../src/api/whisperClient";
import type { Language, ModelSize } from "../src/types/note";

function SelectBtn({
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

export default function SettingsScreen() {
  const { settings, updateServerUrl, updateLanguage, updateModel } = useApp();
  const [urlInput, setUrlInput] = useState(settings.serverUrl);
  const [checking, setChecking] = useState(false);

  const saveUrl = async () => {
    await updateServerUrl(urlInput);
    Alert.alert("", "Sunucu URL güncellendi ✓");
  };

  const healthCheck = async () => {
    setChecking(true);
    const ok = await checkServer(urlInput.trimEnd().replace(/\/$/, ""));
    setChecking(false);
    Alert.alert(
      ok ? "Bağlantı başarılı ✓" : "Bağlantı başarısız",
      ok
        ? `${urlInput} adresindeki sunucu yanıt veriyor.`
        : `${urlInput} adresine ulaşılamadı. Backend çalışıyor mu?`
    );
  };

  return (
    <SafeAreaView className="flex-1 bg-dark-bg">
      <View className="flex-row items-center px-2 py-3" style={{ backgroundColor: "#1C1B1F" }}>
        <TouchableOpacity onPress={() => router.back()} className="p-2 mr-2">
          <Text className="text-xl" style={{ color: "#D0BCFF" }}>←</Text>
        </TouchableOpacity>
        <Text className="text-lg font-bold text-white">Ayarlar</Text>
      </View>

      <ScrollView className="flex-1 px-5 pt-5" showsVerticalScrollIndicator={false}>
        {/* Sunucu bağlantısı */}
        <Text className="text-xs font-bold mb-3" style={{ color: "#E8DEF8" }}>
          Sunucu Bağlantısı
        </Text>

        <View className="rounded-2xl p-4 mb-5" style={{ backgroundColor: "#1C1B1F" }}>
          <TextInput
            value={urlInput}
            onChangeText={setUrlInput}
            placeholder="http://192.168.x.x:8000"
            placeholderTextColor="#9E9E9E"
            autoCapitalize="none"
            autoCorrect={false}
            className="rounded-xl px-3 py-3 text-sm text-white mb-3 border"
            style={{ backgroundColor: "#2B2930", borderColor: "#424242" }}
          />

          <View className="flex-row mb-3">
            <TouchableOpacity
              onPress={saveUrl}
              className="flex-1 rounded-lg py-3 items-center mr-2"
              style={{ backgroundColor: "#D0BCFF" }}
            >
              <Text className="text-sm font-bold" style={{ color: "#121212" }}>Kaydet</Text>
            </TouchableOpacity>
            <TouchableOpacity
              onPress={() => {
                setUrlInput("http://127.0.0.1:8000");
                updateServerUrl("http://127.0.0.1:8000");
              }}
              className="flex-1 rounded-lg py-3 items-center border"
              style={{ borderColor: "#424242" }}
            >
              <Text className="text-sm text-gray-400">Telefon</Text>
            </TouchableOpacity>
          </View>

          <TouchableOpacity
            onPress={healthCheck}
            disabled={checking}
            className="rounded-lg py-3 items-center border"
            style={{ borderColor: "#D0BCFF55" }}
          >
            <Text className="text-sm" style={{ color: "#D0BCFF" }}>
              {checking ? "Kontrol ediliyor..." : "🔍  Bağlantıyı Test Et"}
            </Text>
          </TouchableOpacity>

          <Text className="text-xs text-gray-500 mt-3 leading-5 font-mono">
            {"Kabloyla: http://127.0.0.1:8000\n"}
            {"Wi-Fi: http://[BİLGİSAYAR_IP]:8000"}
          </Text>
        </View>

        {/* Model seçimi */}
        <Text className="text-xs font-bold mb-3" style={{ color: "#E8DEF8" }}>
          Whisper Modeli
        </Text>
        <View className="rounded-2xl p-4 mb-5" style={{ backgroundColor: "#1C1B1F" }}>
          <View className="flex-row">
            <SelectBtn
              label="Small (Hızlı)"
              selected={settings.selectedModel === "small"}
              onPress={() => updateModel("small" as ModelSize)}
            />
            <SelectBtn
              label="Medium (Hassas)"
              selected={settings.selectedModel === "medium"}
              onPress={() => updateModel("medium" as ModelSize)}
            />
          </View>
        </View>

        {/* Dil seçimi */}
        <Text className="text-xs font-bold mb-3" style={{ color: "#E8DEF8" }}>
          Dil
        </Text>
        <View className="rounded-2xl p-4 mb-5" style={{ backgroundColor: "#1C1B1F" }}>
          <View className="flex-row">
            {(
              [
                ["auto", "Otomatik"],
                ["tr", "Türkçe"],
                ["en", "İngilizce"],
              ] as [Language, string][]
            ).map(([code, lbl]) => (
              <SelectBtn
                key={code}
                label={lbl}
                selected={settings.selectedLanguage === code}
                onPress={() => updateLanguage(code)}
              />
            ))}
          </View>
        </View>

        {/* Nasıl başlatılır */}
        <View className="rounded-2xl p-4 mb-8" style={{ backgroundColor: "#1C1B1F" }}>
          <Text className="text-sm font-bold text-white mb-2">Nasıl Başlatılır?</Text>
          <Text className="text-xs text-gray-400 leading-5 font-mono">
            {"1. cd faster-whisper/backend\n"}
            {"2. pip install -r requirements.txt\n"}
            {"3. python server.py\n"}
            {"4. adb reverse tcp:8000 tcp:8000\n"}
            {"5. Expo Go ile QR okut"}
          </Text>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}
