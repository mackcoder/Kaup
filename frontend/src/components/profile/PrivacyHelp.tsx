import type { ProfileModel } from "../../features/profile/useProfile";
export default function PrivacyHelp({ model }: { model: ProfileModel }) {
  const {
    setShowDeleteConfirm,
    helpMsg,
    setHelpMsg,
    tapped,
    fs,
    tap,
    privacyItems,
    helpItems,
    s,
    onOpenAllDocs,
  } = model;
  return (
    <>
      {/* ── PRIVACY ── */}
      <div style={s.section}>
        <div style={s.sectionTitle}>🔒 Minha privacidade</div>
        <div className="kaup-202">
          {privacyItems.map((item) => (
            <button
              key={item.key}
              onClick={() => {
                if (item.key === "delete") {
                  tap(item.key, () => setShowDeleteConfirm(true));
                } else {
                  tap(item.key, () => {
                    if (item.key === "docs" || item.key === "history")
                      onOpenAllDocs("all");
                    else
                      setHelpMsg(
                        "Exportação de dados ainda não disponível nesta demonstração.",
                      );
                  });
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
              <span className="kaup-216">{item.emoji}</span>
              <div className="kaup-39">
                <div
                  className="kaup-208"
                  style={{
                    fontSize: fs(15),
                    color: (item as { danger?: boolean }).danger
                      ? "#dc2626"
                      : "#1e293b",
                  }}
                >
                  {item.title}
                </div>
                <div className="kaup-217" style={{ fontSize: fs(12) }}>
                  {item.sub}
                </div>
              </div>
              <span className="kaup-218" style={{ fontSize: fs(20) }}>
                ›
              </span>
            </button>
          ))}
        </div>
      </div>

      {/* ── HELP ── */}
      <div style={s.section}>
        <div style={s.sectionTitle}>💬 Precisa de ajuda?</div>
        <div className="kaup-202">
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
              <span className="kaup-216">{item.emoji}</span>
              <div className="kaup-39">
                <div className="kaup-219" style={{ fontSize: fs(15) }}>
                  {item.title}
                </div>
                <div className="kaup-217" style={{ fontSize: fs(12) }}>
                  {item.sub}
                </div>
              </div>
              <span className="kaup-218" style={{ fontSize: fs(20) }}>
                ›
              </span>
            </button>
          ))}
        </div>
        {helpMsg && (
          <div className="kaup-220">
            <span className="kaup-132">ℹ️</span>
            <div className="kaup-39">
              <div className="kaup-221" style={{ fontSize: fs(14) }}>
                {helpMsg}
              </div>
              <div className="kaup-222" style={{ fontSize: fs(12) }}>
                Esta é uma versão de demonstração.
              </div>
            </div>
            <button onClick={() => setHelpMsg(null)} className="kaup-223">
              ✕
            </button>
          </div>
        )}
      </div>
    </>
  );
}
