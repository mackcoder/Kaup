import { useState, useRef } from "react";
import type { ChatDocProps } from "./ContractChatPage";

interface DocItem {
  emoji: string;
  name: string;
  description: string;
  date: string;
  status: "ok" | "warning" | "pending";
}

interface CategoryConfig {
  key: string;
  emoji: string;
  label: string;
  color: string;
  colorLight: string;
  colorBg: string;
  gradient: [string, string];
  docs: DocItem[];
}

const STATUS = {
  ok: { emoji: "✅", label: "Aprovado", color: "#059669", bg: "#d1fae5", border: "#a7f3d0" },
  warning: { emoji: "⚠️", label: "Atenção", color: "#d97706", bg: "#fef3c7", border: "#fde68a" },
  pending: { emoji: "🕐", label: "Aguardando", color: "#6366f1", bg: "#eef2ff", border: "#c7d2fe" },
};

export const CATEGORIES: CategoryConfig[] = [
  {
    key: "Carros",
    emoji: "🚗",
    label: "Carros",
    color: "#b45309",
    colorLight: "#d97706",
    colorBg: "#fef3c7",
    gradient: ["#92400e", "#d97706"],
    docs: [
      { emoji: "🚗", name: "Carro Gol 2016", description: "Contrato de compra e venda", date: "12 ago 2026", status: "ok" },
      { emoji: "📄", name: "Financiamento Gol 2016", description: "Contrato de financiamento", date: "12 ago 2026", status: "warning" },
      { emoji: "🛡️", name: "Seguro Gol 2016", description: "Apólice do seguro do veículo", date: "03 mai 2026", status: "ok" },
      { emoji: "🔧", name: "Manutenção Gol 2016", description: "Documento de revisão", date: "20 jan 2026", status: "pending" },
    ],
  },
  {
    key: "Casa",
    emoji: "🏠",
    label: "Casa",
    color: "#0369a1",
    colorLight: "#0284c7",
    colorBg: "#e0f2fe",
    gradient: ["#0c4a6e", "#0284c7"],
    docs: [
      { emoji: "🏠", name: "Aluguel Ap. Centro", description: "Contrato de locação", date: "01 set 2026", status: "ok" },
      { emoji: "⚡", name: "Energia Elétrica", description: "Contrato com a distribuidora", date: "15 mar 2026", status: "ok" },
      { emoji: "🚿", name: "Serviço de Água", description: "Contrato de fornecimento", date: "15 mar 2026", status: "warning" },
      { emoji: "📶", name: "Internet e TV", description: "Contrato de telecomunicações", date: "10 fev 2026", status: "ok" },
      { emoji: "🔨", name: "Reforma Cozinha", description: "Contrato com a construtora", date: "22 jun 2026", status: "pending" },
    ],
  },
  {
    key: "Trabalho",
    emoji: "💼",
    label: "Trabalho",
    color: "#5b21b6",
    colorLight: "#7c3aed",
    colorBg: "#ede9fe",
    gradient: ["#3b0764", "#7c3aed"],
    docs: [
      { emoji: "📋", name: "Contrato de Trabalho", description: "CLT — Empresa XYZ Ltda.", date: "05 jan 2025", status: "ok" },
      { emoji: "💰", name: "Rescisão Contratual", description: "Termo de rescisão 2024", date: "30 dez 2024", status: "warning" },
      { emoji: "🤝", name: "Contrato Freelance", description: "Prestação de serviços", date: "14 ago 2026", status: "ok" },
      { emoji: "📑", name: "Acordo de Sigilo", description: "NDA com cliente", date: "14 ago 2026", status: "ok" },
      { emoji: "🏆", name: "Bônus e Metas", description: "Adendo salarial 2026", date: "01 mar 2026", status: "pending" },
      { emoji: "🩺", name: "Plano de Saúde", description: "Contrato do convênio médico", date: "05 jan 2025", status: "ok" },
    ],
  },
  {
    key: "Banco",
    emoji: "🏦",
    label: "Banco",
    color: "#065f46",
    colorLight: "#059669",
    colorBg: "#d1fae5",
    gradient: ["#022c22", "#059669"],
    docs: [
      { emoji: "💳", name: "Cartão de Crédito", description: "Contrato do cartão Banco X", date: "10 out 2025", status: "warning" },
      { emoji: "🏦", name: "Conta Corrente", description: "Contrato de abertura de conta", date: "03 abr 2023", status: "ok" },
      { emoji: "💸", name: "Empréstimo Pessoal", description: "Contrato de crédito pessoal", date: "18 jul 2026", status: "warning" },
      { emoji: "📈", name: "Investimento CDB", description: "Contrato de aplicação", date: "02 ago 2026", status: "ok" },
    ],
  },
  {
    key: "Outros",
    emoji: "📄",
    label: "Outros",
    color: "#334155",
    colorLight: "#475569",
    colorBg: "#f1f5f9",
    gradient: ["#0f172a", "#475569"],
    docs: [
      { emoji: "🏥", name: "Plano Dentário", description: "Contrato do convênio odonto", date: "12 fev 2026", status: "ok" },
      { emoji: "📱", name: "Celular Pós-Pago", description: "Contrato com a operadora", date: "07 nov 2025", status: "ok" },
      { emoji: "🎓", name: "Curso Online", description: "Contrato de matrícula", date: "30 jan 2026", status: "pending" },
      { emoji: "🐾", name: "Pet Shop Mensal", description: "Contrato de serviço pet", date: "01 abr 2026", status: "ok" },
      { emoji: "🚚", name: "Mudança Residencial", description: "Contrato da transportadora", date: "25 set 2026", status: "warning" },
      { emoji: "📰", name: "Assinatura Digital", description: "Contrato de assinatura", date: "15 mai 2026", status: "ok" },
    ],
  },
];

interface Props {
  categoryKey: string;
  onBack: () => void;
  onOpenDoc: (doc: ChatDocProps) => void;
}

export default function CategoryPage({ categoryKey, onBack, onOpenDoc }: Props) {
  const cat = CATEGORIES.find((c) => c.key === categoryKey)!;
  const [tapped, setTapped] = useState<number | null>(null);
  const [showUpload, setShowUpload] = useState(false);
  const fileRef = useRef<HTMLInputElement>(null);
  const photoRef = useRef<HTMLInputElement>(null);

  const handleTap = (idx: number) => {
    setTapped(idx);
    setTimeout(() => {
      setTapped(null);
      const doc = cat.docs[idx];
      onOpenDoc({
        emoji: doc.emoji,
        name: doc.name,
        description: doc.description,
        catKey: cat.key,
        catLabel: cat.label,
        catEmoji: cat.emoji,
        catColor: cat.color,
        catColorLight: cat.colorLight,
        catColorBg: cat.colorBg,
        status: doc.status,
      });
    }, 150);
  };

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
      {/* ── HERO HEADER ── */}
      <div
        style={{
          background: `linear-gradient(145deg, ${cat.gradient[0]} 0%, ${cat.gradient[1]} 100%)`,
          padding: "20px 20px 32px",
          paddingTop: "max(52px, env(safe-area-inset-top, 0px))",
          position: "relative",
          overflow: "hidden",
        }}
      >
        {/* Decorative circles */}
        <div style={{ position: "absolute", top: -40, right: -40, width: 140, height: 140, borderRadius: "50%", background: "rgba(255,255,255,0.06)" }} />
        <div style={{ position: "absolute", bottom: -30, left: -20, width: 100, height: 100, borderRadius: "50%", background: "rgba(255,255,255,0.04)" }} />

        {/* Back button */}
        <button
          onClick={onBack}
          style={{
            display: "flex",
            alignItems: "center",
            gap: 8,
            background: "rgba(255,255,255,0.18)",
            border: "1.5px solid rgba(255,255,255,0.3)",
            borderRadius: 14,
            padding: "10px 16px",
            cursor: "pointer",
            marginBottom: 28,
            width: "fit-content",
          }}
        >
          <span style={{ fontSize: 18, color: "#fff", lineHeight: 1 }}>←</span>
          <span style={{ fontSize: 15, fontWeight: 800, color: "#fff" }}>Voltar</span>
        </button>

        {/* Title row */}
        <div style={{ display: "flex", alignItems: "center", gap: 16 }}>
          <div
            style={{
              width: 68,
              height: 68,
              borderRadius: 20,
              background: "rgba(255,255,255,0.18)",
              border: "2px solid rgba(255,255,255,0.3)",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              fontSize: 34,
              flexShrink: 0,
            }}
          >
            {cat.emoji}
          </div>
          <div>
            <div style={{ fontSize: 13, color: "rgba(255,255,255,0.7)", fontWeight: 700, letterSpacing: "0.06em", marginBottom: 4 }}>
              CATEGORIA
            </div>
            <div style={{ fontSize: 26, fontWeight: 900, color: "#fff", lineHeight: 1.1 }}>
              {cat.label}
            </div>
            <div style={{ fontSize: 14, color: "rgba(255,255,255,0.75)", fontWeight: 600, marginTop: 3 }}>
              {cat.docs.length} documento{cat.docs.length !== 1 ? "s" : ""}
            </div>
          </div>
        </div>
      </div>

      {/* ── STATS ROW ── */}
      <div
        style={{
          margin: "0 20px",
          marginTop: -18,
          background: "#fff",
          borderRadius: 20,
          padding: "16px 20px",
          display: "flex",
          gap: 0,
          boxShadow: "0 4px 20px rgba(0,0,0,0.08)",
          border: "1.5px solid #e2e8f0",
        }}
      >
        {(["ok", "warning", "pending"] as const).map((s, i) => {
          const count = cat.docs.filter((d) => d.status === s).length;
          const cfg = STATUS[s];
          return (
            <div
              key={s}
              style={{
                flex: 1,
                textAlign: "center",
                borderRight: i < 2 ? "1.5px solid #f1f5f9" : "none",
                padding: "0 8px",
              }}
            >
              <div style={{ fontSize: 22, fontWeight: 900, color: cfg.color }}>{count}</div>
              <div style={{ fontSize: 11, fontWeight: 700, color: "#94a3b8", marginTop: 1 }}>
                {cfg.label}
              </div>
            </div>
          );
        })}
      </div>

      {/* ── ADD BUTTON ── */}
      <div style={{ padding: "20px 20px 4px" }}>
        <button
          onClick={() => setShowUpload(true)}
          style={{
            width: "100%",
            padding: "16px 20px",
            background: cat.colorBg,
            border: `2.5px dashed ${cat.colorLight}`,
            borderRadius: 20,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            gap: 10,
            cursor: "pointer",
          }}
        >
          <span style={{ fontSize: 22 }}>➕</span>
          <span style={{ fontSize: 16, fontWeight: 800, color: cat.color }}>
            Adicionar documento
          </span>
        </button>
      </div>

      {/* ── DOCUMENT LIST ── */}
      <div style={{ padding: "16px 20px 0", display: "flex", flexDirection: "column", gap: 12 }}>
        <div style={{ fontSize: 17, fontWeight: 900, color: "#1e293b", marginBottom: 2 }}>
          Documentos
        </div>

        {cat.docs.map((doc, idx) => {
          const st = STATUS[doc.status];
          const isTapped = tapped === idx;
          return (
            <button
              key={idx}
              onClick={() => handleTap(idx)}
              style={{
                background: "#fff",
                border: `2px solid ${isTapped ? cat.colorLight : "#e2e8f0"}`,
                borderRadius: 22,
                padding: "16px 16px",
                display: "flex",
                alignItems: "center",
                gap: 14,
                cursor: "pointer",
                boxShadow: isTapped
                  ? `0 4px 20px ${cat.colorLight}25`
                  : "0 2px 8px rgba(0,0,0,0.05)",
                transform: isTapped ? "scale(0.98)" : "scale(1)",
                transition: "all 0.15s ease",
                textAlign: "left",
                width: "100%",
              }}
            >
              {/* Emoji icon */}
              <div
                style={{
                  width: 60,
                  height: 60,
                  borderRadius: 16,
                  background: cat.colorBg,
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  fontSize: 28,
                  flexShrink: 0,
                }}
              >
                {doc.emoji}
              </div>

              {/* Text */}
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 15, fontWeight: 800, color: "#1e293b", lineHeight: 1.3 }}>
                  {doc.name}
                </div>
                <div style={{ fontSize: 13, color: "#64748b", fontWeight: 600, marginTop: 3 }}>
                  {doc.description}
                </div>
                <div style={{ fontSize: 11, color: "#94a3b8", fontWeight: 600, marginTop: 4 }}>
                  {doc.date}
                </div>
              </div>

              {/* Status badge */}
              <div
                style={{
                  flexShrink: 0,
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  gap: 4,
                }}
              >
                <div
                  style={{
                    width: 36,
                    height: 36,
                    borderRadius: 10,
                    background: st.bg,
                    border: `1.5px solid ${st.border}`,
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    fontSize: 18,
                  }}
                >
                  {st.emoji}
                </div>
                <span style={{ fontSize: 10, fontWeight: 800, color: st.color }}>{st.label}</span>
              </div>

              {/* Chevron */}
              <div style={{ color: "#cbd5e1", fontSize: 18, flexShrink: 0, marginLeft: 2 }}>›</div>
            </button>
          );
        })}
      </div>

      {/* ── UPLOAD MODAL ── */}
      {showUpload && (
        <div
          onClick={() => setShowUpload(false)}
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(0,0,0,0.45)",
            zIndex: 50,
            display: "flex",
            alignItems: "flex-end",
            justifyContent: "center",
            maxWidth: 430,
            margin: "0 auto",
          }}
        >
          <div
            onClick={(e) => e.stopPropagation()}
            style={{
              background: "#fff",
              borderRadius: "28px 28px 0 0",
              padding: "10px 24px 44px",
              width: "100%",
              boxShadow: "0 -8px 40px rgba(0,0,0,0.15)",
            }}
          >
            <div style={{ width: 44, height: 5, background: "#e2e8f0", borderRadius: 10, margin: "10px auto 24px" }} />

            {/* Category pill */}
            <div style={{ display: "flex", justifyContent: "center", marginBottom: 16 }}>
              <div
                style={{
                  background: cat.colorBg,
                  border: `1.5px solid ${cat.colorLight}`,
                  borderRadius: 20,
                  padding: "6px 16px",
                  display: "flex",
                  alignItems: "center",
                  gap: 8,
                }}
              >
                <span style={{ fontSize: 18 }}>{cat.emoji}</span>
                <span style={{ fontSize: 14, fontWeight: 800, color: cat.color }}>{cat.label}</span>
              </div>
            </div>

            <div style={{ fontSize: 18, fontWeight: 900, color: "#1e293b", marginBottom: 6, textAlign: "center" }}>
              Como deseja enviar?
            </div>
            <div style={{ fontSize: 14, color: "#64748b", fontWeight: 600, textAlign: "center", marginBottom: 24 }}>
              Escolha uma opção abaixo
            </div>

            <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
              <button
                onClick={() => { photoRef.current?.click(); setShowUpload(false); }}
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: 18,
                  padding: "18px 20px",
                  background: "#eff6ff",
                  border: "2px solid #bfdbfe",
                  borderRadius: 20,
                  cursor: "pointer",
                  width: "100%",
                  textAlign: "left",
                }}
              >
                <span style={{ fontSize: 32 }}>📷</span>
                <div>
                  <div style={{ fontSize: 17, fontWeight: 800, color: "#1e293b" }}>Tirar uma foto</div>
                  <div style={{ fontSize: 13, color: "#64748b", fontWeight: 600, marginTop: 2 }}>
                    Aponte a câmera para o documento
                  </div>
                </div>
              </button>

              <button
                onClick={() => { fileRef.current?.click(); setShowUpload(false); }}
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: 18,
                  padding: "18px 20px",
                  background: "#f0fdf4",
                  border: "2px solid #bbf7d0",
                  borderRadius: 20,
                  cursor: "pointer",
                  width: "100%",
                  textAlign: "left",
                }}
              >
                <span style={{ fontSize: 32 }}>📁</span>
                <div>
                  <div style={{ fontSize: 17, fontWeight: 800, color: "#1e293b" }}>Escolher arquivo</div>
                  <div style={{ fontSize: 13, color: "#64748b", fontWeight: 600, marginTop: 2 }}>
                    PDF, imagem ou documento
                  </div>
                </div>
              </button>
            </div>

            <input ref={fileRef} type="file" accept=".pdf,.doc,.docx,.txt,.png,.jpg,.jpeg" style={{ display: "none" }} />
            <input ref={photoRef} type="file" accept="image/*" capture="environment" style={{ display: "none" }} />
          </div>
        </div>
      )}
    </div>
  );
}
