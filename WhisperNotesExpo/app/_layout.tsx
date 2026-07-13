// _layout.tsx — Expo Router kök layout, NativeWind CSS, AppProvider
import "../global.css";
import { Stack } from "expo-router";
import { StatusBar } from "expo-status-bar";
import { SafeAreaProvider } from "react-native-safe-area-context";
import { GestureHandlerRootView } from "react-native-gesture-handler";
import { AppProvider } from "../src/context/AppContext";

export default function RootLayout() {
  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <SafeAreaProvider>
        <AppProvider>
          <StatusBar style="light" backgroundColor="#121212" />
          <Stack
            screenOptions={{
              headerShown: false,
              contentStyle: { backgroundColor: "#121212" },
              animation: "fade",
            }}
          >
            <Stack.Screen name="index" />
            <Stack.Screen name="upload" />
            <Stack.Screen name="record" />
            <Stack.Screen name="processing" />
            <Stack.Screen name="result" />
            <Stack.Screen name="notes" />
            <Stack.Screen name="settings" />
          </Stack>
        </AppProvider>
      </SafeAreaProvider>
    </GestureHandlerRootView>
  );
}
