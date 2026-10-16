import { useState, useEffect } from "react";
import type { DocumentView } from "../../types/domain";
// ── Loading screen ────────────────────────────────────────────────────────────
const LOAD_STEPS = [
  "Lendo o documento...",
  "Identificando cláusulas importantes...",
  "Verificando pontos de atenção...",
  "Análise pronta! ✅",
];

function SpinDot({ color }: { color: string }) {
  return (
    <div className="kaup-154" style={{ border: `2.5px solid ${color}` }} />
  );
}

export default function LoadingScreen({ doc }: { doc: DocumentView }) {
  const [step, setStep] = useState(0);
  useEffect(() => {
    const id = setInterval(
      () => setStep((s) => Math.min(s + 1, LOAD_STEPS.length - 1)),
      750,
    );
    return () => clearInterval(id);
  }, []);

  return (
    <div className="kaup-155">
      <div className="kaup-156" style={{ background: doc.catColorBg }}>
        {doc.emoji}
      </div>
      <div className="kaup-157">
        <div className="kaup-158">Simulando análise</div>
        <div className="kaup-159">{doc.name}</div>
      </div>
      <div className="kaup-160">
        {LOAD_STEPS.map((s, i) => {
          const done = i < step;
          const active = i === step;
          return (
            <div
              key={i}
              className="kaup-161"
              style={{ opacity: i > step ? 0.3 : 1 }}
            >
              <div
                className="kaup-162"
                style={{
                  background: done
                    ? "#059669"
                    : active
                      ? doc.catColorBg
                      : "#f1f5f9",
                  border: `2px solid ${done ? "#059669" : active ? doc.catColorLight : "#e2e8f0"}`,
                }}
              >
                {done ? (
                  "✓"
                ) : active ? (
                  <SpinDot color={doc.catColorLight} />
                ) : null}
              </div>
              <span
                className="kaup-69"
                style={{
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
    </div>
  );
}
