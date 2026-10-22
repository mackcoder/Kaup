import PlusMenu from "./PlusMenu";
import type { DocumentView } from "../../types/domain";
export default function ChatComposer({
  doc,
  inputText,
  setInputText,
  canSendText,
  guided,
  showPlus,
  setShowPlus,
  handleSendText,
  onAttachment,
}: {
  doc: DocumentView;
  inputText: string;
  setInputText: (s: string) => void;
  canSendText: boolean;
  guided: boolean;
  showPlus: boolean;
  setShowPlus: (s: boolean | ((v: boolean) => boolean)) => void;
  handleSendText: () => void;
  onAttachment: (kind: "photo" | "file" | "context") => void;
}) {
  return (
    <>
      {" "}
      {/* ── BOTTOM INPUT BAR ── */}
      <div className="kaup-135">
        <div className="kaup-136">
          {/* Plus button */}
          <div className="kaup-137">
            <button
              aria-label="Anexar documento"
              onClick={() => setShowPlus((v) => !v)}
              className="kaup-138"
              style={{
                border: `1.5px solid ${showPlus ? doc.catColorLight : "#e2e8f0"}`,
                background: showPlus ? doc.catColorBg : "#fff",
                color: showPlus ? doc.catColor : "#64748b",
              }}
            >
              {showPlus ? "✕" : "+"}
            </button>
            {showPlus && (
              <PlusMenu
                onClose={() => setShowPlus(false)}
                onSelect={onAttachment}
                catColorBg={doc.catColorBg}
              />
            )}
          </div>

          {/* Text input */}
          <input
            aria-label="Mensagem sobre o documento"
            minLength={1}
            type="text"
            value={inputText}
            onChange={(e) => setInputText(e.target.value)}
            onKeyDown={(e) => e.key === "Enter" && handleSendText()}
            placeholder={
              !guided
                ? "Pergunte sobre este contrato..."
                : "Responda a pergunta acima primeiro..."
            }
            disabled={!canSendText}
            className="kaup-139"
            style={{ caretColor: doc.catColorLight }}
          />

          {/* Send button */}
          <button
            aria-label="Enviar mensagem"
            onClick={handleSendText}
            disabled={!inputText.trim() || !canSendText}
            className="kaup-140"
            style={{
              background:
                inputText.trim() && canSendText
                  ? `linear-gradient(135deg,${doc.catColor},${doc.catColorLight})`
                  : "#e2e8f0",
              color: inputText.trim() && canSendText ? "#fff" : "#94a3b8",
              cursor: inputText.trim() && canSendText ? "pointer" : "default",
            }}
          >
            ↑
          </button>
        </div>
      </div>
    </>
  );
}
