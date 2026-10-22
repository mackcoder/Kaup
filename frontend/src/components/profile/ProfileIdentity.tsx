import Modal from "../Modal";
import type { ProfileModel } from "../../features/profile/useProfile";
export default function ProfileIdentity({ model }: { model: ProfileModel }) {
  const {
    name,
    email,
    editName,
    setEditName,
    editEmail,
    setEditEmail,
    showEdit,
    setShowEdit,
    editError,
    setEditError,
    profileError,
    savedMsg,
    audioKey,
    tapped,
    fs,
    tap,
    playAudio,
    handleSave,
    s,
    onBack,
  } = model;
  return (
    <>
      {/* ── HEADER ── */}
      <div style={s.header}>
        <div className="kaup-44">
          <button
            onClick={() => tap("back", onBack)}
            style={{
              ...s.backBtn,
              transform: tapped === "back" ? "scale(0.96)" : "scale(1)",
            }}
          >
            <span className="kaup-224" style={{ fontSize: fs(18) }}>
              ←
            </span>
            <span className="kaup-225" style={{ fontSize: fs(15) }}>
              Voltar
            </span>
          </button>
          <div>
            <div className="kaup-209" style={{ fontSize: fs(19) }}>
              Meu perfil
            </div>
            <div className="kaup-226" style={{ fontSize: fs(12) }}>
              Suas informações e escolhas
            </div>
          </div>
        </div>
      </div>

      {/* ── USER CARD ── */}
      <div className="kaup-227">
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
          <div className="kaup-228">👤</div>
          <div>
            <div className="kaup-219" style={{ fontSize: fs(20) }}>
              {name}
            </div>
            <div className="kaup-229" style={{ fontSize: fs(14) }}>
              {email}
            </div>
          </div>

          {savedMsg && (
            <div className="kaup-230" style={{ fontSize: fs(15) }}>
              ✅ Dados salvos!
            </div>
          )}

          <div className="kaup-231">
            <button
              onClick={() => {
                setEditError("");
                setEditName(name);
                setEditEmail(email);
                setShowEdit(true);
              }}
              className="kaup-232"
              style={{ fontSize: fs(15) }}
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
        <Modal
          label="Editar perfil"
          onClose={() => setShowEdit(false)}
          sheetClassName="kaup-234"
          fullOverlay
        >
          <div className="kaup-193" />
          <div className="kaup-235" style={{ fontSize: fs(18) }}>
            ✏️ Mudar meus dados
          </div>
          {editError && <p role="alert">{editError}</p>}
          {profileError && (
            <p role="alert" className="feedback">
              {profileError}
            </p>
          )}
          <div className="kaup-236">
            <label className="kaup-237" style={{ fontSize: fs(13) }}>
              Seu nome
            </label>
            <input
              aria-label="Seu nome"
              value={editName}
              onChange={(e) => setEditName(e.target.value)}
              className="kaup-238"
              style={{ fontSize: fs(15), boxSizing: "border-box" as const }}
            />
          </div>
          <div className="kaup-239">
            <label className="kaup-237" style={{ fontSize: fs(13) }}>
              Seu e-mail
            </label>
            <input
              aria-label="Seu e-mail"
              type="email"
              value={editEmail}
              onChange={(e) => setEditEmail(e.target.value)}
              className="kaup-238"
              style={{ fontSize: fs(15), boxSizing: "border-box" as const }}
            />
          </div>
          <div className="kaup-240">
            <button
              onClick={handleSave}
              className="kaup-241"
              style={{ fontSize: fs(16) }}
            >
              ✅ Salvar
            </button>
            <button
              onClick={() => setShowEdit(false)}
              className="kaup-242"
              style={{ fontSize: fs(15) }}
            >
              Cancelar
            </button>
          </div>
        </Modal>
      )}
    </>
  );
}
