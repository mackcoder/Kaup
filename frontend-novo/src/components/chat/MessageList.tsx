import SummaryCard from "./SummaryCard";
import type {
  ChatMessage,
  DocumentAnalysis,
  DocumentView,
} from "../../types/domain";
export default function MessageList({
  messages,
  doc,
  analysis,
  incomeSent,
  incomeSelected,
  setIncomeSelected,
  handleSendIncome,
}: {
  messages: ChatMessage[];
  doc: DocumentView;
  analysis: DocumentAnalysis | null;
  incomeSent: boolean;
  incomeSelected: string | null;
  setIncomeSelected: (v: string) => void;
  handleSendIncome: () => void;
}) {
  return (
    <>
      {" "}
      {messages.map((msg) => {
        const isUser = msg.role === "user";

        if (msg.kind === "summary-card") {
          return (
            <div key={msg.id} className="kaup-163">
              <SummaryCard doc={doc} analysis={analysis} />
            </div>
          );
        }

        if (msg.kind === "options" && !incomeSent) {
          return (
            <div key={msg.id} className="kaup-164">
              {(msg.options ?? []).map((opt) => (
                <button
                  key={opt}
                  aria-pressed={incomeSelected === opt}
                  onClick={() => setIncomeSelected(opt)}
                  className="kaup-165"
                  style={{
                    border: `2px solid ${incomeSelected === opt ? doc.catColorLight : "#e2e8f0"}`,
                    background:
                      incomeSelected === opt ? doc.catColorBg : "#fff",
                    boxShadow:
                      incomeSelected === opt
                        ? `0 2px 12px ${doc.catColorLight}30`
                        : "0 1px 4px rgba(0,0,0,0.04)",
                  }}
                >
                  <div
                    className="kaup-166"
                    style={{
                      border: `2.5px solid ${incomeSelected === opt ? doc.catColorLight : "#cbd5e1"}`,
                      background:
                        incomeSelected === opt ? doc.catColorLight : "#fff",
                    }}
                  >
                    {incomeSelected === opt && <div className="kaup-167" />}
                  </div>
                  <span
                    className="kaup-168"
                    style={{
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
                  className="kaup-169"
                  style={{
                    background: `linear-gradient(135deg,${doc.catColor},${doc.catColorLight})`,
                    boxShadow: `0 4px 16px ${doc.catColorLight}50`,
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
            <div key={msg.id} className="kaup-170">
              {msg.text}
              {msg.state === "failed" && (
                <small className="message-failed">Não enviada</small>
              )}
            </div>
          );
        }

        // text bubbles
        if (msg.kind === "options") return null; // already sent, hide options

        return (
          <div
            key={msg.id}
            className="kaup-171"
            style={{ justifyContent: isUser ? "flex-end" : "flex-start" }}
          >
            <div
              className="kaup-172"
              style={{
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
                className="kaup-173"
                style={{ color: isUser ? "#fff" : "#1e293b" }}
              >
                {msg.text}
                {msg.state === "failed" && (
                  <small className="message-failed">Não enviada</small>
                )}
              </span>
            </div>
          </div>
        );
      })}
    </>
  );
}
