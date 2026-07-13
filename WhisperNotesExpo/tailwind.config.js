/** @type {import('tailwindcss').Config} */
module.exports = {
  // NativeWind v4 preset zorunlu
  presets: [require("nativewind/preset")],
  content: [
    "./app/**/*.{js,jsx,ts,tsx}",
    "./src/**/*.{js,jsx,ts,tsx}",
    "./components/**/*.{js,jsx,ts,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Android uygulamasından birebir alınan renk paleti
        "dark-bg": "#121212",
        "dark-surface": "#1C1B1F",
        "slate-primary": "#381E72",
        "slate-secondary": "#D0BCFF",
        "slate-accent": "#D0BCFF",
        "accent-gold": "#E8DEF8",
        "emerald-green": "#81C784",
        "coral-red": "#F28B82",
        "shady-white": "#2B2930",
        "on-surface": "#E3E3E3",
      },
    },
  },
  plugins: [],
};
