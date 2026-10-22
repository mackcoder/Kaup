import { useState } from "react";
import { useApp } from "../app/AppProvider";
import type { DocumentView, FilterKey } from "../types/domain";
import { STATUS as STATUS_CFG } from "../styles/status";
import {
  filterDocuments,
  countDocuments,
} from "../features/documents/selectors";
interface Props {
  onBack: () => void;
  onReset: () => void;
  onOpenDoc: (doc: DocumentView) => void;
  filter: FilterKey;
  setFilter: (v: FilterKey) => void;
  search: string;
  setSearch: (v: string) => void;
}

export default function AllDocumentsPage({
  onBack,
  onOpenDoc,
  filter,
  setFilter,
  search,
  setSearch,
  onReset,
}: Props) {
  const { categories: CATEGORIES, documents: ALL_DOCS } = useApp();
  const [tapped, setTapped] = useState<number | null>(null);

  const handleTap = (idx: number, doc: DocumentView) => {
    setTapped(idx);
    setTimeout(() => {
      setTapped(null);
      onOpenDoc(doc);
    }, 150);
  };

  const filtered = filterDocuments(ALL_DOCS, filter, search);
  const counts = countDocuments(ALL_DOCS);

  const FILTERS: {
    key: FilterKey;
    label: string;
    emoji: string;
    color: string;
    bg: string;
    activeBg: string;
    activeBorder: string;
  }[] = [
    {
      key: "all",
      label: "Todos",
      emoji: "📋",
      color: "#475569",
      bg: "#f8fafc",
      activeBg: "#1e293b",
      activeBorder: "#1e293b",
    },
    {
      key: "ok",
      label: "Aprovados",
      emoji: "✅",
      color: "#059669",
      bg: "#f0fdf4",
      activeBg: "#059669",
      activeBorder: "#059669",
    },
    {
      key: "warning",
      label: "Atenção",
      emoji: "⚠️",
      color: "#d97706",
      bg: "#fffbeb",
      activeBg: "#d97706",
      activeBorder: "#d97706",
    },
    {
      key: "pending",
      label: "Aguardando",
      emoji: "🕐",
      color: "#6366f1",
      bg: "#eef2ff",
      activeBg: "#6366f1",
      activeBorder: "#6366f1",
    },
  ];

  return (
    <div className="kaup-1">
      {/* ── HEADER ── */}
      <div className="kaup-56">
        {/* Back + title */}
        <div className="kaup-57">
          <button onClick={onBack} className="kaup-58">
            <span className="kaup-59">←</span>
            <span className="kaup-60">Voltar</span>
          </button>

          <div className="kaup-25">
            <div className="kaup-61">Todos os Documentos</div>
            <div className="kaup-62">{ALL_DOCS.length} documentos no total</div>
          </div>
        </div>

        {/* Search bar */}
        <div className="kaup-63">
          <span className="kaup-64">🔍</span>
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            aria-label="Buscar documento"
            placeholder="Buscar documento..."
            className="kaup-65"
          />
          {search && (
            <button
              aria-label="Limpar busca"
              onClick={() => setSearch("")}
              className="kaup-66"
            >
              ✕
            </button>
          )}
        </div>

        {/* Filter chips */}
        <div className="kaup-67">
          {FILTERS.map((f) => {
            const active = filter === f.key;
            return (
              <button
                key={f.key}
                aria-pressed={active}
                onClick={() => setFilter(f.key)}
                className="kaup-68"
                style={{
                  border: `1.5px solid ${active ? f.activeBorder : "#e2e8f0"}`,
                  background: active ? f.activeBg : f.bg,
                }}
              >
                <span className="kaup-69">{f.emoji}</span>
                <span
                  className="kaup-70"
                  style={{ color: active ? "#fff" : f.color }}
                >
                  {f.label}
                </span>
                <span
                  className="kaup-71"
                  style={{
                    color: active ? "rgba(255,255,255,0.8)" : "#94a3b8",
                    background: active ? "rgba(255,255,255,0.2)" : "#f1f5f9",
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
        <div className="kaup-72">
          <span className="kaup-73">
            {filtered.length} resultado{filtered.length !== 1 ? "s" : ""}{" "}
            encontrado{filtered.length !== 1 ? "s" : ""}
          </span>
        </div>
      )}

      {/* ── CATEGORY GROUPS ── */}
      {filtered.length === 0 ? (
        <div className="kaup-74">
          <span className="kaup-75">🔍</span>
          <div className="kaup-76">Nenhum documento encontrado</div>
          <div className="kaup-77">
            Tente mudar o filtro ou o texto da busca
          </div>
          <button onClick={onReset} className="kaup-78">
            Ver todos
          </button>
        </div>
      ) : (
        <div className="kaup-79">
          {CATEGORIES.map((cat) => {
            const catDocs = filtered.filter((d) => d.catKey === cat.key);
            if (catDocs.length === 0) return null;
            return (
              <div key={cat.key} className="kaup-80">
                {/* Section header */}
                <div className="kaup-81">
                  <div className="kaup-82" style={{ background: cat.colorBg }}>
                    {cat.emoji}
                  </div>
                  <span className="kaup-83">{cat.label}</span>
                  <span
                    className="kaup-84"
                    style={{ color: cat.color, background: cat.colorBg }}
                  >
                    {catDocs.length}
                  </span>
                </div>

                {/* Compact cards */}
                <div className="kaup-85">
                  {catDocs.map((doc, i) => {
                    const globalIdx = ALL_DOCS.indexOf(doc);
                    const st = STATUS_CFG[doc.status];
                    const isTapped = tapped === globalIdx;
                    const isLast = i === catDocs.length - 1;

                    return (
                      <button
                        key={i}
                        onClick={() => handleTap(globalIdx, doc)}
                        className="kaup-86"
                        style={{
                          background: isTapped ? cat.colorBg : "#fff",
                          borderBottom: isLast ? "none" : "1.5px solid #f1f5f9",
                        }}
                      >
                        {/* Doc emoji in category color */}
                        <div
                          className="kaup-87"
                          style={{
                            background: cat.colorBg,
                            border: `1.5px solid ${cat.colorLight}40`,
                          }}
                        >
                          {doc.emoji}
                        </div>

                        {/* Text */}
                        <div className="kaup-25">
                          <div className="kaup-88">{doc.name}</div>
                          <div className="kaup-89">
                            <span
                              className="kaup-90"
                              style={{
                                color: cat.color,
                                background: cat.colorBg,
                              }}
                            >
                              {cat.emoji} {cat.label}
                            </span>
                            <span className="kaup-91">{doc.date}</span>
                          </div>
                        </div>

                        {/* Status dot */}
                        <div className="kaup-92">
                          <div
                            className="kaup-93"
                            style={{
                              background: st.bg,
                              border: `1.5px solid ${st.border}`,
                            }}
                          >
                            {st.emoji}
                          </div>
                        </div>

                        <span className="kaup-94">›</span>
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
