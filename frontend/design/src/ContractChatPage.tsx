import { useState, useEffect, useRef } from "react";

// ── Shared type exported for use in other pages ──────────────────────────────
export interface ChatDocProps {
  emoji: string;
  name: string;
  description: string;
  catKey: string;
  catLabel: string;
  catEmoji: string;
  catColor: string;
  catColorLight: string;
  catColorBg: string;
  status: "ok" | "warning" | "pending";
  isNew?: boolean;
}

// ── Message types ─────────────────────────────────────────────────────────────
type MsgRole = "ai" | "user";
type MsgKind = "text" | "summary-card" | "options" | "disclaimer";

interface ChatMessage {
  id: string;
  role: MsgRole;
  kind: MsgKind;
  text?: string;
  options?: string[];
}

// ── Static data ───────────────────────────────────────────────────────────────
const INCOME_OPTIONS = [
  "Menos de R$ 3.000",
  "Entre R$ 3.000 e R$ 5.000",
  "Entre R$ 5.000 e R$ 7.000",
  "Entre R$ 7.000 e R$ 15.000",
  "Entre R$ 15.000 e R$ 20.000",
  "Mais de R$ 20.000",
  "Outra opção",
];

const INCOME_RESPONSES: Record<string, string> = {
  "Menos de R$ 3.000":
    "⚠️ A parcela comprometeria mais de 40% da sua renda. Especialistas recomendam no máximo 30%. Isso pode ser arriscado. Avalie bem antes de assinar.",
  "Entre R$ 3.000 e R$ 5.000":
    "⚠️ A parcela pode comprometer entre 25% e 42% da sua renda. Isso pode ser elevado. Verifique se você tem outras despesas fixas antes de fechar o contrato.",
  "Entre R$ 5.000 e R$ 7.000":
    "✅ A parcela comprometeria entre 18% e 25% da sua renda. Está dentro do limite recomendado, mas verifique outras dívidas antes de assinar.",
  "Entre R$ 7.000 e R$ 15.000":
    "✅ A parcela comprometeria entre 8% e 18% da sua renda. Parece adequado. Compare com outras opções de financiamento antes de decidir.",
  "Entre R$ 15.000 e R$ 20.000":
    "✅ A parcela comprometeria menos de 8% da sua renda. O compromisso financeiro é relativamente baixo em relação ao que você ganha.",
  "Mais de R$ 20.000":
    "✅ A parcela comprometeria menos de 7% da sua renda. O impacto no seu orçamento seria pequeno. Ainda assim, leia todas as cláusulas.",
  "Outra opção":
    "Tudo bem! Independente da renda, o mais importante é verificar se as parcelas cabem no seu orçamento junto com todas as despesas fixas. Quer que eu explique alguma cláusula?",
};

const FREE_TEXT_RESPONSES: Array<{ keywords: string[]; response: string }> = [
  {
    keywords: ["multa", "cancelar", "rescisão", "sair", "desistir"],
    response:
      "A multa por cancelamento antecipado geralmente é de 10% a 20% do valor restante. Verifique a cláusula de rescisão para saber o valor exato antes de assinar.",
  },
  {
    keywords: ["juros", "taxa", "cet", "percentual", "custo"],
    response:
      "Os juros são o custo extra pelo dinheiro emprestado. Fique atento ao CET (Custo Efetivo Total), que inclui juros, taxas e seguros. Quanto menor, melhor para você.",
  },
  {
    keywords: ["prazo", "meses", "anos", "duração", "tempo", "parcelas"],
    response:
      "O prazo é de 48 meses (4 anos). Quanto mais longo o prazo, menor a parcela, mas você paga mais juros no total. Prazos curtos custam menos no geral.",
  },
  {
    keywords: ["assinar", "assinatura", "documento", "papel"],
    response:
      "Antes de assinar, certifique-se de ler todas as páginas. Peça para explicar qualquer trecho que não entender. Nunca assine em branco e guarde sempre uma cópia.",
  },
];

const DEFAULT_FREE_RESPONSE =
  "Posso explicar cláusulas, valores, prazos, multas e pontos de atenção deste contrato. Basta perguntar de forma simples, como \"O que acontece se eu atrasar?\" ou \"Qual é a multa se eu cancelar?\"";

function getFreeTextResponse(text: string): string {
  const lower = text.toLowerCase();
  for (const { keywords, response } of FREE_TEXT_RESPONSES) {
    if (keywords.some((k) => lower.includes(k))) return response;
  }
  return DEFAULT_FREE_RESPONSE;
}

function uid() {
  return Math.random().toString(36).slice(2);
}

// ── Context popup ─────────────────────────────────────────────────────────────
interface CtxFile {
  emoji: string;
  name: string;
  type: string;
  isMain?: boolean;
}

function getContextFiles(doc: ChatDocProps): CtxFile[] {
  const base: CtxFile = { emoji: doc.emoji, name: doc.name, type: "Principal", isMain: true };
  const extras: Record<string, CtxFile[]> = {
    Carros: [
      { emoji: "🚗", name: "Contrato de compra e venda", type: "Documento relacionado" },
      { emoji: "📑", name: "Comprovante de renda.pdf", type: "Documento de apoio" },
    ],
    Casa: [
      { emoji: "🏠", name: "Vistoria do imóvel.pdf", type: "Documento relacionado" },
      { emoji: "📑", name: "Comprovante de endereço.pdf", type: "Documento de apoio" },
    ],
    Trabalho: [
      { emoji: "💼", name: "Holerite — último mês", type: "Documento de apoio" },
      { emoji: "📑", name: "Carteira de trabalho.pdf", type: "Documento relacionado" },
    ],
    Banco: [
      { emoji: "📑", name: "Extrato bancário — 3 meses", type: "Documento de apoio" },
      { emoji: "🏦", name: "Proposta comercial.pdf", type: "Documento relacionado" },
    ],
    Outros: [{ emoji: "📄", name: "Documento adicional.pdf", type: "Documento relacionado" }],
  };
  return [base, ...(extras[doc.catKey] ?? extras["Outros"])];
}

// ── Loading screen ────────────────────────────────────────────────────────────
const LOAD_STEPS = [
  "Lendo o documento...",
  "Identificando cláusulas importantes...",
  "Verificando pontos de atenção...",
  "Análise pronta! ✅",
];

function SpinDot({ color }: { color: string }) {
  return (
    <div
      style={{
        width: 10,
        height: 10,
        borderRadius: "50%",
        border: `2.5px solid ${color}`,
        borderTopColor: "transparent",
        animation: "cdSpin 0.7s linear infinite",
      }}
    />
  );
}

function LoadingScreen({ doc }: { doc: ChatDocProps }) {
  const [step, setStep] = useState(0);
  useEffect(() => {
    const id = setInterval(() => setStep((s) => Math.min(s + 1, LOAD_STEPS.length - 1)), 750);
    return () => clearInterval(id);
  }, []);

  return (
    <div
      style={{
        flex: 1,
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        padding: "40px 32px",
        gap: 28,
      }}
    >
      <div
        style={{
          width: 100,
          height: 100,
          borderRadius: 28,
          background: doc.catColorBg,
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          fontSize: 48,
          animation: "cdPulse 1.4s ease-in-out infinite",
        }}
      >
        {doc.emoji}
      </div>
      <div style={{ textAlign: "center" }}>
        <div style={{ fontSize: 20, fontWeight: 900, color: "#1e293b", marginBottom: 6 }}>
          Analisando documento
        </div>
        <div style={{ fontSize: 15, fontWeight: 700, color: "#64748b" }}>{doc.name}</div>
      </div>
      <div style={{ display: "flex", flexDirection: "column", gap: 12, width: "100%" }}>
        {LOAD_STEPS.map((s, i) => {
          const done = i < step;
          const active = i === step;
          return (
            <div
              key={i}
              style={{
                display: "flex",
                alignItems: "center",
                gap: 12,
                opacity: i > step ? 0.3 : 1,
                transition: "opacity 0.4s",
              }}
            >
              <div
                style={{
                  width: 28,
                  height: 28,
                  borderRadius: "50%",
                  background: done ? "#059669" : active ? doc.catColorBg : "#f1f5f9",
                  border: `2px solid ${done ? "#059669" : active ? doc.catColorLight : "#e2e8f0"}`,
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  fontSize: 13,
                  flexShrink: 0,
                  transition: "all 0.4s",
                }}
              >
                {done ? "✓" : active ? <SpinDot color={doc.catColorLight} /> : null}
              </div>
              <span
                style={{
                  fontSize: 14,
                  fontWeight: active ? 800 : 600,
                  color: done ? "#059669" : active ? "#1e293b" : "#94a3b8",
                }}
              >
                {s}
              </span>
            </div>
          );
        })}
      </div>
      <style>{`
        @keyframes cdPulse {
          0%,100%{transform:scale(1);box-shadow:0 0 0 0 rgba(0,0,0,0.1);}
          50%{transform:scale(1.06);box-shadow:0 0 0 12px rgba(0,0,0,0.04);}
        }
        @keyframes cdSpin { to { transform:rotate(360deg); } }
        @keyframes cdBounce {
          0%,60%,100%{transform:translateY(0);opacity:0.4;}
          30%{transform:translateY(-6px);opacity:1;}
        }
      `}</style>
    </div>
  );
}

// ── Summary card ──────────────────────────────────────────────────────────────
function SummaryCard({ doc }: { doc: ChatDocProps }) {
  const isOk = doc.status === "ok";
  return (
    <div
      style={{
        background: "#fff",
        border: "1.5px solid #e2e8f0",
        borderRadius: 20,
        overflow: "hidden",
        boxShadow: "0 2px 10px rgba(0,0,0,0.06)",
      }}
    >
      <div
        style={{
          background: isOk
            ? "linear-gradient(135deg,#ecfdf5,#d1fae5)"
            : "linear-gradient(135deg,#fffbeb,#fef3c7)",
          borderBottom: `1.5px solid ${isOk ? "#a7f3d0" : "#fde68a"}`,
          padding: "12px 16px",
          display: "flex",
          alignItems: "center",
          gap: 10,
        }}
      >
        <span style={{ fontSize: 20 }}>{isOk ? "✅" : "⚠️"}</span>
        <div>
          <div
            style={{
              fontSize: 11,
              fontWeight: 800,
              letterSpacing: "0.06em",
              color: isOk ? "#065f46" : "#92400e",
            }}
          >
            ANÁLISE INICIAL
          </div>
          <div style={{ fontSize: 14, fontWeight: 800, color: isOk ? "#059669" : "#d97706" }}>
            {isOk ? "Documento OK" : "Atenção"}
          </div>
        </div>
      </div>
      {[
        { label: "Parcela mensal", value: "R$ 1.250,00" },
        { label: "Prazo", value: "48 meses" },
        { label: "Valor total estimado", value: "R$ 60.000,00" },
      ].map((row, i) => (
        <div
          key={i}
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            padding: "10px 16px",
            borderBottom: "1px solid #f8fafc",
          }}
        >
          <span style={{ fontSize: 13, fontWeight: 600, color: "#64748b" }}>{row.label}</span>
          <span style={{ fontSize: 14, fontWeight: 800, color: "#1e293b" }}>{row.value}</span>
        </div>
      ))}
      <div
        style={{
          padding: "10px 16px",
          background: "#fffbeb",
          display: "flex",
          gap: 8,
          alignItems: "flex-start",
        }}
      >
        <span style={{ fontSize: 16, flexShrink: 0 }}>💡</span>
        <span style={{ fontSize: 13, fontWeight: 600, color: "#78350f", lineHeight: 1.5 }}>
          Existem juros e encargos que merecem atenção.
        </span>
      </div>
    </div>
  );
}

// ── Context popup ─────────────────────────────────────────────────────────────
function ContextPopup({
  doc,
  files,
  onClose,
  onAddFile,
}: {
  doc: ChatDocProps;
  files: CtxFile[];
  onClose: () => void;
  onAddFile: () => void;
}) {
  return (
    <>
      <div
        onClick={onClose}
        style={{ position: "fixed", inset: 0, background: "rgba(0,0,0,0.35)", zIndex: 40 }}
      />
      <div
        style={{
          position: "fixed",
          top: 70,
          right: 16,
          width: 290,
          maxWidth: "calc(100vw - 32px)",
          background: "#fff",
          borderRadius: 20,
          boxShadow: "0 8px 32px rgba(0,0,0,0.18)",
          border: "1.5px solid #e2e8f0",
          zIndex: 41,
          overflow: "hidden",
        }}
      >
        <div
          style={{
            padding: "14px 16px 12px",
            borderBottom: "1.5px solid #f1f5f9",
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
          }}
        >
          <div>
            <div style={{ fontSize: 15, fontWeight: 900, color: "#1e293b" }}>
              Contexto da análise
            </div>
            <div style={{ fontSize: 12, color: "#94a3b8", fontWeight: 600, marginTop: 1 }}>
              {files.length} {files.length === 1 ? "arquivo" : "arquivos"} utilizados
            </div>
          </div>
          <button
            onClick={onClose}
            style={{
              width: 30,
              height: 30,
              borderRadius: 8,
              border: "1.5px solid #e2e8f0",
              background: "#f8fafc",
              cursor: "pointer",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              fontSize: 14,
              color: "#64748b",
            }}
          >
            ✕
          </button>
        </div>
        <div style={{ padding: "8px 0" }}>
          {files.map((f, i) => (
            <div
              key={i}
              style={{
                display: "flex",
                alignItems: "center",
                gap: 10,
                padding: "9px 16px",
                background: f.isMain ? doc.catColorBg + "80" : "transparent",
              }}
            >
              <div
                style={{
                  width: 36,
                  height: 36,
                  borderRadius: 10,
                  background: f.isMain ? doc.catColorBg : "#f1f5f9",
                  border: f.isMain
                    ? `1.5px solid ${doc.catColorLight}50`
                    : "1.5px solid #e2e8f0",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  fontSize: 18,
                  flexShrink: 0,
                }}
              >
                {f.emoji}
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div
                  style={{
                    fontSize: 13,
                    fontWeight: 800,
                    color: "#1e293b",
                    whiteSpace: "nowrap",
                    overflow: "hidden",
                    textOverflow: "ellipsis",
                  }}
                >
                  {f.name}
                </div>
                <div style={{ fontSize: 11, color: "#94a3b8", fontWeight: 600, marginTop: 1 }}>
                  {f.type}
                  {f.isMain && (
                    <span
                      style={{
                        marginLeft: 6,
                        background: doc.catColorBg,
                        color: doc.catColor,
                        borderRadius: 4,
                        padding: "1px 6px",
                        fontWeight: 800,
                        fontSize: 10,
                      }}
                    >
                      Principal
                    </span>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
        <div style={{ padding: "8px 12px 14px" }}>
          <button
            onClick={onAddFile}
            style={{
              width: "100%",
              padding: "11px 16px",
              background: doc.catColorBg,
              border: `1.5px dashed ${doc.catColorLight}`,
              borderRadius: 12,
              cursor: "pointer",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              gap: 8,
            }}
          >
            <span style={{ fontSize: 16 }}>➕</span>
            <span style={{ fontSize: 14, fontWeight: 800, color: doc.catColor }}>
              Adicionar arquivo ao contexto
            </span>
          </button>
        </div>
      </div>
    </>
  );
}

// ── Plus menu ─────────────────────────────────────────────────────────────────
function PlusMenu({
  onClose,
  catColor,
  catColorBg,
}: {
  onClose: () => void;
  catColor: string;
  catColorBg: string;
}) {
  const items = [
    { emoji: "📷", label: "Tirar foto", sub: "Aponte a câmera para o documento" },
    { emoji: "📁", label: "Enviar arquivo", sub: "PDF, imagem ou documento" },
    { emoji: "📎", label: "Adicionar ao contexto", sub: "Usar como referência na análise" },
  ];
  return (
    <>
      <div onClick={onClose} style={{ position: "fixed", inset: 0, zIndex: 30 }} />
      <div
        style={{
          position: "absolute",
          bottom: "100%",
          left: 0,
          marginBottom: 10,
          background: "#fff",
          borderRadius: 18,
          boxShadow: "0 8px 32px rgba(0,0,0,0.15)",
          border: "1.5px solid #e2e8f0",
          overflow: "hidden",
          zIndex: 31,
          width: 260,
        }}
      >
        {items.map((item, i) => (
          <button
            key={i}
            onClick={onClose}
            style={{
              display: "flex",
              alignItems: "center",
              gap: 12,
              padding: "13px 16px",
              width: "100%",
              background: "#fff",
              border: "none",
              borderBottom: i < items.length - 1 ? "1px solid #f1f5f9" : "none",
              cursor: "pointer",
              textAlign: "left",
            }}
          >
            <div
              style={{
                width: 38,
                height: 38,
                borderRadius: 11,
                background: catColorBg,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                fontSize: 20,
                flexShrink: 0,
              }}
            >
              {item.emoji}
            </div>
            <div>
              <div style={{ fontSize: 14, fontWeight: 800, color: "#1e293b" }}>{item.label}</div>
              <div style={{ fontSize: 12, color: "#94a3b8", fontWeight: 600, marginTop: 1 }}>
                {item.sub}
              </div>
            </div>
          </button>
        ))}
      </div>
    </>
  );
}

// ── Typing indicator ──────────────────────────────────────────────────────────
function TypingBubble({ color }: { color: string }) {
  return (
    <div
      style={{
        display: "flex",
        gap: 5,
        alignItems: "center",
        padding: "14px 16px",
        background: "#fff",
        border: "1.5px solid #e2e8f0",
        borderRadius: "6px 18px 18px 18px",
        width: "fit-content",
        boxShadow: "0 2px 8px rgba(0,0,0,0.05)",
      }}
    >
      {[0, 1, 2].map((i) => (
        <div
          key={i}
          style={{
            width: 8,
            height: 8,
            borderRadius: "50%",
            background: color,
            animation: `cdBounce 1.2s ease-in-out ${i * 0.2}s infinite`,
          }}
        />
      ))}
    </div>
  );
}

// ── Main component ────────────────────────────────────────────────────────────
interface Props {
  doc: ChatDocProps;
  onBack: () => void;
}

export default function ContractChatPage({ doc, onBack }: Props) {
  const [phase, setPhase] = useState<"loading" | "chatting">(doc.isNew ? "loading" : "chatting");
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [incomeSelected, setIncomeSelected] = useState<string | null>(null);
  const [incomeSent, setIncomeSent] = useState(false);
  const [inputText, setInputText] = useState("");
  const [isTyping, setIsTyping] = useState(false);
  const [showContext, setShowContext] = useState(false);
  const [showPlus, setShowPlus] = useState(false);
  const [contextFiles, setContextFiles] = useState<CtxFile[]>(() => getContextFiles(doc));

  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, isTyping]);

  // Transition from loading to chatting
  useEffect(() => {
    if (phase !== "loading") return;
    const t = setTimeout(() => setPhase("chatting"), 3200);
    return () => clearTimeout(t);
  }, [phase]);

  // Seed initial AI messages
  useEffect(() => {
    if (phase !== "chatting") return;
    const delay = doc.isNew ? 300 : 0;

    const addLater = (ms: number, msg: Omit<ChatMessage, "id">) =>
      setTimeout(() => setMessages((p) => [...p, { ...msg, id: uid() }]), delay + ms);

    const t1 = addLater(0, {
      role: "ai",
      kind: "text",
      text: `Analisei seu contrato. Encontrei alguns pontos importantes e preciso de uma informação para avaliar se as parcelas comprometem muito da sua renda.`,
    });
    const t2 = addLater(600, { role: "ai", kind: "summary-card" });
    const t3 = addLater(1000, { role: "ai", kind: "text", text: "Qual é a sua renda mensal aproximada?" });
    const t4 = addLater(1400, { role: "ai", kind: "options", options: INCOME_OPTIONS });

    return () => { clearTimeout(t1); clearTimeout(t2); clearTimeout(t3); clearTimeout(t4); };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [phase]);

  function addMsg(m: Omit<ChatMessage, "id">) {
    setMessages((p) => [...p, { ...m, id: uid() }]);
  }

  function handleSendIncome() {
    if (!incomeSelected) return;
    setIncomeSent(true);
    addMsg({ role: "user", kind: "text", text: incomeSelected });
    setIsTyping(true);
    setTimeout(() => {
      setIsTyping(false);
      addMsg({ role: "ai", kind: "text", text: INCOME_RESPONSES[incomeSelected] });
    }, 1800);
    setTimeout(() => {
      addMsg({
        role: "ai",
        kind: "disclaimer",
        text: "⚖️ Esta análise é informativa e não substitui orientação jurídica ou financeira profissional.",
      });
    }, 2600);
  }

  function handleSendText() {
    const text = inputText.trim();
    if (!text || isTyping) return;
    setInputText("");
    addMsg({ role: "user", kind: "text", text });
    setIsTyping(true);
    setTimeout(() => {
      setIsTyping(false);
      addMsg({ role: "ai", kind: "text", text: getFreeTextResponse(text) });
    }, 1600);
  }

  // ── RENDER ──────────────────────────────────────────────────────────────────
  return (
    <div
      style={{
        height: "100%",
        display: "flex",
        flexDirection: "column",
        background: "#f5f7fa",
        maxWidth: 430,
        margin: "0 auto",
        position: "relative",
        overflow: "hidden",
      }}
    >
      {/* ── KEYFRAMES ── */}
      <style>{`
        @keyframes cdPulse {
          0%,100%{transform:scale(1);}
          50%{transform:scale(1.06);}
        }
        @keyframes cdSpin{to{transform:rotate(360deg);}}
        @keyframes cdBounce{
          0%,60%,100%{transform:translateY(0);opacity:0.4;}
          30%{transform:translateY(-6px);opacity:1;}
        }
      `}</style>

      {/* ── HEADER ── */}
      <div
        style={{
          background: "#fff",
          borderBottom: "1.5px solid #e2e8f0",
          padding: "16px 16px 14px",
          paddingTop: "max(52px, env(safe-area-inset-top, 0px))",
          flexShrink: 0,
          zIndex: 20,
          position: "relative",
        }}
      >
        {/* Row 1: back + context */}
        <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 14 }}>
          <button
            onClick={onBack}
            style={{
              display: "flex",
              alignItems: "center",
              gap: 7,
              background: "#f1f5f9",
              border: "1.5px solid #e2e8f0",
              borderRadius: 13,
              padding: "9px 14px",
              cursor: "pointer",
              flexShrink: 0,
            }}
          >
            <span style={{ fontSize: 17, color: "#334155", lineHeight: 1 }}>←</span>
            <span style={{ fontSize: 14, fontWeight: 800, color: "#334155" }}>Voltar</span>
          </button>

          <div style={{ flex: 1 }} />

          <button
            onClick={() => setShowContext((v) => !v)}
            style={{
              display: "flex",
              alignItems: "center",
              gap: 7,
              background: showContext ? doc.catColorBg : "#f1f5f9",
              border: `1.5px solid ${showContext ? doc.catColorLight : "#e2e8f0"}`,
              borderRadius: 13,
              padding: "9px 14px",
              cursor: "pointer",
            }}
          >
            <span style={{ fontSize: 16 }}>📂</span>
            <span style={{ fontSize: 14, fontWeight: 800, color: showContext ? doc.catColor : "#334155" }}>
              Contexto
            </span>
            <span
              style={{
                fontSize: 11,
                fontWeight: 900,
                background: showContext ? doc.catColor : "#e2e8f0",
                color: showContext ? "#fff" : "#64748b",
                borderRadius: 6,
                padding: "1px 6px",
              }}
            >
              {contextFiles.length}
            </span>
          </button>
        </div>

        {/* Row 2: doc identity */}
        <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
          <div
            style={{
              width: 56,
              height: 56,
              borderRadius: 16,
              background: doc.catColorBg,
              border: `2px solid ${doc.catColorLight}60`,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              fontSize: 28,
              flexShrink: 0,
            }}
          >
            {doc.emoji}
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div
              style={{
                fontSize: 16,
                fontWeight: 900,
                color: "#1e293b",
                lineHeight: 1.25,
                whiteSpace: "nowrap",
                overflow: "hidden",
                textOverflow: "ellipsis",
              }}
            >
              {doc.name}
            </div>
            <div style={{ fontSize: 13, color: "#64748b", fontWeight: 600, marginTop: 2 }}>
              {doc.description}
            </div>
            <div style={{ marginTop: 4 }}>
              <span
                style={{
                  fontSize: 11,
                  fontWeight: 800,
                  color: doc.catColor,
                  background: doc.catColorBg,
                  borderRadius: 6,
                  padding: "2px 8px",
                }}
              >
                {doc.catEmoji} {doc.catLabel}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* ── CONTEXT POPUP ── */}
      {showContext && (
        <ContextPopup
          doc={doc}
          files={contextFiles}
          onClose={() => setShowContext(false)}
          onAddFile={() =>
            setContextFiles((p) => [
              ...p,
              { emoji: "📑", name: `Documento adicional ${p.length}.pdf`, type: "Adicionado agora" },
            ])
          }
        />
      )}

      {/* ── MAIN AREA ── */}
      {phase === "loading" ? (
        <LoadingScreen doc={doc} />
      ) : (
        <>
          {/* Chat scroll area */}
          <div
            style={{
              flex: 1,
              overflowY: "auto",
              padding: "16px 16px 12px",
              display: "flex",
              flexDirection: "column",
              gap: 10,
            }}
          >
            {/* AI identity row */}
            <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 4 }}>
              <div
                style={{
                  width: 32,
                  height: 32,
                  borderRadius: "50%",
                  background: doc.catColorBg,
                  border: `2px solid ${doc.catColorLight}60`,
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  fontSize: 16,
                  flexShrink: 0,
                }}
              >
                🤖
              </div>
              <span style={{ fontSize: 12, fontWeight: 800, color: doc.catColor }}>
                Assistente IA
              </span>
              <div
                style={{
                  width: 7,
                  height: 7,
                  borderRadius: "50%",
                  background: "#34d399",
                  boxShadow: "0 0 0 2px #d1fae5",
                }}
              />
            </div>

            {messages.map((msg) => {
              const isUser = msg.role === "user";

              if (msg.kind === "summary-card") {
                return (
                  <div key={msg.id} style={{ marginBottom: 4 }}>
                    <SummaryCard doc={doc} />
                  </div>
                );
              }

              if (msg.kind === "options" && !incomeSent) {
                return (
                  <div
                    key={msg.id}
                    style={{ display: "flex", flexDirection: "column", gap: 8, marginTop: 4 }}
                  >
                    {(msg.options ?? []).map((opt) => (
                      <button
                        key={opt}
                        onClick={() => setIncomeSelected(opt)}
                        style={{
                          padding: "14px 16px",
                          borderRadius: 16,
                          border: `2px solid ${incomeSelected === opt ? doc.catColorLight : "#e2e8f0"}`,
                          background: incomeSelected === opt ? doc.catColorBg : "#fff",
                          cursor: "pointer",
                          textAlign: "left",
                          display: "flex",
                          alignItems: "center",
                          gap: 10,
                          boxShadow:
                            incomeSelected === opt
                              ? `0 2px 12px ${doc.catColorLight}30`
                              : "0 1px 4px rgba(0,0,0,0.04)",
                          transition: "all 0.15s",
                          width: "100%",
                        }}
                      >
                        <div
                          style={{
                            width: 22,
                            height: 22,
                            borderRadius: "50%",
                            border: `2.5px solid ${incomeSelected === opt ? doc.catColorLight : "#cbd5e1"}`,
                            background: incomeSelected === opt ? doc.catColorLight : "#fff",
                            display: "flex",
                            alignItems: "center",
                            justifyContent: "center",
                            flexShrink: 0,
                            transition: "all 0.15s",
                          }}
                        >
                          {incomeSelected === opt && (
                            <div
                              style={{ width: 8, height: 8, borderRadius: "50%", background: "#fff" }}
                            />
                          )}
                        </div>
                        <span
                          style={{
                            fontSize: 15,
                            fontWeight: incomeSelected === opt ? 800 : 700,
                            color: incomeSelected === opt ? doc.catColor : "#334155",
                          }}
                        >
                          {opt}
                        </span>
                      </button>
                    ))}

                    {incomeSelected && (
                      <button
                        onClick={handleSendIncome}
                        style={{
                          marginTop: 4,
                          padding: "16px 20px",
                          borderRadius: 18,
                          border: "none",
                          background: `linear-gradient(135deg,${doc.catColor},${doc.catColorLight})`,
                          color: "#fff",
                          fontSize: 16,
                          fontWeight: 900,
                          cursor: "pointer",
                          boxShadow: `0 4px 16px ${doc.catColorLight}50`,
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          gap: 10,
                        }}
                      >
                        <span>📤</span>
                        <span>Enviar resposta</span>
                      </button>
                    )}
                  </div>
                );
              }

              if (msg.kind === "disclaimer") {
                return (
                  <div
                    key={msg.id}
                    style={{
                      padding: "10px 14px",
                      background: "#fefce8",
                      border: "1.5px solid #fde68a",
                      borderRadius: 14,
                      fontSize: 12,
                      fontWeight: 600,
                      color: "#78350f",
                      lineHeight: 1.5,
                    }}
                  >
                    {msg.text}
                  </div>
                );
              }

              // text bubbles
              if (msg.kind === "options") return null; // already sent, hide options

              return (
                <div
                  key={msg.id}
                  style={{ display: "flex", justifyContent: isUser ? "flex-end" : "flex-start" }}
                >
                  <div
                    style={{
                      maxWidth: "80%",
                      padding: "12px 15px",
                      borderRadius: isUser
                        ? "18px 18px 5px 18px"
                        : "5px 18px 18px 18px",
                      background: isUser
                        ? `linear-gradient(135deg,${doc.catColor},${doc.catColorLight})`
                        : "#fff",
                      border: isUser ? "none" : "1.5px solid #e2e8f0",
                      boxShadow: isUser
                        ? `0 2px 10px ${doc.catColorLight}40`
                        : "0 2px 8px rgba(0,0,0,0.05)",
                    }}
                  >
                    <span
                      style={{
                        fontSize: 15,
                        fontWeight: 600,
                        color: isUser ? "#fff" : "#1e293b",
                        lineHeight: 1.55,
                      }}
                    >
                      {msg.text}
                    </span>
                  </div>
                </div>
              );
            })}

            {isTyping && <TypingBubble color={doc.catColorLight} />}
            <div ref={bottomRef} style={{ height: 8 }} />
          </div>

          {/* ── BOTTOM INPUT BAR ── */}
          <div
            style={{
              background: "#fff",
              borderTop: "1.5px solid #e2e8f0",
              padding: "10px 12px",
              paddingBottom: "max(24px, env(safe-area-inset-bottom, 0px))",
              flexShrink: 0,
              position: "relative",
            }}
          >
            <div
              style={{
                display: "flex",
                alignItems: "center",
                gap: 8,
                background: "#f8fafc",
                border: "1.5px solid #e2e8f0",
                borderRadius: 24,
                padding: "6px 6px 6px 8px",
              }}
            >
              {/* Plus button */}
              <div style={{ position: "relative", flexShrink: 0 }}>
                <button
                  onClick={() => setShowPlus((v) => !v)}
                  style={{
                    width: 40,
                    height: 40,
                    borderRadius: "50%",
                    border: `1.5px solid ${showPlus ? doc.catColorLight : "#e2e8f0"}`,
                    background: showPlus ? doc.catColorBg : "#fff",
                    color: showPlus ? doc.catColor : "#64748b",
                    cursor: "pointer",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    fontSize: 20,
                    fontWeight: 700,
                    lineHeight: 1,
                    transition: "all 0.15s",
                    fontFamily: "inherit",
                  }}
                >
                  {showPlus ? "✕" : "+"}
                </button>
                {showPlus && (
                  <PlusMenu
                    onClose={() => setShowPlus(false)}
                    catColor={doc.catColor}
                    catColorBg={doc.catColorBg}
                  />
                )}
              </div>

              {/* Text input */}
              <input
                type="text"
                value={inputText}
                onChange={(e) => setInputText(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && handleSendText()}
                placeholder={
                  incomeSent
                    ? "Pergunte sobre este contrato..."
                    : "Responda a pergunta acima primeiro..."
                }
                disabled={!incomeSent && messages.some((m) => m.kind === "options")}
                style={{
                  flex: 1,
                  background: "transparent",
                  border: "none",
                  outline: "none",
                  fontSize: 14,
                  fontWeight: 600,
                  color: "#1e293b",
                  fontFamily: "inherit",
                  caretColor: doc.catColorLight,
                }}
              />

              {/* Send button */}
              <button
                onClick={handleSendText}
                disabled={!inputText.trim() || isTyping}
                style={{
                  width: 40,
                  height: 40,
                  borderRadius: "50%",
                  border: "none",
                  background:
                    inputText.trim() && !isTyping
                      ? `linear-gradient(135deg,${doc.catColor},${doc.catColorLight})`
                      : "#e2e8f0",
                  color: inputText.trim() && !isTyping ? "#fff" : "#94a3b8",
                  cursor: inputText.trim() && !isTyping ? "pointer" : "default",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center",
                  fontSize: 17,
                  transition: "all 0.2s",
                  flexShrink: 0,
                  fontFamily: "inherit",
                }}
              >
                ↑
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
