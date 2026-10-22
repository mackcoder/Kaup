import { useEffect, useRef, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useApp } from "../app/AppProvider";
import { useChat } from "../features/chat/useChat";
import { presentDocument } from "../styles/presenters";
import type { DocumentView } from "../types/domain";
import { errorMessage } from "../services";
import ResourceState from "../components/ResourceState";
import UploadDialog from "../components/UploadDialog";
import LoadingScreen from "../components/chat/LoadingScreen";
import ContextPopup from "../components/chat/ContextPopup";
import TypingBubble from "../components/chat/TypingBubble";
import MessageList from "../components/chat/MessageList";
import ChatComposer from "../components/chat/ChatComposer";
export default function ChatPage() {
  const { documentId } = useParams();
  const { service } = useApp();
  const navigate = useNavigate();
  const [doc, setDoc] = useState<DocumentView | null>(null),
    [error, setError] = useState<string | null>(null),
    [attempt, setAttempt] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    setError(null);
    setDoc(null);
    if (!documentId) {
      setError("Documento não informado.");
      return;
    }
    service
      .getDocument(documentId, controller.signal)
      .then((d) => {
        if (!controller.signal.aborted) setDoc(presentDocument(d));
      })
      .catch((e) => {
        if (!controller.signal.aborted) setError(errorMessage(e));
      });
    return () => controller.abort();
  }, [service, documentId, attempt]);
  const onBack = () =>
    window.history.state?.idx > 0 ? navigate(-1) : navigate("/documentos");
  if (!doc)
    return (
      <>
        <button className="secondary-button" onClick={onBack}>
          ← Voltar
        </button>
        <ResourceState
          loading={!error}
          error={error}
          onRetry={() => setAttempt((n) => n + 1)}
        />
      </>
    );
  return <ChatView key={doc.id} doc={doc} onBack={onBack} />;
}
function ChatView({ doc, onBack }: { doc: DocumentView; onBack: () => void }) {
  const { service } = useApp();
  const {
    messages,
    analysis,
    contextFiles,
    phase,
    error,
    sendError,
    isTyping,
    stage,
    incomeSelected,
    selectIncome,
    sendIncome,
    inputText,
    setInputText,
    canSendText,
    incomeLocked,
    send,
    retry,
    retrySend,
    refreshContext,
  } = useChat(doc);
  const [showContext, setShowContext] = useState(false),
    [showPlus, setShowPlus] = useState(false),
    [attachment, setAttachment] = useState<"photo" | "file" | "context" | null>(
      null,
    );
  const bottomRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages, isTyping]);
  useEffect(() => {
    function key(e: KeyboardEvent) {
      if (e.key === "Escape") {
        setShowContext(false);
        setShowPlus(false);
      }
    }
    document.addEventListener("keydown", key);
    return () => document.removeEventListener("keydown", key);
  }, []);
  function handleSendIncome() {
    void sendIncome();
  }
  function handleSendText() {
    const text = inputText.trim();
    if (!text || !canSendText) return;
    void send(text).then((ok) => {
      if (ok) setInputText("");
    });
  }
  // ── RENDER ──────────────────────────────────────────────────────────────────
  return (
    <div className="kaup-33">
      {/* ── HEADER ── */}
      <div className="kaup-34">
        {/* Row 1: back + context */}
        <div className="kaup-35">
          <button onClick={onBack} className="kaup-36">
            <span className="kaup-37">←</span>
            <span className="kaup-38">Voltar</span>
          </button>

          <div className="kaup-39" />

          <button
            onClick={() => setShowContext((v) => !v)}
            className="kaup-40"
            style={{
              background: showContext ? doc.catColorBg : "#f1f5f9",
              border: `1.5px solid ${showContext ? doc.catColorLight : "#e2e8f0"}`,
            }}
          >
            <span className="kaup-41">📂</span>
            <span
              className="kaup-42"
              style={{ color: showContext ? doc.catColor : "#334155" }}
            >
              Contexto
            </span>
            <span
              className="kaup-43"
              style={{
                background: showContext ? doc.catColor : "#e2e8f0",
                color: showContext ? "#fff" : "#64748b",
              }}
            >
              {contextFiles.length}
            </span>
          </button>
        </div>

        {/* Row 2: doc identity */}
        <div className="kaup-44">
          <div
            className="kaup-45"
            style={{
              background: doc.catColorBg,
              border: `2px solid ${doc.catColorLight}60`,
            }}
          >
            {doc.emoji}
          </div>
          <div className="kaup-25">
            <div className="kaup-46">{doc.name}</div>
            <div className="kaup-47">{doc.description}</div>
            <div className="kaup-48">
              <span
                className="kaup-49"
                style={{ color: doc.catColor, background: doc.catColorBg }}
              >
                {doc.catEmoji} {doc.catLabel}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* ── CONTEXT POPUP ── */}
      {showContext && (
        <ContextPopup
          doc={doc}
          files={contextFiles}
          onClose={() => setShowContext(false)}
          onAddFile={() => {
            setShowContext(false);
            setAttachment("context");
          }}
        />
      )}

      {/* ── MAIN AREA ── */}
      {attachment && (
        <UploadDialog
          contextId={doc.id}
          initialCapture={attachment === "photo"}
          onClose={() => setAttachment(null)}
          onAttached={() => void refreshContext()}
        />
      )}
      {error ? (
        <ResourceState error={error} onRetry={retry} />
      ) : phase === "loading" ? (
        service.mode === "mock" ? (
          <LoadingScreen doc={doc} />
        ) : (
          <ResourceState loading />
        )
      ) : (
        <>
          {/* Chat scroll area */}
          <div
            className="kaup-50"
            role="log"
            aria-label="Conversa sobre o documento"
            aria-live="polite"
          >
            {/* AI identity row */}
            <div className="kaup-51">
              <div
                className="kaup-52"
                style={{
                  background: doc.catColorBg,
                  border: `2px solid ${doc.catColorLight}60`,
                }}
              >
                🤖
              </div>
              <span className="kaup-53" style={{ color: doc.catColor }}>
                Assistente IA
              </span>
              <div className="kaup-54" />
            </div>

            <MessageList
              messages={messages}
              doc={doc}
              analysis={analysis}
              stage={stage}
              incomeLocked={incomeLocked}
              incomeSelected={incomeSelected}
              setIncomeSelected={selectIncome}
              handleSendIncome={handleSendIncome}
            />

            {isTyping && <TypingBubble color={doc.catColorLight} />}
            <div ref={bottomRef} className="kaup-55" />
          </div>

          {sendError && (
            <div role="alert" className="feedback">
              {sendError}
              <button
                className="secondary-button"
                disabled={incomeLocked && isTyping}
                onClick={() =>
                  void retrySend().then((ok) => {
                    if (ok) setInputText("");
                  })
                }
              >
                Tentar enviar novamente
              </button>
            </div>
          )}
          <ChatComposer
            doc={doc}
            inputText={inputText}
            setInputText={setInputText}
            canSendText={canSendText}
            guided={stage !== "free-chat"}
            showPlus={showPlus}
            setShowPlus={setShowPlus}
            handleSendText={handleSendText}
            onAttachment={(kind) => {
              setShowPlus(false);
              setAttachment(kind);
            }}
          />
        </>
      )}
    </div>
  );
}
