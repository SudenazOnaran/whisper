// Android uygulamasından birebir alınan renk paleti (Color.kt)
export const Colors = {
  darkBg: "#121212",
  darkSurface: "#1C1B1F",
  slatePrimary: "#381E72",
  slateSecondary: "#D0BCFF",
  slateAccent: "#D0BCFF",
  accentGold: "#E8DEF8",
  emeraldGreen: "#81C784",
  coralRed: "#F28B82",
  shadyWhite: "#2B2930",
  onSurface: "#E3E3E3",
  white: "#FFFFFF",
  gray: "#9E9E9E",
  lightGray: "#BDBDBD",
  darkGray: "#424242",
} as const;

export type ColorKey = keyof typeof Colors;
