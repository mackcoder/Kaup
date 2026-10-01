import { useState } from "react";
import { CATEGORIES } from "./CategoryPage";
import type { ChatDocProps } from "./ContractChatPage";

const STATUS_CFG = {
  ok:      { emoji: "✅", label: "Aprovado",   color: "#059669", bg: "#d1fae5", border: "#a7f3d0" },
  warning: { emoji: "⚠️", label: "Atenção",    color: "#d97706", bg: "#fef3c7", border: "#fde68a" },
  pending: { emoji: "🕐", label: "Aguardando", color: "#6366f1", bg: "#eef2ff", border: "#c7d2fe" },
};

type FilterKey = "all" | "ok" | "warning" | "pending";

interface FlatDoc {
  emoji: string;
  name: string;
  description: string;
  date: string;
  status: "ok" | "warning" | "pending";
  catKey: string;
  catEmoji: string;
  catLabel: string;
  catColor: string;
  catColorLight: string;
  catColorBg: string;
}

const ALL_DOCS: FlatDoc[] = CATEGORIES.flatMap((cat) =>
  cat.docs.map((doc) => ({
    ...doc,
    catKey: cat.key,
    catEmoji: cat.emoji,
    catLabel: cat.label,
    catColor: cat.color,
    catColorLight: cat.colorLight,
    catColorBg: cat.colorBg,
  }))
);

interface Props {
  onBack: () => void;
  onOpenDoc: (doc: ChatDocProps) => void;
  initialFilter?: FilterKey;
}

export default function AllDocumentsPage({ onBack, onOpenDoc, initialFilter }: Props) {
  const [filter, setFilter] = useState<FilterKey>(initialFilter ?? "all");
  const [search, setSearch] = useState("");
  const [tapped, setTapped] = useState<number | null>(null);

  const handleTap = (idx: number, doc: FlatDoc) => {
    setTapped(idx);
    setTimeout(() => {
      setTapped(null);
      onOpenDoc({
        emoji: doc.emoji,
        name: doc.name,
        description: doc.description,
        catKey: doc.catKey,
        catLabel: doc.catLabel,
        catEmoji: doc.catEmoji,
        catColor: doc.catColor,
        catColorLight: doc.catColorLight,
        catColorBg: doc.catColorBg,
        status: doc.status,
      });
    }, 150);
  };

  const filtered = ALL_DOCS.filter((d) => {
    const matchFilter = filter === "all" || d.status === filter;
    const q = search.toLowerCase();
    const matchSearch =
      !q ||
      d.name.toLowerCase().includes(q) ||
      d.catLabel.toLowerCase().includes(q) ||
      d.description.toLowerCase().includes(q);
    return matchFilter && matchSearch;
  });

  const counts = {
    all: ALL_DOCS.length,
    ok: ALL_DOCS.filter((d) => d.status === "ok").length,
    warning: ALL_DOCS.filter((d) => d.status === "warning").length,
    pending: ALL_DOCS.filter((d) => d.status === "pending").length,
  };

  const FILTERS: { key: FilterKey; label: string; emoji: string; color: string; bg: string; activeBg: string; activeBorder: string }[] = [
    { key: "all",     label: "Todos",     emoji: "📋", color: "#475569", bg: "#f8fafc", activeBg: "#1e293b",  activeBorder: "#1e293b" },
    { key: "ok",      label: "Aprovados", emoji: "✅", color: "#059669", bg: "#f0fdf4", activeBg: "#059669",  activeBorder: "#059669" },
    { key: "warning", label: "Atenção",   emoji: "⚠️", color: "#d97706", bg: "#fffbeb", activeBg: "#d97706",  activeBorder: "#d97706" },
    { key: "pending", label: "Aguardando",emoji: "🕐", color: "#6366f1", bg: "#eef2ff", activeBg: "#6366f1",  activeBorder: "#6366f1" },
  ];

  return (
    <div
      style={{
        minHeight: "100%",
        background: "#f5f7fa",
        display: "flex",
        flexDirection: "column",
        maxWidth: 430,
        margin: "0 auto",
        paddingBottom: 40,
      }}
    >
      {/* ── HEADER ── */}
      <div
        style={{
          background: "#fff",
          borderBottom: "1.5px solid #e2e8f0",
          padding: "20px 20px 16px",
          paddingTop: "max(52px, env(safe-area-inset-top, 0px))",
          position: "sticky",
          top: 0,
          zIndex: 20,
        }}
      >
        {/* Back + title */}
        <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: 16 }}>
          <button
            onClick={onBack}
            style={{
              display: "flex",
              alignItems: "center",
              gap: 8,
              background: "#f1f5f9",
              border: "1.5px solid #e2e8f0",
              borderRadius: 14,
              padding: "10px 16px",
              cursor: "pointer",
              flexShrink: 0,
            }}
          >
            <span style={{ fontSize: 18, color: "#334155", lineHeight: 1 }}>←</span>
            <span style={{ fontSize: 15, fontWeight: 800, color: "#334155" }}>Voltar</span>
          </button>

          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 20, fontWeight: 900, color: "#1e293b", lineHeight: 1.1 }}>
              Todos os Documentos
            </div>
            <div style={{ fontSize: 12, color: "#94a3b8", fontWeight: 600, marginTop: 2 }}>
              {ALL_DOCS.length} documentos no total
            </div>
          </div>
        </div>

        {/* Search bar */}
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 10,
            background: "#f8fafc",
            border: "1.5px solid #e2e8f0",
            borderRadius: 14,
            padding: "10px 14px",
            marginBottom: 14,
          }}
        >
          <span style={{ fontSize: 18, flexShrink: 0 }}>🔍</span>
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Buscar documento..."
            style={{
              flex: 1,
              background: "transparent",
              border: "none",
              outline: "none",
              fontSize: 15,
              fontWeight: 600,
              color: "#1e293b",
              fontFamily: "inherit",
            }}
          />
          {search && (
            <button
              onClick={() => setSearch("")}
              style={{ background: "none", border: "none", cursor: "pointer", fontSize: 16, color: "#94a3b8", padding: 0 }}
            >
              ✕
            </button>
          )}
        </div>

        {/* Filter chips */}
        <div style={{ display: "flex", gap: 8, overflowX: "auto", paddingBottom: 2 }}>
          {FILTERS.map((f) => {
            const active = filter === f.key;
            return (
              <button
                key={f.key}
                onClick={() => setFilter(f.key)}
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: 6,
                  padding: "7px 14px",
                  borderRadius: 20,
                  border: `1.5px solid ${active ? f.activeBorder : "#e2e8f0"}`,
                  background: active ? f.activeBg : f.bg,
                  cursor: "pointer",
                  whiteSpace: "nowrap",
                  flexShrink: 0,
                  transition: "all 0.15s",
                }}
              >
                <span style={{ fontSize: 14 }}>{f.emoji}</span>
                <span
                  style={{
                    fontSize: 13,
                    fontWeight: 800,
                    color: active ? "#fff" : f.color,
                  }}
                >
                  {f.label}
                </span>
                <span
                  style={{
                    fontSize: 11,
                    fontWeight: 900,
                    color: active ? "rgba(255,255,255,0.8)" : "#94a3b8",
                    background: active ? "rgba(255,255,255,0.2)" : "#f1f5f9",
                    borderRadius: 8,
                    padding: "1px 6px",
                    minWidth: 18,
                    textAlign: "center",
                  }}
                >
                  {counts[f.key]}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      {/* ── RESULTS COUNT ── */}
      {(search || filter !== "all") && (
        <div style={{ padding: "12px 20px 0" }}>
          <span style={{ fontSize: 13, fontWeight: 700, color: "#94a3b8" }}>
            {filtered.length} resultado{filtered.length !== 1 ? "s" : ""} encontrado{filtered.length !== 1 ? "s" : ""}
          </span>
        </div>
      )}

      {/* ── CATEGORY GROUPS ── */}
      {filtered.length === 0 ? (
        <div
          style={{
            flex: 1,
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            justifyContent: "center",
            padding: "60px 32px",
            gap: 14,
          }}
        >
          <span style={{ fontSize: 56 }}>🔍</span>
          <div style={{ fontSize: 18, fontWeight: 900, color: "#334155", textAlign: "center" }}>
            Nenhum documento encontrado
          </div>
          <div style={{ fontSize: 14, fontWeight: 600, color: "#94a3b8", textAlign: "center" }}>
            Tente mudar o filtro ou o texto da busca
          </div>
          <button
            onClick={() => { setFilter("all"); setSearch(""); }}
            style={{
              marginTop: 8,
              padding: "12px 24px",
              background: "#1e293b",
              border: "none",
              borderRadius: 14,
              fontSize: 15,
              fontWeight: 800,
              color: "#fff",
              cursor: "pointer",
            }}
          >
            Ver todos
          </button>
        </div>
      ) : (
        <div style={{ padding: "16px 20px 0" }}>
          {CATEGORIES.map((cat) => {
            const catDocs = filtered.filter((d) => d.catKey === cat.key);
            if (catDocs.length === 0) return null;
            return (
              <div key={cat.key} style={{ marginBottom: 24 }}>
                {/* Section header */}
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: 8,
                    marginBottom: 10,
                  }}
                >
                  <div
                    style={{
                      width: 30,
                      height: 30,
                      borderRadius: 9,
                      background: cat.colorBg,
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      fontSize: 16,
                    }}
                  >
                    {cat.emoji}
                  </div>
                  <span style={{ fontSize: 15, fontWeight: 900, color: "#1e293b" }}>{cat.label}</span>
                  <span
                    style={{
                      fontSize: 11,
                      fontWeight: 800,
                      color: cat.color,
                      background: cat.colorBg,
                      borderRadius: 8,
                      padding: "2px 8px",
                      marginLeft: 2,
                    }}
                  >
                    {catDocs.length}
                  </span>
                </div>

                {/* Compact cards */}
                <div
                  style={{
                    background: "#fff",
                    borderRadius: 20,
                    border: "1.5px solid #e2e8f0",
                    overflow: "hidden",
                    boxShadow: "0 2px 8px rgba(0,0,0,0.05)",
                  }}
                >
                  {catDocs.map((doc, i) => {
                    const globalIdx = ALL_DOCS.indexOf(doc);
                    const st = STATUS_CFG[doc.status];
                    const isTapped = tapped === globalIdx;
                    const isLast = i === catDocs.length - 1;

                    return (
                      <button
                        key={i}
                        onClick={() => handleTap(globalIdx, doc)}
                        style={{
                          display: "flex",
                          alignItems: "center",
                          gap: 12,
                          padding: "13px 14px",
                          width: "100%",
                          background: isTapped ? cat.colorBg : "#fff",
                          border: "none",
                          borderBottom: isLast ? "none" : "1.5px solid #f1f5f9",
                          cursor: "pointer",
                          textAlign: "left",
                          transition: "background 0.12s",
                        }}
                      >
                        {/* Doc emoji in category color */}
                        <div
                          style={{
                            width: 46,
                            height: 46,
                            borderRadius: 13,
                            background: cat.colorBg,
                            border: `1.5px solid ${cat.colorLight}40`,
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            fontSize: 22,
                            flexShrink: 0,
                          }}
                        >
                          {doc.emoji}
                        </div>

                        {/* Text */}
                        <div style={{ flex: 1, minWidth: 0 }}>
                          <div
                            style={{
                              fontSize: 14,
                              fontWeight: 800,
                              color: "#1e293b",
                              lineHeight: 1.3,
                              whiteSpace: "nowrap",
                              overflow: "hidden",
                              textOverflow: "ellipsis",
                            }}
                          >
                            {doc.name}
                          </div>
                          <div style={{ display: "flex", alignItems: "center", gap: 6, marginTop: 3 }}>
                            <span
                              style={{
                                fontSize: 11,
                                fontWeight: 800,
                                color: cat.color,
                                background: cat.colorBg,
                                borderRadius: 6,
                                padding: "1px 7px",
                              }}
                            >
                              {cat.emoji} {cat.label}
                            </span>
                            <span style={{ fontSize: 11, color: "#94a3b8", fontWeight: 600 }}>
                              {doc.date}
                            </span>
                          </div>
                        </div>

                        {/* Status dot */}
                        <div
                          style={{
                            flexShrink: 0,
                            display: "flex",
                            flexDirection: "column",
                            alignItems: "center",
                            gap: 3,
                          }}
                        >
                          <div
                            style={{
                              width: 28,
                              height: 28,
                              borderRadius: 8,
                              background: st.bg,
                              border: `1.5px solid ${st.border}`,
                              display: "flex",
                              alignItems: "center",
                              justifyContent: "center",
                              fontSize: 14,
                            }}
                          >
                            {st.emoji}
                          </div>
                        </div>

                        <span style={{ color: "#cbd5e1", fontSize: 16, flexShrink: 0 }}>›</span>
                      </button>
                    );
                  })}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
