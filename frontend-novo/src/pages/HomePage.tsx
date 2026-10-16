import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useApp } from "../app/AppProvider";
import UploadDialog from "../components/UploadDialog";
export default function HomePage() {
  const navigate = useNavigate();
  const { categories } = useApp();
  const DOCS = categories.map((c) => ({
    emoji: c.emoji,
    label: c.label,
    count: c.docs.length,
    color: c.colorLight,
    bg: c.colorBg,
  }));
  const [tapped, setTapped] = useState<string | null>(null);
  const [showOptions, setShowOptions] = useState(false);
  const handleTap = (key: string, cb?: () => void) => {
    setTapped(key);
    setTimeout(() => {
      setTapped(null);
      cb?.();
    }, 150);
  };
  // ── HOME SCREEN ─────────────────────────────────────────────────────────────
  return (
    <div className="kaup-95">
      {/* Header */}
      <div className="kaup-96">
        <div>
          <div className="kaup-97">Contrato Fácil</div>
          <div className="kaup-98">Seus documentos</div>
        </div>
        <button
          aria-label="Meu perfil"
          onClick={() => navigate("/perfil")}
          className="kaup-99"
        >
          👤
        </button>
      </div>

      {/* Chatbot card */}
      <div className="kaup-100">
        <div className="kaup-101">
          <div className="kaup-102" />
          <div className="kaup-103" />

          <div className="kaup-104">
            <div className="kaup-105">🤖</div>
            <div>
              <div className="kaup-106">
                <span className="kaup-107">ASSISTENTE IA</span>
              </div>
              <div className="kaup-108">Olá! 👋</div>
            </div>
          </div>

          <div className="kaup-109">
            <p className="kaup-110">Envie um documento para&nbsp;começarmos.</p>
            <p className="kaup-111">
              Eu leio e explico tudo pra você, de um jeito fácil de entender. ✅
            </p>
          </div>

          <button
            onClick={() => handleTap("main", () => setShowOptions(true))}
            className="kaup-112"
            style={{
              background: tapped === "main" ? "#f0fdf4" : "#ffffff",
              transform: tapped === "main" ? "scale(0.97)" : "scale(1)",
            }}
          >
            <span className="kaup-113">📤</span>
            <span className="kaup-114">Enviar documento</span>
          </button>
        </div>
      </div>

      {showOptions && <UploadDialog onClose={() => setShowOptions(false)} />}

      {/* Section label */}
      <div className="kaup-115">
        <div>
          <div className="kaup-116">Meus documentos</div>
          <div className="kaup-117">Toque para ver o resultado</div>
        </div>
        <button onClick={() => navigate("/documentos")} className="kaup-118">
          Ver todos
        </button>
      </div>

      {/* Doc grid */}
      <div className="kaup-119">
        {DOCS.map((doc) => (
          <button
            key={doc.label}
            onClick={() =>
              handleTap(doc.label, () => navigate(`/categorias/${doc.label}`))
            }
            className="kaup-120"
            style={{
              border: `2.5px solid ${tapped === doc.label ? doc.color : "#e2e8f0"}`,
              boxShadow:
                tapped === doc.label
                  ? `0 4px 20px ${doc.color}30`
                  : "0 2px 10px rgba(0,0,0,0.06)",
              transform: tapped === doc.label ? "scale(0.96)" : "scale(1)",
            }}
          >
            {doc.count > 0 && (
              <div className="kaup-121" style={{ background: doc.color }}>
                <span className="kaup-122">{doc.count}</span>
              </div>
            )}
            <div className="kaup-123" style={{ background: doc.bg }}>
              {doc.emoji}
            </div>
            <span className="kaup-124">{doc.label}</span>
            <span className="kaup-125">
              {doc.count} {doc.count === 1 ? "documento" : "documentos"}
            </span>
          </button>
        ))}

        {/* Add new card */}
        <button
          onClick={() => handleTap("add", () => setShowOptions(true))}
          className="kaup-126"
          style={{ transform: tapped === "add" ? "scale(0.96)" : "scale(1)" }}
        >
          <div className="kaup-127">➕</div>
          <span className="kaup-128">Novo tipo</span>
          <span className="kaup-129">Adicionar</span>
        </button>
      </div>

      {/* Bottom tip */}
      <div className="kaup-130">
        <div className="kaup-131">
          <span className="kaup-132">💡</span>
          <div>
            <div className="kaup-133">Dica</div>
            <div className="kaup-134">
              Nunca assine um contrato sem entender o que diz. Envie aqui antes!
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
