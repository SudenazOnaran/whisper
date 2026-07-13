// metro.config.js — Expo SDK 54 + NativeWind v4 uyumlu yapılandırma
const { getDefaultConfig } = require("expo/metro-config");
const { withNativeWind } = require("nativewind/metro");

const config = getDefaultConfig(__dirname);

// NativeWind v4 için gerekli CSS işleme desteği
module.exports = withNativeWind(config, { input: "./global.css" });
