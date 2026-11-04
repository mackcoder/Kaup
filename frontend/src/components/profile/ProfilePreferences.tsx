import type { ProfileModel } from "../../features/profile/useProfile";
import type { FontSize } from "../../types/domain";
export default function ProfilePreferences({ model }: { model: ProfileModel }) {
  const {
    fontSize,
    explainLevel,
    income,
    shareIncomeWithAi,
    service,
    setFontSize,
    setExplainLevel,
    setIncome,
    setShareIncome,
    audioKey,
    tapped,
    fs,
    tap,
    playAudio,
    explainOptions,
    incomeOptions,
    s,
  } = model;
  return (
    <>
      {/* ── EXPLAIN PREFERENCE ── */}
      <div style={s.section}>
        <div className="kaup-243">
          <div style={s.sectionTitle}>🤖 Como você quer as explicações?</div>
        </div>
        <div className="kaup-244">
          <button
            onClick={() => playAudio("explain")}
            style={s.audioBtn("explain")}
          >
            {audioKey === "explain" ? "🔊 Lendo..." : "🔊 Ouvir esta pergunta"}
          </button>
        </div>
        <div className="kaup-202">
          {explainOptions.map((opt) => {
            const active = explainLevel === opt.key;
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
                <span className="kaup-245">{opt.emoji}</span>
                <div className="kaup-39">
                  <div
                    className="kaup-208"
                    style={{
                      fontSize: fs(16),
                      color: active ? "#1d4ed8" : "#1e293b",
                    }}
                  >
                    {opt.title}
                  </div>
                  <div className="kaup-217" style={{ fontSize: fs(12) }}>
                    {opt.sub}
                  </div>
                </div>
                {active && (
                  <span className="kaup-246" style={{ fontSize: fs(12) }}>
                    ✓ Escolhido
                  </span>
                )}
              </button>
            );
          })}
        </div>
      </div>

      {/* ── FONT SIZE ── */}
      <div style={s.section}>
        <div style={s.sectionTitle}>🔠 Tamanho das letras</div>
        <div className="kaup-247">
          {(["small", "medium", "large"] as FontSize[]).map((sz, i) => {
            const labels = ["A Pequena", "A Média", "A Grande"];
            const fsz = [16, 20, 26];
            const active = fontSize === sz;
            return (
              <button
                key={sz}
                onClick={() => tap(`fs-${sz}`, () => setFontSize(sz))}
                className="kaup-248"
                style={{
                  background: active ? "#eff6ff" : "#fff",
                  border: `2px solid ${active ? "#2563eb" : "#e2e8f0"}`,
                  transform: tapped === `fs-${sz}` ? "scale(0.95)" : "scale(1)",
                }}
              >
                <span
                  className="kaup-249"
                  style={{
                    fontSize: fsz[i],
                    color: active ? "#2563eb" : "#1e293b",
                  }}
                >
                  A
                </span>
                <span
                  className="kaup-250"
                  style={{
                    fontSize: fs(11),
                    color: active ? "#2563eb" : "#64748b",
                  }}
                >
                  {labels[i]}
                </span>
                {active && <span className="kaup-251">✓</span>}
              </button>
            );
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
        <div className="kaup-240">
          {incomeOptions.map((opt) => {
            const active = income === opt.key;
            return (
              <button
                key={opt.key}
                onClick={() => tap(`inc-${opt.key}`, () => setIncome(opt.key))}
                className="kaup-252"
                style={{
                  background: active ? "#eff6ff" : "#fff",
                  border: `2px solid ${active ? "#2563eb" : "#e2e8f0"}`,
                  transform:
                    tapped === `inc-${opt.key}` ? "scale(0.97)" : "scale(1)",
                }}
              >
                <div
                  className="kaup-253"
                  style={{
                    border: `2.5px solid ${active ? "#2563eb" : "#cbd5e1"}`,
                    background: active ? "#2563eb" : "#fff",
                  }}
                >
                  {active && <div className="kaup-167" />}
                </div>
                <span
                  className="kaup-39"
                  style={{
                    fontSize: fs(15),
                    fontWeight: active ? 800 : 700,
                    color: active ? "#1d4ed8" : "#334155",
                  }}
                >
                  {opt.label}
                </span>
                {active && (
                  <span className="kaup-210" style={{ fontSize: fs(18) }}>
                    ✓
                  </span>
                )}
              </button>
            );
          })}
        </div>
        <div className="kaup-254">
          <span className="kaup-255">🔒</span>
          <span className="kaup-256" style={{ fontSize: fs(13) }}>
            {service.mode === "mock"
              ? "Demonstração: a renda fica só na memória desta sessão."
              : "A renda fica guardada só na sua conta."}
          </span>
        </div>
        <button
          type="button"
          role="switch"
          aria-checked={shareIncomeWithAi}
          className={`share-toggle${shareIncomeWithAi ? " on" : ""}`}
          style={{ fontSize: fs(15) }}
          onClick={() => tap("share", () => setShareIncome(!shareIncomeWithAi))}
        >
          <span className="share-toggle-text">
            <strong>Deixar a IA usar minha renda</strong>
            <small style={{ fontSize: fs(13) }}>
              {shareIncomeWithAi
                ? "Ligado: a faixa de renda é enviada à IA só para dizer se as parcelas cabem no seu orçamento. Seu nome e e-mail nunca são enviados."
                : "Desligado: a IA não recebe a sua renda, mesmo que você tenha escolhido uma faixa acima."}
            </small>
          </span>
          <span className="share-toggle-track" aria-hidden="true">
            <span className="share-toggle-knob" />
          </span>
        </button>
      </div>
    </>
  );
}
