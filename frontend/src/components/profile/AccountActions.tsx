import Modal from "../Modal";
import type { ProfileModel } from "../../features/profile/useProfile";
export default function AccountActions({ model }: { model: ProfileModel }) {
  const {
    service,
    showDeleteConfirm,
    setShowDeleteConfirm,
    deleteSimulated,
    setDeleteSimulated,
    showLogoutConfirm,
    setShowLogoutConfirm,
    accountError,
    accountBusy,
    confirmLogout,
    confirmDeleteAccount,
    tapped,
    fs,
    tap,
  } = model;
  return (
    <>
      {/* ── LOGOUT ── */}
      <div className="kaup-189">
        <button
          onClick={() => tap("logout", () => setShowLogoutConfirm(true))}
          className="kaup-190"
          style={{
            fontSize: fs(17),
            transform: tapped === "logout" ? "scale(0.97)" : "scale(1)",
          }}
        >
          🚪 Sair da conta
        </button>
      </div>

      {/* ── DELETE CONFIRM MODAL ── */}
      {showDeleteConfirm && (
        <Modal
          label="Apagar conta"
          onClose={() => setShowDeleteConfirm(false)}
          sheetClassName="kaup-192"
          fullOverlay
        >
          <div className="kaup-193" />
          <div className="kaup-194">
            <span className="kaup-195">⚠️</span>
            <div className="kaup-196" style={{ fontSize: fs(20) }}>
              Apagar sua conta?
            </div>
            <div className="kaup-197" style={{ fontSize: fs(14) }}>
              {service.mode === "mock"
                ? "Simulação: nenhum dado de servidor será apagado."
                : "Isto apaga sua conta, seus documentos e as conversas. Não dá para desfazer."}
            </div>
          </div>
          {accountError && (
            <p role="alert" className="feedback">
              {accountError}
            </p>
          )}
          {deleteSimulated ? (
            <div className="kaup-198">
              <div className="kaup-199" style={{ fontSize: fs(16) }}>
                🗑️ Conta apagada (simulação)
              </div>
              <div className="kaup-200" style={{ fontSize: fs(13) }}>
                Nenhum dado foi removido de verdade.
              </div>
              <button
                onClick={() => {
                  setDeleteSimulated(false);
                  setShowDeleteConfirm(false);
                }}
                className="kaup-201"
                style={{ fontSize: fs(15) }}
              >
                ← Fechar
              </button>
            </div>
          ) : (
            <div className="kaup-202">
              <button
                disabled={accountBusy}
                onClick={() => void confirmDeleteAccount()}
                className="kaup-203"
                style={{ fontSize: fs(16) }}
              >
                {accountBusy ? "Apagando..." : "🗑️ Sim, apagar"}
              </button>
              <button
                onClick={() => setShowDeleteConfirm(false)}
                className="kaup-204"
                style={{ fontSize: fs(15) }}
              >
                ← Não, voltar
              </button>
            </div>
          )}
        </Modal>
      )}

      {/* ── LOGOUT CONFIRM MODAL ── */}
      {showLogoutConfirm && (
        <Modal
          label="Sair da conta"
          onClose={() => setShowLogoutConfirm(false)}
          sheetClassName="kaup-192"
          fullOverlay
        >
          <div className="kaup-193" />
          {accountError && (
            <p role="alert" className="feedback">
              {accountError}
            </p>
          )}
          <div className="kaup-205">
            <span className="kaup-75">🚪</span>
            <div className="kaup-196" style={{ fontSize: fs(20) }}>
              Você quer sair?
            </div>
          </div>
          <div className="kaup-202">
            <button
              disabled={accountBusy}
              onClick={() => void confirmLogout()}
              className="kaup-203"
              style={{ fontSize: fs(16) }}
            >
              {accountBusy ? "Saindo..." : "Sim, sair"}
            </button>
            <button
              onClick={() => setShowLogoutConfirm(false)}
              className="kaup-204"
              style={{ fontSize: fs(15) }}
            >
              Não, ficar
            </button>
          </div>
        </Modal>
      )}
    </>
  );
}
