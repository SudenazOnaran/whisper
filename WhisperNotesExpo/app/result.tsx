// result.tsx — Sonuç ekranı (tam metin + segmentler + paylaş/kopyala)
import { useState } from "react";
import {
  View, Text, TouchableOpacity, ScrollView, Share, Alert,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { router } from "expo-router";
import * as Clipboard from "expo-clipboard";
import { useApp } from "../src/context/AppContext";
import { wordCount } from "../src/utils/export";
import type { Segment } from "../src/types/note";

function InfoChip({ icon, label }: { icon: string; label: string }) {
  return (
    <View
      className="flex-row items-center px-2 py-1 rounded-lg mr-2"
      style={{ backgroundColor: "#1C1B1F" }}
    >
      <Text className="text-xs mr-1">{icon}</Text>
      <Text className="text-xs font-medium" style={{ color: "#BDBDBD" }}>{label}</Text>
    </View>
  );
}

function SegmentCard({ seg }: { seg: Segment }) {
  return (
    <View className="rounded-xl p-3 mb-2 flex-row" style={{ backgroundColor: "#1C1B1F" }}>
      <View
        className="rounded px-2 py-1 mr-3"
        style={{ backgroundColor: "#381E72" }}
      >
        <Text className="text-xs font-bold" style={{ color: "#D0BCFF" }}>{seg.time}</Text>
      </View>
      <Text className="flex-1 text-sm text-white leading-5">{seg.text}</Text>
    </View>
  );
}

export default function ResultScreen() {
  const { result, resultTitle } = useApp();
  const [tab, setTab] = useState<0 | 1>(0);

  if (!result) {
    return (
      <SafeAreaView className="flex-1 bg-dark-bg items-center justify-center">
        <Text className="text-gray-400">Sonuç yok.</Text>
        <TouchableOpacity onPress={() => router.replace("/")} className="mt-4">
          <Text style={{ color: "#D0BCFF" }}>Ana ekrana dön</Text>
        </TouchableOpacity>
      </SafeAreaView>
    );
  }

  const copyToClipboard = async () => {
    await Clipboard.setStringAsync(result.fullText);
    Alert.alert("", "Panoya kopyalandı ✓");
  };

  const shareText = async () => {
    await Share.share({ message: result.fullText, title: resultTitle });
  };

  return (
    <SafeAreaView className="flex-1 bg-dark-bg">
      {/* Üst çubuk */}
      <View style={{ backgroundColor: "#1C1B1F" }}>
        <View className="flex-row items-center justify-between px-2 py-2">
          <View className="flex-row items-center">
            <TouchableOpacity onPress={() => router.replace("/")} className="p-2 mr-1">
              <Text className="text-xl" style={{ color: "#D0BCFF" }}>🏠</Text>
            </TouchableOpacity>
            <Text
              className="text-base font-bold text-white"
              numberOfLines={1}
              style={{ maxWidth: 180 }}
            >
              {resultTitle || "Transkript"}
            </Text>
          </View>
          <View className="flex-row">
            <TouchableOpacity onPress={copyToClipboard} className="p-2">
              <Text className="text-xl">📋</Text>
            </TouchableOpacity>
            <TouchableOpacity onPress={shareText} className="p-2">
              <Text className="text-xl">📤</Text>
            </TouchableOpacity>
          </View>
        </View>

        {/* Sekme çubuğu */}
        <View className="flex-row px-4 pb-0">
          {["Tam Metin", "Zaman Çizelgesi"].map((lbl, i) => (
            <TouchableOpacity
              key={i}
              onPress={() => setTab(i as 0 | 1)}
              className="flex-1 items-center py-2.5"
            >
              <Text
                className="text-sm font-semibold"
                style={{ color: tab === i ? "#D0BCFF" : "#9E9E9E" }}
              >
                {lbl}
              </Text>
              {tab === i && (
                <View className="h-0.5 w-8 mt-1 rounded" style={{ backgroundColor: "#D0BCFF" }} />
              )}
            </TouchableOpacity>
          ))}
        </View>
      </View>

      {/* Meta bilgi */}
      <View className="flex-row px-4 py-3 flex-wrap">
        <InfoChip icon="🌍" label={result.language.toUpperCase()} />
        <InfoChip icon="📋" label={`${result.segments.length} segment`} />
        <InfoChip icon="🔤" label={`${wordCount(result.fullText)} kelime`} />
      </View>

      {/* İçerik */}
      {tab === 0 ? (
        <ScrollView className="flex-1 mx-4" showsVerticalScrollIndicator={false}>
          <View className="rounded-2xl p-4 mb-6" style={{ backgroundColor: "#1C1B1F" }}>
            <Text className="text-sm leading-7" style={{ color: "#BDBDBD" }}>
              {result.fullText}
            </Text>
          </View>
        </ScrollView>
      ) : (
        <ScrollView className="flex-1 px-4" showsVerticalScrollIndicator={false}>
          {result.segments.length === 0 ? (
            <Text className="text-gray-400 text-center mt-10">Segment bilgisi yok.</Text>
          ) : (
            result.segments.map((seg, i) => <SegmentCard key={i} seg={seg} />)
          )}
          <View className="h-6" />
        </ScrollView>
      )}
    </SafeAreaView>
  );
}
