// index.tsx — Ana ekran
import { View, Text, TouchableOpacity, ScrollView, Modal, Pressable } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { router } from "expo-router";
import { useApp } from "../src/context/AppContext";

function ActionCard({
  title,
  subtitle,
  accentColor,
  onPress,
}: {
  title: string;
  subtitle: string;
  accentColor: string;
  onPress: () => void;
}) {
  return (
    <TouchableOpacity
      onPress={onPress}
      className="rounded-2xl p-5 mb-4 border"
      style={{ backgroundColor: "#1C1B1F", borderColor: accentColor + "55" }}
      activeOpacity={0.7}
    >
      <Text className="text-base font-bold text-white mb-1">{title}</Text>
      <Text className="text-xs text-gray-400">{subtitle}</Text>
    </TouchableOpacity>
  );
}

export default function HomeScreen() {
  const { error, clearError, result, resultTitle, savedNotes } = useApp();

  return (
    <SafeAreaView className="flex-1 bg-dark-bg">
      {/* Hata dialogu */}
      <Modal transparent visible={!!error} animationType="fade">
        <View className="flex-1 items-center justify-center" style={{ backgroundColor: "#00000088" }}>
          <View className="mx-6 rounded-2xl p-6" style={{ backgroundColor: "#1C1B1F" }}>
            <Text className="text-base font-bold text-coral-red mb-2">Bağlantı Hatası</Text>
            <Text className="text-sm text-on-surface mb-4">{error}</Text>
            <View className="flex-row justify-end gap-3">
              <TouchableOpacity onPress={clearError}>
                <Text className="text-gray-400 font-medium">Kapat</Text>
              </TouchableOpacity>
              <TouchableOpacity
                onPress={() => { clearError(); router.push("/settings"); }}
              >
                <Text className="font-medium" style={{ color: "#D0BCFF" }}>Ayarlar</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>

      {/* Başlık çubuğu */}
      <View className="flex-row items-center justify-between px-5 py-4" style={{ backgroundColor: "#1C1B1F" }}>
        <View>
          <Text className="text-xs font-medium tracking-widest" style={{ color: "#D0BCFF" }}>
            WHISPER NOTES
          </Text>
          <Text className="text-xl font-bold text-white">Ses → Metin → Not</Text>
        </View>
        <TouchableOpacity onPress={() => router.push("/settings")} className="p-2">
          <Text className="text-2xl">⚙️</Text>
        </TouchableOpacity>
      </View>

      <ScrollView className="flex-1 px-5 pt-5" showsVerticalScrollIndicator={false}>
        {/* Tanıtım kartı */}
        <View className="rounded-2xl p-5 mb-5 flex-row items-center" style={{ backgroundColor: "#1C1B1F" }}>
          <View className="w-14 h-14 rounded-full items-center justify-center mr-4" style={{ backgroundColor: "#381E72" }}>
            <Text className="text-2xl">🎙️</Text>
          </View>
          <View className="flex-1">
            <Text className="text-sm font-bold" style={{ color: "#E8DEF8" }}>Faster-Whisper Motorlu</Text>
            <Text className="text-xs text-gray-400 mt-0.5">Ses ve video dosyalarını{"\n"}anında metne dönüştürün</Text>
          </View>
        </View>

        <Text className="text-lg font-bold text-white mb-4">Ne yapmak istersiniz?</Text>

        <ActionCard
          title="📁  Dosya Yükle"
          subtitle="MP3, WAV, MP4, M4A, FLAC seç ve transkript al"
          accentColor="#D0BCFF"
          onPress={() => router.push("/upload")}
        />

        <ActionCard
          title="🎙️  Canlı Kayıt"
          subtitle="Mikrofonu aç, konuş, kaydı bitir ve not al"
          accentColor="#F28B82"
          onPress={() => router.push("/record")}
        />

        <ActionCard
          title="📝  Notlarım"
          subtitle={
            savedNotes.length === 0
              ? "Kalıcı notların burada listelenecek"
              : `${savedNotes.length} kayıtlı not`
          }
          accentColor="#E8DEF8"
          onPress={() => router.push("/notes")}
        />

        {/* Son sonuç kartı */}
        {result && (
          <TouchableOpacity
            className="rounded-2xl p-4 mb-4 border flex-row items-center"
            style={{ backgroundColor: "#1C1B1F", borderColor: "#D0BCFF44" }}
            onPress={() => router.push("/result")}
            activeOpacity={0.7}
          >
            <Text className="text-2xl mr-3">📄</Text>
            <View className="flex-1">
              <Text className="text-sm font-semibold text-white" numberOfLines={1}>
                {resultTitle || "Son transkript"}
              </Text>
              <Text className="text-xs text-gray-400 mt-0.5">
                {result.language.toUpperCase()} • {result.segments.length} segment
              </Text>
            </View>
            <Text className="text-gray-400">›</Text>
          </TouchableOpacity>
        )}

        <View className="h-8" />
      </ScrollView>
    </SafeAreaView>
  );
}
