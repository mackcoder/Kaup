import { useState, useRef } from "react";
import CategoryPage from "./CategoryPage";
import AllDocumentsPage from "./AllDocumentsPage";
import ContractChatPage, { type ChatDocProps } from "./ContractChatPage";
import ProfilePage from "./ProfilePage";

interface DocCard {
  emoji: string;
  label: string;
  count: number;
  color: string;
  bg: string;
}

const DOCS: DocCard[] = [
  { emoji: "🚗", label: "Carros", count: 2, color: "#d97706", bg: "#fef3c7" },
  { emoji: "🏠", label: "Casa", count: 1, color: "#0284c7", bg: "#e0f2fe" },
  { emoji: "💼", label: "Trabalho", count: 3, color: "#7c3aed", bg: "#ede9fe" },
  { emoji: "🏦", label: "Banco", count: 1, color: "#059669", bg: "#d1fae5" },
  { emoji: "📄", label: "Outros", count: 4, color: "#475569", bg: "#f1f5f9" },
];

// ── Navigation stack ──────────────────────────────────────────────────────────
type Screen =
  | { id: "home" }
  | { id: "profile" }
  | { id: "category"; catKey: string }
  | { id: "all"; filter?: "all" | "ok" | "warning" | "pending" }
  | { id: "chat"; doc: ChatDocProps };

const NEW_DOC: ChatDocProps = {
  emoji: "📄",
  name: "Novo Documento",
  description: "Documento enviado agora",
  catKey: "Outros",
  catLabel: "Outros",
  catEmoji: "📄",
  catColor: "#334155",
  catColorLight: "#475569",
  catColorBg: "#f1f5f9",
  status: "pending",
  isNew: true,
};

export default function App() {
  const [stack, setStack] = useState<Screen[]>([{ id: "home" }]);
  const push = (s: Screen) => setStack((p) => [...p, s]);
  const pop = () => setStack((p) => (p.length > 1 ? p.slice(0, -1) : p));
  const current = stack[stack.length - 1];

  const openDoc = (doc: ChatDocProps) => push({ id: "chat", doc });

  // Home-specific state
  const [tapped, setTapped] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const photoInputRef = useRef<HTMLInputElement>(null);
  const [showOptions, setShowOptions] = useState(false);

  const handleTap = (key: string, cb?: () => void) => {
    setTapped(key);
    setTimeout(() => { setTapped(null); cb?.(); }, 150);
  };

  const handleFileChange = (_e: React.ChangeEvent<HTMLInputElement>) => {
    setShowOptions(false);
    push({ id: "chat", doc: NEW_DOC });
    _e.target.value = "";
  };

  // ── Route rendering ─────────────────────────────────────────────────────────
  if (current.id === "chat") {
    return <ContractChatPage doc={current.doc} onBack={pop} />;
  }

  if (current.id === "profile") {
    return (
      <ProfilePage
        onBack={pop}
        onOpenAllDocs={(filter) => push({ id: "all", filter: filter ?? "all" })}
      />
    );
  }

  if (current.id === "all") {
    return <AllDocumentsPage onBack={pop} onOpenDoc={openDoc} initialFilter={(current as { id: "all"; filter?: "all" | "ok" | "warning" | "pending" }).filter} />;
  }

  if (current.id === "category") {
    return (
      <CategoryPage
        categoryKey={current.catKey}
        onBack={pop}
        onOpenDoc={openDoc}
      />
    );
  }

  // ── HOME SCREEN ─────────────────────────────────────────────────────────────
  return (
    <div
      style={{
        minHeight: "100%",
        background: "#f5f7fa",
        display: "flex",
        flexDirection: "column",
        maxWidth: 430,
        margin: "0 auto",
        paddingBottom: 32,
      paddingTop: "max(52px, env(safe-area-inset-top, 0px))",
      }}
    >

      {/* Header */}
      <div style={{ padding: "0 20px 20px", display: "flex", alignItems: "center", justifyContent: "space-between" }}>
        <div>
          <div style={{ fontSize: 22, fontWeight: 900, color: "#1e293b", lineHeight: 1.1 }}>
            Contrato Fácil
          </div>
          <div style={{ fontSize: 14, color: "#64748b", fontWeight: 600, marginTop: 2 }}>
            Seus documentos
          </div>
        </div>
        <button
          onClick={() => push({ id: "profile" })}
          style={{
            width: 46, height: 46, borderRadius: "50%",
            background: "#dbeafe", display: "flex", alignItems: "center",
            justifyContent: "center", fontSize: 22, cursor: "pointer",
            border: "2.5px solid #bfdbfe",
          }}
        >
          👤
        </button>
      </div>

      {/* Chatbot card */}
      <div style={{ padding: "0 20px 24px" }}>
        <div
          style={{
            background: "linear-gradient(145deg, #1d4ed8 0%, #2563eb 60%, #3b82f6 100%)",
            borderRadius: 28, padding: "28px 24px 24px",
            boxShadow: "0 8px 32px rgba(37,99,235,0.28)",
            position: "relative", overflow: "hidden",
          }}
        >
          <div style={{ position: "absolute", top: -30, right: -30, width: 120, height: 120, borderRadius: "50%", background: "rgba(255,255,255,0.07)" }} />
          <div style={{ position: "absolute", bottom: -20, right: 40, width: 80, height: 80, borderRadius: "50%", background: "rgba(255,255,255,0.05)" }} />

          <div style={{ display: "flex", alignItems: "center", gap: 14, marginBottom: 20 }}>
            <div
              style={{
                width: 56, height: 56, borderRadius: "50%",
                background: "rgba(255,255,255,0.18)", border: "2.5px solid rgba(255,255,255,0.35)",
                display: "flex", alignItems: "center", justifyContent: "center", fontSize: 26, flexShrink: 0,
              }}
            >
              🤖
            </div>
            <div>
              <div style={{ background: "rgba(255,255,255,0.15)", borderRadius: 12, padding: "4px 12px", display: "inline-block", marginBottom: 6 }}>
                <span style={{ fontSize: 11, color: "rgba(255,255,255,0.85)", fontWeight: 700, letterSpacing: "0.05em" }}>
                  ASSISTENTE IA
                </span>
              </div>
              <div style={{ fontSize: 20, fontWeight: 800, color: "#fff", lineHeight: 1.25 }}>Olá! 👋</div>
            </div>
          </div>

          <div style={{ background: "rgba(255,255,255,0.14)", borderRadius: 18, borderTopLeftRadius: 4, padding: "16px 18px", marginBottom: 22 }}>
            <p style={{ fontSize: 17, color: "#fff", fontWeight: 700, lineHeight: 1.5, margin: 0 }}>
              Envie um documento para&nbsp;começarmos.
            </p>
            <p style={{ fontSize: 14, color: "rgba(255,255,255,0.75)", fontWeight: 600, lineHeight: 1.5, margin: "8px 0 0" }}>
              Eu leio e explico tudo pra você, de um jeito fácil de entender. ✅
            </p>
          </div>

          <button
            onClick={() => handleTap("main", () => setShowOptions(true))}
            style={{
              width: "100%", padding: "18px 24px",
              background: tapped === "main" ? "#f0fdf4" : "#ffffff",
              border: "none", borderRadius: 20, display: "flex",
              alignItems: "center", justifyContent: "center", gap: 12,
              cursor: "pointer", boxShadow: "0 4px 16px rgba(0,0,0,0.15)",
              transform: tapped === "main" ? "scale(0.97)" : "scale(1)",
              transition: "all 0.15s ease",
            }}
          >
            <span style={{ fontSize: 26 }}>📤</span>
            <span style={{ fontSize: 18, fontWeight: 900, color: "#1d4ed8" }}>Enviar documento</span>
          </button>
        </div>
      </div>

      {/* Send options modal */}
      {showOptions && (
        <div
          onClick={() => setShowOptions(false)}
          style={{
            position: "fixed", inset: 0, background: "rgba(0,0,0,0.45)",
            zIndex: 50, display: "flex", alignItems: "flex-end", justifyContent: "center",
            maxWidth: 430, margin: "0 auto",
          }}
        >
          <div
            onClick={(e) => e.stopPropagation()}
            style={{
              background: "#fff", borderRadius: "28px 28px 0 0",
              padding: "10px 24px 40px", width: "100%",
              boxShadow: "0 -8px 40px rgba(0,0,0,0.15)",
            }}
          >
            <div style={{ width: 44, height: 5, background: "#e2e8f0", borderRadius: 10, margin: "10px auto 24px" }} />
            <div style={{ fontSize: 18, fontWeight: 900, color: "#1e293b", marginBottom: 6, textAlign: "center" }}>
              Como deseja enviar?
            </div>
            <div style={{ fontSize: 14, color: "#64748b", fontWeight: 600, textAlign: "center", marginBottom: 28 }}>
              Escolha uma opção abaixo
            </div>
            <div style={{ display: "flex", flexDirection: "column", gap: 14 }}>
              <button
                onClick={() => { photoInputRef.current?.click(); }}
                style={{ display: "flex", alignItems: "center", gap: 18, padding: "18px 20px", background: "#eff6ff", border: "2px solid #bfdbfe", borderRadius: 20, cursor: "pointer", width: "100%", textAlign: "left" }}
              >
                <span style={{ fontSize: 32 }}>📷</span>
                <div>
                  <div style={{ fontSize: 17, fontWeight: 800, color: "#1e293b" }}>Tirar uma foto</div>
                  <div style={{ fontSize: 13, color: "#64748b", fontWeight: 600, marginTop: 2 }}>Aponte a câmera para o documento</div>
                </div>
              </button>
              <button
                onClick={() => { fileInputRef.current?.click(); }}
                style={{ display: "flex", alignItems: "center", gap: 18, padding: "18px 20px", background: "#f0fdf4", border: "2px solid #bbf7d0", borderRadius: 20, cursor: "pointer", width: "100%", textAlign: "left" }}
              >
                <span style={{ fontSize: 32 }}>📁</span>
                <div>
                  <div style={{ fontSize: 17, fontWeight: 800, color: "#1e293b" }}>Escolher arquivo</div>
                  <div style={{ fontSize: 13, color: "#64748b", fontWeight: 600, marginTop: 2 }}>PDF, imagem ou documento</div>
                </div>
              </button>
            </div>
            <input ref={fileInputRef} type="file" accept=".pdf,.doc,.docx,.txt,.png,.jpg,.jpeg" onChange={handleFileChange} style={{ display: "none" }} />
            <input ref={photoInputRef} type="file" accept="image/*" capture="environment" onChange={handleFileChange} style={{ display: "none" }} />
          </div>
        </div>
      )}

      {/* Section label */}
      <div style={{ padding: "0 20px 14px", display: "flex", alignItems: "center", justifyContent: "space-between" }}>
        <div>
          <div style={{ fontSize: 19, fontWeight: 900, color: "#1e293b" }}>Meus documentos</div>
          <div style={{ fontSize: 13, color: "#94a3b8", fontWeight: 600, marginTop: 1 }}>
            Toque para ver o resultado
          </div>
        </div>
        <button
          onClick={() => push({ id: "all" })}
          style={{
            fontSize: 13, fontWeight: 800, color: "#2563eb",
            background: "#eff6ff", border: "none", borderRadius: 12,
            padding: "6px 14px", cursor: "pointer",
          }}
        >
          Ver todos
        </button>
      </div>

      {/* Doc grid */}
      <div style={{ padding: "0 20px", display: "grid", gridTemplateColumns: "1fr 1fr", gap: 14 }}>
        {DOCS.map((doc) => (
          <button
            key={doc.label}
            onClick={() => handleTap(doc.label, () => push({ id: "category", catKey: doc.label }))}
            style={{
              background: "#ffffff",
              border: `2.5px solid ${tapped === doc.label ? doc.color : "#e2e8f0"}`,
              borderRadius: 24, padding: "24px 16px 20px",
              display: "flex", flexDirection: "column", alignItems: "center", gap: 10,
              cursor: "pointer",
              boxShadow: tapped === doc.label ? `0 4px 20px ${doc.color}30` : "0 2px 10px rgba(0,0,0,0.06)",
              transform: tapped === doc.label ? "scale(0.96)" : "scale(1)",
              transition: "all 0.15s ease", position: "relative",
            }}
          >
            {doc.count > 0 && (
              <div
                style={{
                  position: "absolute", top: 14, right: 14, width: 22, height: 22,
                  borderRadius: "50%", background: doc.color,
                  display: "flex", alignItems: "center", justifyContent: "center",
                }}
              >
                <span style={{ fontSize: 11, fontWeight: 900, color: "#fff" }}>{doc.count}</span>
              </div>
            )}
            <div
              style={{
                width: 72, height: 72, borderRadius: 20, background: doc.bg,
                display: "flex", alignItems: "center", justifyContent: "center", fontSize: 36,
              }}
            >
              {doc.emoji}
            </div>
            <span style={{ fontSize: 16, fontWeight: 800, color: "#1e293b" }}>{doc.label}</span>
            <span style={{ fontSize: 12, fontWeight: 600, color: "#94a3b8" }}>
              {doc.count} {doc.count === 1 ? "documento" : "documentos"}
            </span>
          </button>
        ))}

        {/* Add new card */}
        <button
          onClick={() => handleTap("add", () => setShowOptions(true))}
          style={{
            background: "#f8fafc", border: "2.5px dashed #cbd5e1", borderRadius: 24,
            padding: "24px 16px 20px", display: "flex", flexDirection: "column",
            alignItems: "center", gap: 10, cursor: "pointer",
            transform: tapped === "add" ? "scale(0.96)" : "scale(1)",
            transition: "all 0.15s ease",
          }}
        >
          <div style={{ width: 72, height: 72, borderRadius: 20, background: "#e2e8f0", display: "flex", alignItems: "center", justifyContent: "center", fontSize: 32 }}>
            ➕
          </div>
          <span style={{ fontSize: 16, fontWeight: 800, color: "#94a3b8" }}>Novo tipo</span>
          <span style={{ fontSize: 12, fontWeight: 600, color: "#cbd5e1" }}>Adicionar</span>
        </button>
      </div>

      {/* Bottom tip */}
      <div style={{ padding: "28px 20px 0" }}>
        <div
          style={{
            background: "#fefce8", border: "2px solid #fde68a", borderRadius: 20,
            padding: "16px 18px", display: "flex", alignItems: "flex-start", gap: 12,
          }}
        >
          <span style={{ fontSize: 22, flexShrink: 0 }}>💡</span>
          <div>
            <div style={{ fontSize: 14, fontWeight: 800, color: "#92400e", marginBottom: 3 }}>Dica</div>
            <div style={{ fontSize: 13, fontWeight: 600, color: "#78350f", lineHeight: 1.5 }}>
              Nunca assine um contrato sem entender o que diz. Envie aqui antes!
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
