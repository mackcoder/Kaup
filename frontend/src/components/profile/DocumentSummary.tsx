import type { ProfileModel } from "../../features/profile/useProfile";
export default function DocumentSummary({ model }: { model: ProfileModel }) {
  const { tapped, fs, tap, docStats, s, onOpenAllDocs } = model;
  return (
    <>
      {/* ── DOC SUMMARY ── */}
      <div style={s.section}>
        <div style={s.sectionTitle}>📄 Meus documentos</div>
        <div style={s.sectionSub}>Veja como estão seus contratos.</div>
        <div className="kaup-202">
          {docStats.map((d) => (
            <button
              key={d.key}
              onClick={() => tap(d.key, () => onOpenAllDocs(d.key))}
              className="kaup-206"
              style={{
                background: d.bg,
                border: `2px solid ${d.border}`,
                transform: tapped === d.key ? "scale(0.97)" : "scale(1)",
              }}
            >
              <span className="kaup-207">{d.emoji}</span>
              <div className="kaup-39">
                <div
                  className="kaup-208"
                  style={{ fontSize: fs(16), color: d.color }}
                >
                  {d.label}
                </div>
                <div className="kaup-209" style={{ fontSize: fs(22) }}>
                  {d.sub}
                </div>
              </div>
              <span
                className="kaup-210"
                style={{ fontSize: fs(22), color: d.color }}
              >
                ›
              </span>
            </button>
          ))}
        </div>
        <button
          onClick={() => tap("all-docs", () => onOpenAllDocs("all"))}
          className="kaup-211"
          style={{
            fontSize: fs(16),
            transform: tapped === "all-docs" ? "scale(0.97)" : "scale(1)",
          }}
        >
          📂 Ver todos os documentos
        </button>
      </div>
    </>
  );
}
