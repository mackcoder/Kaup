import { useState } from "react"

interface Props {
  onBack: () => void
  onOpenAllDocs: (filter?: "all" | "ok" | "warning" | "pending") => void
}

type FontSize = "small" | "medium" | "large"
type ExplainLevel = "simple" | "normal" | "complete"
type IncomeRange = "less3" | "3to5" | "5to7" | "7to15" | "more15" | "prefer_not"

const FONT_SCALE: Record<FontSize, number> = {
  small: 0.88,
  medium: 1,
  large: 1.14,
}

export default function ProfilePage({ onBack, onOpenAllDocs }: Props) {
  // User data
  const [name, setName] = useState("João da Silva")
  const [email, setEmail] = useState("joao@email.com")
  const [editName, setEditName] = useState(name)
  const [editEmail, setEditEmail] = useState(email)
  const [showEdit, setShowEdit] = useState(false)
  const [savedMsg, setSavedMsg] = useState(false)

  // Audio simulation
  const [audioKey, setAudioKey] = useState<string | null>(null)

  // Preferences
  const [explainLevel, setExplainLevel] = useState<ExplainLevel>("simple")
  const [fontSize, setFontSize] = useState<FontSize>("large")
  const [income, setIncome] = useState<IncomeRange>("prefer_not")

  // Modals
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [deleteSimulated, setDeleteSimulated] = useState(false)
  const [showLogoutConfirm, setShowLogoutConfirm] = useState(false)
  const [loggedOut, setLoggedOut] = useState(false)
  const [helpMsg, setHelpMsg] = useState<string | null>(null)

  // Tap feedback
  const [tapped, setTapped] = useState<string | null>(null)

  const scale = FONT_SCALE[fontSize]
  const fs = (base: number) => base * scale

  function tap(key: string, cb?: () => void) {
    setTapped(key)
    setTimeout(() => {
      setTapped(null)
      cb?.()
    }, 140)
  }

  function playAudio(key: string) {
    setAudioKey(key)
    setTimeout(() => setAudioKey(null), 2000)
  }

  function handleSave() {
    setName(editName.trim() || name)
    setEmail(editEmail.trim() || email)
    setShowEdit(false)
    setSavedMsg(true)
    setTimeout(() => setSavedMsg(false), 2500)
  }

  const docStats = [
    {
      key: "ok" as const,
      emoji: "✅",
      label: "Aprovados",
      sub: "12 documentos",
      color: "#059669",
      bg: "#d1fae5",
      border: "#a7f3d0",
    },
    {
      key: "warning" as const,
      emoji: "⚠️",
      label: "Precisam de atenção",
      sub: "4 documentos",
      color: "#d97706",
      bg: "#fef3c7",
      border: "#fde68a",
    },
    {
      key: "pending" as const,
      emoji: "🕐",
      label: "Esperando análise",
      sub: "3 documentos",
      color: "#6366f1",
      bg: "#eef2ff",
      border: "#c7d2fe",
    },
  ]

  const explainOptions: {
    key: ExplainLevel
    emoji: string
    title: string
    sub: string
  }[] = [
    {
      key: "simple",
      emoji: "😊",
      title: "Bem simples",
      sub: "Frases pequenas e palavras fáceis.",
    },
    {
      key: "normal",
      emoji: "📘",
      title: "Normal",
      sub: "Explicações com um pouco mais de detalhes.",
    },
    {
      key: "complete",
      emoji: "🔎",
      title: "Completa",
      sub: "Mostra todos os detalhes do contrato.",
    },
  ]

  const incomeOptions: { key: IncomeRange label: string }[] = [
    { key: "less3", label: "Menos de R$ 3.000" },
    { key: "3to5", label: "R$ 3.000 a R$ 5.000" },
    { key: "5to7", label: "R$ 5.000 a R$ 7.000" },
    { key: "7to15", label: "R$ 7.000 a R$ 15.000" },
    { key: "more15", label: "Mais de R$ 15.000" },
    { key: "prefer_not", label: "Prefiro não informar" },
  ]

  const privacyItems = [
    {
      key: "docs",
      emoji: "📂",
      title: "Cuidar dos meus documentos",
      sub: "Veja ou apague arquivos enviados.",
    },
    {
      key: "history",
      emoji: "🕘",
      title: "Ver minhas análises",
      sub: "Veja contratos analisados antes.",
    },
    {
      key: "download",
      emoji: "⬇️",
      title: "Baixar minhas informações",
      sub: "Guarde uma cópia dos seus dados.",
    },
    {
      key: "delete",
      emoji: "🗑️",
      title: "Apagar minha conta",
      sub: "Apague seus dados do aplicativo.",
      danger: true,
    },
  ]

  const helpItems = [
    {
      key: "video",
      emoji: "▶️",
      title: "Ver como usar",
      sub: "Assista a uma explicação simples.",
      msg: "▶️ Abrindo vídeo explicativo...",
    },
    {
      key: "audio",
      emoji: "🎧",
      title: "Ouvir ajuda",
      sub: "Escute como o aplicativo funciona.",
      msg: "🎧 Preparando o áudio de ajuda...",
    },
    {
      key: "person",
      emoji: "👥",
      title: "Falar com alguém",
      sub: "Peça ajuda para uma pessoa.",
      msg: "👥 Conectando com a equipe de suporte...",
    },
  ]

  const s = {
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
  }

  if (loggedOut) {
    return (
      <div
        style={{
          ...s.page,
          alignItems: "center",
          justifyContent: "center",
          gap: 24,
        }}
      >
        <span style={{ fontSize: 72 }}>👋</span>
        <div
          style={{
            fontSize: fs(22),
            fontWeight: 900,
            color: "#1e293b",
            textAlign: "center",
          }}
        >
          Até logo!
        </div>
        <div
          style={{
            fontSize: fs(15),
            color: "#64748b",
            fontWeight: 600,
            textAlign: "center",
            padding: "0 32px",
          }}
        >
          Você saiu da conta. Volte quando quiser.
        </div>
        <button
          onClick={() => {
            setLoggedOut(false)
            onBack()
          }}
          style={{
            background: "#2563eb",
            border: "none",
            borderRadius: 18,
            padding: "16px 40px",
            fontSize: fs(16),
            fontWeight: 900,
            color: "#fff",
            cursor: "pointer",
            minHeight: 56,
          }}
        >
          🏠 Ir para o início
        </button>
      </div>
    )
  }

  return (
    <div style={s.page}>
      {/* ── HEADER ── */}
      <div style={s.header}>
        <div style={{ display: "flex", alignItems: "center", gap: 14 }}>
          <button
            onClick={() => tap("back", onBack)}
            style={{
              ...s.backBtn,
              transform: tapped === "back" ? "scale(0.96)" : "scale(1)",
            }}
          >
            <span style={{ fontSize: fs(18), color: "#334155", lineHeight: 1 }}>
              ←
            </span>
            <span
              style={{ fontSize: fs(15), fontWeight: 900, color: "#334155" }}
            >
              Voltar
            </span>
          </button>
          <div>
            <div
              style={{
                fontSize: fs(19),
                fontWeight: 900,
                color: "#1e293b",
                lineHeight: 1.1,
              }}
            >
              Meu perfil
            </div>
            <div
              style={{ fontSize: fs(12), color: "#64748b", fontWeight: 600 }}
            >
              Suas informações e escolhas
            </div>
          </div>
        </div>
      </div>

      {/* ── USER CARD ── */}
      <div style={{ padding: "20px 18px 0" }}>
        <div
          style={{
            ...s.card,
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            gap: 10,
            textAlign: "center",
          }}
        >
          <div
            style={{
              width: 80,
              height: 80,
              borderRadius: "50%",
              background: "#dbeafe",
              border: "3px solid #bfdbfe",
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              fontSize: 40,
            }}
          >
            👤
          </div>
          <div>
            <div
              style={{ fontSize: fs(20), fontWeight: 900, color: "#1e293b" }}
            >
              {name}
            </div>
            <div
              style={{
                fontSize: fs(14),
                color: "#64748b",
                fontWeight: 600,
                marginTop: 2,
              }}
            >
              {email}
            </div>
          </div>

          {savedMsg && (
            <div
              style={{
                background: "#d1fae5",
                border: "1.5px solid #a7f3d0",
                borderRadius: 12,
                padding: "10px 18px",
                fontSize: fs(15),
                fontWeight: 800,
                color: "#065f46",
              }}
            >
              ✅ Dados salvos!
            </div>
          )}

          <div
            style={{ display: "flex", gap: 10, width: "100%", marginTop: 4 }}
          >
            <button
              onClick={() => {
                setEditName(name)
                setEditEmail(email)
                setShowEdit(true)
              }}
              style={{
                flex: 1,
                background: "#2563eb",
                border: "none",
                borderRadius: 16,
                padding: "14px 16px",
                fontSize: fs(15),
                fontWeight: 900,
                color: "#fff",
                cursor: "pointer",
                minHeight: 52,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                gap: 8,
              }}
            >
              ✏️ Mudar meus dados
            </button>
            <button
              onClick={() => playAudio("user-card")}
              style={s.audioBtn("user-card")}
            >
              {audioKey === "user-card" ? "🔊 Lendo..." : "🔊 Ouvir"}
            </button>
          </div>
        </div>
      </div>

      {/* ── EDIT MODAL ── */}
      {showEdit && (
        <>
          <div
            onClick={() => setShowEdit(false)}
            style={{
              position: "fixed",
              inset: 0,
              background: "rgba(0,0,0,0.4)",
              zIndex: 50,
            }}
          />
          <div
            style={{
              position: "fixed",
              bottom: 0,
              left: "50%",
              transform: "translateX(-50%)",
              width: "100%",
              maxWidth: 430,
              background: "#fff",
              borderRadius: "28px 28px 0 0",
              padding: "10px 22px 44px",
              zIndex: 51,
              boxShadow: "0 -8px 40px rgba(0,0,0,0.15)",
            }}
          >
            <div
              style={{
                width: 44,
                height: 5,
                background: "#e2e8f0",
                borderRadius: 10,
                margin: "10px auto 22px",
              }}
            />
            <div
              style={{
                fontSize: fs(18),
                fontWeight: 900,
                color: "#1e293b",
                marginBottom: 20,
                textAlign: "center",
              }}
            >
              ✏️ Mudar meus dados
            </div>
            <div style={{ marginBottom: 14 }}>
              <label
                style={{
                  fontSize: fs(13),
                  fontWeight: 800,
                  color: "#475569",
                  display: "block",
                  marginBottom: 6,
                }}
              >
                Seu nome
              </label>
              <input
                value={editName}
                onChange={(e) => setEditName(e.target.value)}
                style={{
                  width: "100%",
                  border: "2px solid #e2e8f0",
                  borderRadius: 14,
                  padding: "14px 16px",
                  fontSize: fs(15),
                  fontWeight: 700,
                  color: "#1e293b",
                  outline: "none",
                  background: "#f8fafc",
                  boxSizing: "border-box" as const,
                }}
              />
            </div>
            <div style={{ marginBottom: 22 }}>
              <label
                style={{
                  fontSize: fs(13),
                  fontWeight: 800,
                  color: "#475569",
                  display: "block",
                  marginBottom: 6,
                }}
              >
                Seu e-mail
              </label>
              <input
                value={editEmail}
                onChange={(e) => setEditEmail(e.target.value)}
                style={{
                  width: "100%",
                  border: "2px solid #e2e8f0",
                  borderRadius: 14,
                  padding: "14px 16px",
                  fontSize: fs(15),
                  fontWeight: 700,
                  color: "#1e293b",
                  outline: "none",
                  background: "#f8fafc",
                  boxSizing: "border-box" as const,
                }}
              />
            </div>
            <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
              <button
                onClick={handleSave}
                style={{
                  background: "#2563eb",
                  border: "none",
                  borderRadius: 18,
                  padding: "16px",
                  fontSize: fs(16),
                  fontWeight: 900,
                  color: "#fff",
                  cursor: "pointer",
                  minHeight: 56,
                }}
              >
                ✅ Salvar
              </button>
              <button
                onClick={() => setShowEdit(false)}
                style={{
                  background: "#f1f5f9",
                  border: "1.5px solid #e2e8f0",
                  borderRadius: 18,
                  padding: "14px",
                  fontSize: fs(15),
                  fontWeight: 800,
                  color: "#475569",
                  cursor: "pointer",
                  minHeight: 52,
                }}
              >
                Cancelar
              </button>
            </div>
          </div>
        </>
      )}

      {/* ── DOC SUMMARY ── */}
      <div style={s.section}>
        <div style={s.sectionTitle}>📄 Meus documentos</div>
        <div style={s.sectionSub}>Veja como estão seus contratos.</div>
        <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
          {docStats.map((d) => (
            <button
              key={d.key}
              onClick={() => tap(d.key, () => onOpenAllDocs(d.key))}
              style={{
                display: "flex",
                alignItems: "center",
                gap: 14,
                padding: "16px 18px",
                minHeight: 64,
                background: d.bg,
                border: `2px solid ${d.border}`,
                borderRadius: 18,
                cursor: "pointer",
                width: "100%",
                textAlign: "left",
                transform: tapped === d.key ? "scale(0.97)" : "scale(1)",
                transition: "all 0.14s",
              }}
            >
              <span style={{ fontSize: 32, flexShrink: 0 }}>{d.emoji}</span>
              <div style={{ flex: 1 }}>
                <div
                  style={{ fontSize: fs(16), fontWeight: 900, color: d.color }}
                >
                  {d.label}
                </div>
                <div
                  style={{
                    fontSize: fs(22),
                    fontWeight: 900,
                    color: "#1e293b",
                    lineHeight: 1.1,
                  }}
                >
                  {d.sub}
                </div>
              </div>
              <span style={{ fontSize: fs(22), color: d.color, flexShrink: 0 }}>
                ›
              </span>
            </button>
          ))}
        </div>
        <button
          onClick={() => tap("all-docs", () => onOpenAllDocs("all"))}
          style={{
            marginTop: 14,
            width: "100%",
            background: "#fff",
            border: "2px solid #bfdbfe",
            borderRadius: 18,
            padding: "16px",
            fontSize: fs(16),
            fontWeight: 900,
            color: "#2563eb",
            cursor: "pointer",
            minHeight: 56,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            gap: 10,
            transform: tapped === "all-docs" ? "scale(0.97)" : "scale(1)",
            transition: "all 0.14s",
          }}
        >
          📂 Ver todos os documentos
        </button>
      </div>

      {/* ── EXPLAIN PREFERENCE ── */}
      <div style={s.section}>
        <div
          style={{
            display: "flex",
            alignItems: "center",
            justifyContent: "space-between",
            marginBottom: 4,
          }}
        >
          <div style={s.sectionTitle}>🤖 Como você quer as explicações?</div>
        </div>
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 8,
            marginBottom: 14,
          }}
        >
          <button
            onClick={() => playAudio("explain")}
            style={s.audioBtn("explain")}
          >
            {audioKey === "explain" ? "🔊 Lendo..." : "🔊 Ouvir esta pergunta"}
          </button>
        </div>
        <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
          {explainOptions.map((opt) => {
            const active = explainLevel === opt.key
            return (
              <button
                key={opt.key}
                onClick={() =>
                  tap(`exp-${opt.key}`, () => setExplainLevel(opt.key))
                }
                style={{
                  ...s.bigBtn(active, "#2563eb"),
                  transform:
                    tapped === `exp-${opt.key}` ? "scale(0.97)" : "scale(1)",
                }}
              >
                <span style={{ fontSize: 30, flexShrink: 0 }}>{opt.emoji}</span>
                <div style={{ flex: 1 }}>
                  <div
                    style={{
                      fontSize: fs(16),
                      fontWeight: 900,
                      color: active ? "#1d4ed8" : "#1e293b",
                    }}
                  >
                    {opt.title}
                  </div>
                  <div
                    style={{
                      fontSize: fs(12),
                      fontWeight: 600,
                      color: "#64748b",
                      marginTop: 2,
                    }}
                  >
                    {opt.sub}
                  </div>
                </div>
                {active && (
                  <span
                    style={{
                      fontSize: fs(12),
                      fontWeight: 900,
                      background: "#2563eb",
                      color: "#fff",
                      borderRadius: 8,
                      padding: "3px 10px",
                      flexShrink: 0,
                    }}
                  >
                    ✓ Escolhido
                  </span>
                )}
              </button>
            )
          })}
        </div>
      </div>

      {/* ── FONT SIZE ── */}
      <div style={s.section}>
        <div style={s.sectionTitle}>🔠 Tamanho das letras</div>
        <div style={{ display: "flex", gap: 10, marginTop: 2 }}>
          {(["small", "medium", "large"] as FontSize[]).map((sz, i) => {
            const labels = ["A Pequena", "A Média", "A Grande"]
            const fsz = [16, 20, 26]
            const active = fontSize === sz
            return (
              <button
                key={sz}
                onClick={() => tap(`fs-${sz}`, () => setFontSize(sz))}
                style={{
                  flex: 1,
                  minHeight: 64,
                  background: active ? "#eff6ff" : "#fff",
                  border: `2px solid ${active ? "#2563eb" : "#e2e8f0"}`,
                  borderRadius: 18,
                  cursor: "pointer",
                  display: "flex",
                  flexDirection: "column",
                  alignItems: "center",
                  justifyContent: "center",
                  gap: 4,
                  transform: tapped === `fs-${sz}` ? "scale(0.95)" : "scale(1)",
                  transition: "all 0.14s",
                }}
              >
                <span
                  style={{
                    fontSize: fsz[i],
                    fontWeight: 900,
                    color: active ? "#2563eb" : "#1e293b",
                    lineHeight: 1,
                  }}
                >
                  A
                </span>
                <span
                  style={{
                    fontSize: fs(11),
                    fontWeight: 800,
                    color: active ? "#2563eb" : "#64748b",
                  }}
                >
                  {labels[i]}
                </span>
                {active && (
                  <span
                    style={{
                      fontSize: 10,
                      fontWeight: 900,
                      background: "#2563eb",
                      color: "#fff",
                      borderRadius: 6,
                      padding: "2px 8px",
                    }}
                  >
                    ✓
                  </span>
                )}
              </button>
            )
          })}
        </div>
      </div>

      {/* ── INCOME ── */}
      <div style={s.section}>
        <div style={s.sectionTitle}>💰 Minha faixa de renda</div>
        <div style={s.sectionSub}>
          Isso ajuda o aplicativo a analisar parcelas. Você não precisa
          responder.
        </div>
        <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
          {incomeOptions.map((opt) => {
            const active = income === opt.key
            return (
              <button
                key={opt.key}
                onClick={() => tap(`inc-${opt.key}`, () => setIncome(opt.key))}
                style={{
                  display: "flex",
                  alignItems: "center",
                  gap: 12,
                  padding: "14px 16px",
                  minHeight: 56,
                  background: active ? "#eff6ff" : "#fff",
                  border: `2px solid ${active ? "#2563eb" : "#e2e8f0"}`,
                  borderRadius: 16,
                  cursor: "pointer",
                  width: "100%",
                  textAlign: "left",
                  transform:
                    tapped === `inc-${opt.key}` ? "scale(0.97)" : "scale(1)",
                  transition: "all 0.14s",
                }}
              >
                <div
                  style={{
                    width: 24,
                    height: 24,
                    borderRadius: "50%",
                    border: `2.5px solid ${active ? "#2563eb" : "#cbd5e1"}`,
                    background: active ? "#2563eb" : "#fff",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "center",
                    flexShrink: 0,
                  }}
                >
                  {active && (
                    <div
                      style={{
                        width: 8,
                        height: 8,
                        borderRadius: "50%",
                        background: "#fff",
                      }}
                    />
                  )}
                </div>
                <span
                  style={{
                    fontSize: fs(15),
                    fontWeight: active ? 800 : 700,
                    color: active ? "#1d4ed8" : "#334155",
                    flex: 1,
                  }}
                >
                  {opt.label}
                </span>
                {active && (
                  <span style={{ fontSize: fs(18), flexShrink: 0 }}>✓</span>
                )}
              </button>
            )
          })}
        </div>
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 8,
            background: "#f8fafc",
            border: "1.5px solid #e2e8f0",
            borderRadius: 14,
            padding: "12px 16px",
            marginTop: 14,
          }}
        >
          <span style={{ fontSize: 18 }}>🔒</span>
          <span style={{ fontSize: fs(13), fontWeight: 700, color: "#475569" }}>
            Esta informação é privada.
          </span>
        </div>
      </div>

      {/* ── PRIVACY ── */}
      <div style={s.section}>
        <div style={s.sectionTitle}>🔒 Minha privacidade</div>
        <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
          {privacyItems.map((item) => (
            <button
              key={item.key}
              onClick={() => {
                if (item.key === "delete") {
                  tap(item.key, () => setShowDeleteConfirm(true))
                } else {
                  tap(item.key)
                }
              }}
              style={{
                ...s.bigBtn(false),
                transform: tapped === item.key ? "scale(0.97)" : "scale(1)",
                borderColor: (item as { danger?: boolean }).danger
                  ? "#fecaca"
                  : "#e2e8f0",
                background: (item as { danger?: boolean }).danger
                  ? "#fff5f5"
                  : "#fff",
              }}
            >
              <span style={{ fontSize: 28, flexShrink: 0 }}>{item.emoji}</span>
              <div style={{ flex: 1 }}>
                <div
                  style={{
                    fontSize: fs(15),
                    fontWeight: 900,
                    color: (item as { danger?: boolean }).danger
                      ? "#dc2626"
                      : "#1e293b",
                  }}
                >
                  {item.title}
                </div>
                <div
                  style={{
                    fontSize: fs(12),
                    fontWeight: 600,
                    color: "#64748b",
                    marginTop: 2,
                  }}
                >
                  {item.sub}
                </div>
              </div>
              <span
                style={{ fontSize: fs(20), color: "#cbd5e1", flexShrink: 0 }}
              >
                ›
              </span>
            </button>
          ))}
        </div>
      </div>

      {/* ── HELP ── */}
      <div style={s.section}>
        <div style={s.sectionTitle}>💬 Precisa de ajuda?</div>
        <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
          {helpItems.map((item) => (
            <button
              key={item.key}
              onClick={() =>
                tap(`help-${item.key}`, () => setHelpMsg(item.msg))
              }
              style={{
                ...s.bigBtn(false),
                transform:
                  tapped === `help-${item.key}` ? "scale(0.97)" : "scale(1)",
              }}
            >
              <span style={{ fontSize: 28, flexShrink: 0 }}>{item.emoji}</span>
              <div style={{ flex: 1 }}>
                <div
                  style={{
                    fontSize: fs(15),
                    fontWeight: 900,
                    color: "#1e293b",
                  }}
                >
                  {item.title}
                </div>
                <div
                  style={{
                    fontSize: fs(12),
                    fontWeight: 600,
                    color: "#64748b",
                    marginTop: 2,
                  }}
                >
                  {item.sub}
                </div>
              </div>
              <span
                style={{ fontSize: fs(20), color: "#cbd5e1", flexShrink: 0 }}
              >
                ›
              </span>
            </button>
          ))}
        </div>
        {helpMsg && (
          <div
            style={{
              marginTop: 14,
              background: "#eff6ff",
              border: "1.5px solid #bfdbfe",
              borderRadius: 16,
              padding: "14px 18px",
              display: "flex",
              alignItems: "center",
              gap: 12,
            }}
          >
            <span style={{ fontSize: 22, flexShrink: 0 }}>ℹ️</span>
            <div style={{ flex: 1 }}>
              <div
                style={{ fontSize: fs(14), fontWeight: 800, color: "#1d4ed8" }}
              >
                {helpMsg}
              </div>
              <div
                style={{
                  fontSize: fs(12),
                  fontWeight: 600,
                  color: "#3b82f6",
                  marginTop: 3,
                }}
              >
                Esta é uma versão de demonstração.
              </div>
            </div>
            <button
              onClick={() => setHelpMsg(null)}
              style={{
                background: "none",
                border: "none",
                cursor: "pointer",
                color: "#94a3b8",
                fontSize: 18,
                flexShrink: 0,
              }}
            >
              ✕
            </button>
          </div>
        )}
      </div>

      {/* ── LOGOUT ── */}
      <div style={{ padding: "24px 18px 0" }}>
        <button
          onClick={() => tap("logout", () => setShowLogoutConfirm(true))}
          style={{
            width: "100%",
            background: "#fff",
            border: "2px solid #fca5a5",
            borderRadius: 18,
            padding: "16px",
            fontSize: fs(17),
            fontWeight: 900,
            color: "#dc2626",
            cursor: "pointer",
            minHeight: 58,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            gap: 10,
            transform: tapped === "logout" ? "scale(0.97)" : "scale(1)",
            transition: "all 0.14s",
          }}
        >
          🚪 Sair da conta
        </button>
      </div>

      {/* ── DELETE CONFIRM MODAL ── */}
      {showDeleteConfirm && (
        <>
          <div
            onClick={() => setShowDeleteConfirm(false)}
            style={{
              position: "fixed",
              inset: 0,
              background: "rgba(0,0,0,0.45)",
              zIndex: 60,
            }}
          />
          <div
            style={{
              position: "fixed",
              bottom: 0,
              left: "50%",
              transform: "translateX(-50%)",
              width: "100%",
              maxWidth: 430,
              background: "#fff",
              borderRadius: "28px 28px 0 0",
              padding: "10px 22px 48px",
              zIndex: 61,
              boxShadow: "0 -8px 40px rgba(0,0,0,0.18)",
            }}
          >
            <div
              style={{
                width: 44,
                height: 5,
                background: "#e2e8f0",
                borderRadius: 10,
                margin: "10px auto 22px",
              }}
            />
            <div style={{ textAlign: "center", marginBottom: 20 }}>
              <span style={{ fontSize: 60 }}>⚠️</span>
              <div
                style={{
                  fontSize: fs(20),
                  fontWeight: 900,
                  color: "#1e293b",
                  marginTop: 12,
                  marginBottom: 8,
                }}
              >
                Apagar sua conta?
              </div>
              <div
                style={{
                  fontSize: fs(14),
                  fontWeight: 600,
                  color: "#64748b",
                  lineHeight: 1.55,
                  padding: "0 8px",
                }}
              >
                Se continuar, seus documentos também serão apagados.
              </div>
            </div>
            {deleteSimulated ? (
              <div
                style={{
                  background: "#fef2f2",
                  border: "1.5px solid #fecaca",
                  borderRadius: 16,
                  padding: "18px",
                  textAlign: "center",
                }}
              >
                <div
                  style={{
                    fontSize: fs(16),
                    fontWeight: 900,
                    color: "#dc2626",
                  }}
                >
                  🗑️ Conta apagada (simulação)
                </div>
                <div
                  style={{ fontSize: fs(13), color: "#64748b", marginTop: 4 }}
                >
                  Nenhum dado foi removido de verdade.
                </div>
                <button
                  onClick={() => {
                    setDeleteSimulated(false)
                    setShowDeleteConfirm(false)
                  }}
                  style={{
                    marginTop: 14,
                    background: "#2563eb",
                    border: "none",
                    borderRadius: 14,
                    padding: "12px 28px",
                    fontSize: fs(15),
                    fontWeight: 900,
                    color: "#fff",
                    cursor: "pointer",
                  }}
                >
                  ← Fechar
                </button>
              </div>
            ) : (
              <div
                style={{ display: "flex", flexDirection: "column", gap: 12 }}
              >
                <button
                  onClick={() => setDeleteSimulated(true)}
                  style={{
                    background: "#dc2626",
                    border: "none",
                    borderRadius: 18,
                    padding: "16px",
                    fontSize: fs(16),
                    fontWeight: 900,
                    color: "#fff",
                    cursor: "pointer",
                    minHeight: 56,
                  }}
                >
                  🗑️ Sim, apagar
                </button>
                <button
                  onClick={() => setShowDeleteConfirm(false)}
                  style={{
                    background: "#eff6ff",
                    border: "2px solid #bfdbfe",
                    borderRadius: 18,
                    padding: "14px",
                    fontSize: fs(15),
                    fontWeight: 900,
                    color: "#2563eb",
                    cursor: "pointer",
                    minHeight: 52,
                  }}
                >
                  ← Não, voltar
                </button>
              </div>
            )}
          </div>
        </>
      )}

      {/* ── LOGOUT CONFIRM MODAL ── */}
      {showLogoutConfirm && (
        <>
          <div
            onClick={() => setShowLogoutConfirm(false)}
            style={{
              position: "fixed",
              inset: 0,
              background: "rgba(0,0,0,0.45)",
              zIndex: 60,
            }}
          />
          <div
            style={{
              position: "fixed",
              bottom: 0,
              left: "50%",
              transform: "translateX(-50%)",
              width: "100%",
              maxWidth: 430,
              background: "#fff",
              borderRadius: "28px 28px 0 0",
              padding: "10px 22px 48px",
              zIndex: 61,
              boxShadow: "0 -8px 40px rgba(0,0,0,0.18)",
            }}
          >
            <div
              style={{
                width: 44,
                height: 5,
                background: "#e2e8f0",
                borderRadius: 10,
                margin: "10px auto 22px",
              }}
            />
            <div style={{ textAlign: "center", marginBottom: 22 }}>
              <span style={{ fontSize: 56 }}>🚪</span>
              <div
                style={{
                  fontSize: fs(20),
                  fontWeight: 900,
                  color: "#1e293b",
                  marginTop: 12,
                  marginBottom: 8,
                }}
              >
                Você quer sair?
              </div>
            </div>
            <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
              <button
                onClick={() => {
                  setShowLogoutConfirm(false)
                  setLoggedOut(true)
                }}
                style={{
                  background: "#dc2626",
                  border: "none",
                  borderRadius: 18,
                  padding: "16px",
                  fontSize: fs(16),
                  fontWeight: 900,
                  color: "#fff",
                  cursor: "pointer",
                  minHeight: 56,
                }}
              >
                Sim, sair
              </button>
              <button
                onClick={() => setShowLogoutConfirm(false)}
                style={{
                  background: "#eff6ff",
                  border: "2px solid #bfdbfe",
                  borderRadius: 18,
                  padding: "14px",
                  fontSize: fs(15),
                  fontWeight: 900,
                  color: "#2563eb",
                  cursor: "pointer",
                  minHeight: 52,
                }}
              >
                Não, ficar
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  )
}
