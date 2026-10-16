import { useEffect, useMemo, useState, useSyncExternalStore } from "react";
import { useApp } from "../../app/AppProvider";
import { errorMessage } from "../../services";
import type {
  ChatMessage,
  ContextFile,
  DocumentAnalysis,
  DocumentView,
} from "../../types/domain";
import {
  appendMessages,
  chatSession,
  stageFromHistory,
  type PendingTurn,
} from "./session";
export function useChat(doc: DocumentView) {
  const { service, acceptDocument } = useApp();
  const store = useMemo(() => chatSession(service, doc.id), [service, doc.id]);
  const session = useSyncExternalStore(store.subscribe, store.getSnapshot);
  const [analysis, setAnalysis] = useState<DocumentAnalysis | null>(null);
  const [contextFiles, setContextFiles] = useState<ContextFile[]>([]);
  const [phase, setPhase] = useState<"loading" | "chatting">("loading");
  const [error, setError] = useState<string | null>(null),
    [attempt, setAttempt] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    const timers: ReturnType<typeof setTimeout>[] = [];
    let poll: ReturnType<typeof setTimeout> | undefined;
    setError(null);
    setPhase("loading");
    async function load() {
      try {
        const result = await service.getAnalysis(doc.id, controller.signal);
        if (controller.signal.aborted) return;
        setAnalysis(result);
        if (result.processingStatus === "failed") {
          setError(
            result.ocrQualityWarning ??
              "Não foi possível processar o documento. Envie uma foto mais nítida ou tente novamente.",
          );
          return;
        }
        if (
          result.processingStatus === "pending" ||
          result.processingStatus === "processing"
        ) {
          poll = setTimeout(() => void load(), 2500);
          return;
        }
        const current = store.getSnapshot();
        const [history, context] = await Promise.all([
          current.initialized
            ? Promise.resolve(current.messages)
            : service.getMessages(doc.id, controller.signal),
          service.getContext(doc.id, controller.signal),
        ]);
        if (controller.signal.aborted) return;
        setContextFiles(context);
        const show = () => {
          setPhase("chatting");
          if (store.getSnapshot().initialized) return;
          store.update({ initialized: true });
          if (
            service.mode === "mock" &&
            stageFromHistory(service.mode, history) === "waiting-income"
          ) {
            // Same progressive reveal as the original: 0, 600, 1000, 1400ms.
            history.forEach((message, index) =>
              timers.push(
                setTimeout(
                  () => {
                    store.update((s) => ({
                      messages: appendMessages(s.messages, [message]),
                      stage:
                        message.kind === "options" ? "waiting-income" : s.stage,
                    }));
                  },
                  (doc.isNew ? 300 : 0) +
                    [0, 600, 1000, 1400][Math.min(index, 3)],
                ),
              ),
            );
          } else {
            const messages: ChatMessage[] = history.length
              ? history
              : [
                  {
                    id: `summary-${doc.id}`,
                    documentId: doc.id,
                    role: "ai",
                    kind: "summary-card",
                  },
                ];
            store.update({
              messages,
              stage: stageFromHistory(service.mode, history),
            });
          }
        };
        // Interrupted introductions resume once without replaying or duplicating messages.
        if (current.initialized && current.stage === "intro")
          store.update({
            messages: await service.getMessages(doc.id, controller.signal),
            stage: "waiting-income",
          });
        if (doc.isNew && service.mode === "mock" && !current.initialized)
          timers.push(setTimeout(show, 3200));
        else show();
        if (service.mode === "api") {
          const latest = await service.getDocument(doc.id, controller.signal);
          if (!controller.signal.aborted) acceptDocument(latest);
        }
      } catch (e) {
        if (!controller.signal.aborted) setError(errorMessage(e));
      }
    }
    void load();
    return () => {
      controller.abort();
      clearTimeout(poll);
      timers.forEach(clearTimeout);
    };
  }, [doc.id, doc.isNew, service, attempt, acceptDocument, store]);
  async function executeTurn(turn: PendingTurn) {
    if (store.getSnapshot().isSending) return false;
    const user: ChatMessage = {
      id: `user-${turn.turnId}`,
      documentId: doc.id,
      role: "user",
      kind: "text",
      text: turn.text,
      intent: turn.intent,
      turnId: turn.turnId,
      state: "sending",
    };
    store.update((s) => ({
      pendingTurn: turn,
      isSending: true,
      isTyping: true,
      sendError: null,
      stage: turn.intent === "income" ? "responding-income" : s.stage,
      messages: appendMessages(s.messages, [user]).map((m) =>
        m.id === user.id ? user : m,
      ),
    }));
    try {
      // An accepted send belongs to the document session, even when its screen unmounts.
      const result = await service.sendMessage(
        doc.id,
        turn.text,
        undefined,
        turn,
      );
      store.update((s) => ({
        messages: appendMessages(
          s.messages.map((m) =>
            m.id === user.id ? { ...m, state: "sent" } : m,
          ),
          [result.reply],
        ),
        isTyping: false,
      }));
      if (result.followUps.length)
        await new Promise<void>((resolve) => setTimeout(resolve, 800));
      store.update((s) => ({
        messages: appendMessages(s.messages, result.followUps),
        stage: "free-chat",
        pendingTurn: null,
        isSending: false,
        sendError: null,
        draft:
          turn.intent === "free-text" && s.draft === turn.text ? "" : s.draft,
      }));
      return true;
    } catch (e) {
      store.update((s) => ({
        isSending: false,
        isTyping: false,
        sendError: errorMessage(e),
        stage: turn.intent === "income" ? "waiting-income" : s.stage,
        messages: s.messages.map((m) =>
          m.id === user.id ? { ...m, state: "failed" } : m,
        ),
      }));
      return false;
    }
  }
  function selectIncome(value: string) {
    const s = store.getSnapshot();
    if (
      s.stage === "waiting-income" &&
      !s.pendingTurn &&
      s.messages.some(
        (m) => m.intent === "income" && m.options?.includes(value),
      )
    )
      store.update({ incomeSelected: value });
  }
  function sendIncome() {
    const s = store.getSnapshot();
    return s.stage === "waiting-income" && s.incomeSelected && !s.pendingTurn
      ? executeTurn({
          turnId: crypto.randomUUID(),
          intent: "income",
          text: s.incomeSelected,
        })
      : Promise.resolve(false);
  }
  function send(text: string) {
    const s = store.getSnapshot();
    return s.stage === "free-chat" && text.trim() && !s.pendingTurn
      ? executeTurn({
          turnId: crypto.randomUUID(),
          intent: "free-text",
          text: text.trim(),
        })
      : Promise.resolve(false);
  }
  async function refreshContext() {
    try {
      setContextFiles(await service.getContext(doc.id));
    } catch (e) {
      store.update({ sendError: errorMessage(e) });
    }
  }
  return {
    messages: session.messages,
    analysis,
    contextFiles,
    phase,
    error,
    sendError: session.sendError,
    isTyping: session.isTyping,
    isSending: session.isSending,
    stage: session.stage,
    incomeSelected: session.incomeSelected,
    selectIncome,
    sendIncome,
    inputText: session.draft,
    setInputText: (draft: string) => store.update({ draft }),
    canSendText:
      session.stage === "free-chat" &&
      !session.isSending &&
      !session.pendingTurn,
    incomeLocked: session.stage !== "waiting-income" || !!session.pendingTurn,
    send,
    retry: () => setAttempt((n) => n + 1),
    retrySend: () => {
      const turn = store.getSnapshot().pendingTurn;
      return turn ? executeTurn(turn) : Promise.resolve(false);
    },
    refreshContext,
  };
}
