// processing.tsx — İşleniyor ekranı
import { View, Text, ActivityIndicator } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { useApp } from "../src/context/AppContext";
import type { ProcessingStep } from "../src/context/AppContext";

function StepRow({ step }: { step: ProcessingStep }) {
  const bg = step.done
    ? "#81C784"
    : step.active
    ? "#D0BCFF"
    : "#424242";

  return (
    <View className="flex-row items-center mb-4">
      <View
        className="w-7 h-7 rounded-full items-center justify-center mr-3"
        style={{ backgroundColor: bg }}
      >
        {step.done ? (
          <Text className="text-xs text-white font-bold">✓</Text>
        ) : step.active ? (
          <ActivityIndicator size="small" color="#121212" />
        ) : (
          <View className="w-1.5 h-1.5 rounded-full bg-gray-500" />
        )}
      </View>
      <Text
        className="text-sm"
        style={{
          color: step.done ? "#BDBDBD" : step.active ? "#FFFFFF" : "#9E9E9E",
          fontWeight: step.active ? "700" : "400",
        }}
      >
        {step.label}
      </Text>
    </View>
  );
}

export default function ProcessingScreen() {
  const { processingSteps, processingTitle } = useApp();

  return (
    <SafeAreaView className="flex-1 bg-dark-bg items-center justify-center">
      <View className="px-8 w-full items-center">
        {/* Dönen gösterge */}
        <View className="w-24 h-24 items-center justify-center mb-7">
          <ActivityIndicator size="large" color="#D0BCFF" />
          <Text className="text-3xl absolute">🎙️</Text>
        </View>

        <Text className="text-xl font-bold text-white text-center mb-2">
          İşleniyor: {processingTitle || "Ses dosyası"}
        </Text>
        <Text className="text-sm text-gray-400 text-center mb-8">
          Faster-Whisper transkript oluşturuyor...
        </Text>

        {/* Adımlar */}
        <View className="w-full">
          {processingSteps.map((step, i) => (
            <StepRow key={i} step={step} />
          ))}
        </View>
      </View>
    </SafeAreaView>
  );
}
