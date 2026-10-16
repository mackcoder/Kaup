import type { FontSize } from "../../types/domain";
export const FONT_SCALE: Record<FontSize, number> = {
  small: 0.88,
  medium: 1,
  large: 1.14,
};
export function profileStyles(
  fs: (n: number) => number,
  audioKey: string | null,
) {
  return {
    page: {
      minHeight: "100%",
      background: "#f5f7fa",
      display: "flex" as const,
      flexDirection: "column" as const,
      maxWidth: 430,
      margin: "0 auto",
      paddingBottom: 40,
      fontSize: fs(14),
      fontFamily: "'Nunito', sans-serif",
    },
    header: {
      background: "#fff",
      borderBottom: "1.5px solid #e2e8f0",
      padding: "16px 20px 14px",
      paddingTop: "max(52px, env(safe-area-inset-top, 0px))",
      position: "sticky" as const,
      top: 0,
      zIndex: 20,
    },
    backBtn: {
      display: "flex" as const,
      alignItems: "center" as const,
      gap: 8,
      background: "#f1f5f9",
      border: "1.5px solid #e2e8f0",
      borderRadius: 14,
      padding: "12px 18px",
      cursor: "pointer",
      minHeight: 52,
      width: "fit-content" as const,
    },
    section: {
      padding: "0 18px",
      marginTop: 24,
    },
    sectionTitle: {
      fontSize: fs(17),
      fontWeight: 900,
      color: "#1e293b",
      marginBottom: 4,
    },
    sectionSub: {
      fontSize: fs(13),
      fontWeight: 600,
      color: "#64748b",
      marginBottom: 14,
    },
    card: {
      background: "#fff",
      border: "1.5px solid #e2e8f0",
      borderRadius: 22,
      padding: "18px 18px",
      boxShadow: "0 2px 10px rgba(0,0,0,0.06)",
    },
    bigBtn: (active?: boolean, activeColor?: string) => ({
      display: "flex" as const,
      alignItems: "center" as const,
      gap: 14,
      padding: "16px 18px",
      minHeight: 64,
      background: active
        ? activeColor
          ? `${activeColor}15`
          : "#eff6ff"
        : "#fff",
      border: `2px solid ${active ? (activeColor ?? "#2563eb") : "#e2e8f0"}`,
      borderRadius: 18,
      cursor: "pointer",
      width: "100%",
      textAlign: "left" as const,
      transition: "all 0.14s",
    }),
    audioBtn: (key: string) => ({
      display: "flex" as const,
      alignItems: "center" as const,
      gap: 6,
      background: audioKey === key ? "#eff6ff" : "#f8fafc",
      border: `1.5px solid ${audioKey === key ? "#bfdbfe" : "#e2e8f0"}`,
      borderRadius: 12,
      padding: "7px 14px",
      cursor: "pointer",
      fontSize: fs(13),
      fontWeight: 800,
      color: audioKey === key ? "#2563eb" : "#64748b",
      transition: "all 0.14s",
    }),
  };
}
