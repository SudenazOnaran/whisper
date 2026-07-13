// babel.config.js — Expo SDK 54 + NativeWind v4 + Reanimated uyumlu yapılandırma
module.exports = function (api) {
  api.cache(true);
  return {
    presets: [
      [
        "babel-preset-expo",
        {
          // NativeWind v4: JSX transform kaynağını nativewind olarak ayarla
          jsxImportSource: "nativewind",
        },
      ],
    ],
    plugins: [
      // react-native-reanimated plugin MUTLAKA en sonda olmalı
      "react-native-reanimated/plugin",
    ],
  };
};
