import { useEffect, useRef, useState } from "react";
import { useApp } from "../../app/AppProvider";
import { errorMessage } from "../../services";
import type {
  ChatMessage,
  ContextFile,
  DocumentAnalysis,
  DocumentView,
} from "../../types/domain";
export function useChat(doc: DocumentView) {
  const { service, acceptDocument } = useApp();
  const [messages, setMessages] = useState<ChatMessage[]>([]),
    [analysis, setAnalysis] = useState<DocumentAnalysis | null>(null),
    [contextFiles, setContextFiles] = useState<ContextFile[]>([]);
  const [phase, setPhase] = useState<"loading" | "chatting">("loading");
  const [error, setError] = useState<string | null>(null),
    [sendError, setSendError] = useState<string | null>(null),
    [isTyping, setIsTyping] = useState(false),
    [attempt, setAttempt] = useState(0);
  const active = useRef(true),
    sendController = useRef<AbortController | null>(null),
    sending = useRef(false),
    failedText = useRef<string | null>(null);
  useEffect(() => {
    active.current = true;
    return () => {
      active.current = false;
      sendController.current?.abort();
    };
  }, []);
  useEffect(() => {
    const controller = new AbortController();
    const timers: ReturnType<typeof setTimeout>[] = [];
    setError(null);
    setPhase("loading");
    let poll: ReturnType<typeof setTimeout> | undefined;
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
        const [history, context] = await Promise.all([
          service.getMessages(doc.id, controller.signal),
          service.getContext(doc.id, controller.signal),
        ]);
        if (controller.signal.aborted) return;
        setContextFiles(context);
        const show = () => {
          setPhase("chatting");
          if (
            service.mode === "mock" &&
            !history.some((m) => m.role === "user")
          ) {
            setMessages([]);
            history.forEach((m, i) =>
              timers.push(
                setTimeout(
                  () => setMessages((old) => [...old, m]),
                  i === 0 ? 0 : i === 1 ? 600 : i === 2 ? 1000 : 1400,
                ),
              ),
            );
          } else
            setMessages(
              history.length
                ? history
                : [
                    {
                      id: crypto.randomUUID(),
                      documentId: doc.id,
                      role: "ai",
                      kind: "summary-card",
                    },
                  ],
            );
        };
        if (doc.isNew && service.mode === "mock")
          timers.push(setTimeout(show, 3200));
        else show();
        if (service.mode === "api") {
          const current = await service.getDocument(doc.id, controller.signal);
          if (!controller.signal.aborted) acceptDocument(current);
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
  }, [doc.id, doc.isNew, service, attempt, acceptDocument]);
  const incomeSent = messages.some((m) => m.role === "user");
  async function send(text: string) {
    if (!text.trim() || sending.current) return false;
    const retry = failedText.current === text;
    const message: ChatMessage = {
      id: crypto.randomUUID(),
      documentId: doc.id,
      role: "user",
      kind: "text",
      text,
      state: "sending",
    };
    if (!retry) setMessages((old) => [...old, message]);
    setIsTyping(true);
    sending.current = true;
    setSendError(null);
    const controller = new AbortController();
    sendController.current = controller;
    try {
      const reply = await service.sendMessage(doc.id, text, controller.signal);
      if (!active.current) return false;
      setMessages((old) => [
        ...old.map((m) =>
          m.role === "user" && m.text === text
            ? { ...m, state: "sent" as const }
            : m,
        ),
        reply,
      ]);
      failedText.current = null;
      if (
        text.includes("R$") ||
        text === "Outra opção" ||
        text === "Prefiro não informar"
      )
        setMessages((old) => [
          ...old,
          {
            id: crypto.randomUUID(),
            documentId: doc.id,
            role: "ai",
            kind: "disclaimer",
            text:
              service.mode === "mock"
                ? "⚖️ Esta análise é demonstrativa e não substitui orientação jurídica ou financeira profissional."
                : "⚖️ Esta análise é informativa e não substitui orientação jurídica ou financeira profissional.",
          },
        ]);
      return true;
    } catch (e) {
      if (active.current && !controller.signal.aborted) {
        failedText.current = text;
        setSendError(errorMessage(e));
        setMessages((old) =>
          old.map((m) =>
            m.role === "user" && m.text === text
              ? { ...m, state: "failed" }
              : m,
          ),
        );
      }
      return false;
    } finally {
      sending.current = false;
      if (active.current) setIsTyping(false);
    }
  }
  async function refreshContext() {
    try {
      setContextFiles(await service.getContext(doc.id));
    } catch (e) {
      setSendError(errorMessage(e));
    }
  }
  return {
    messages,
    analysis,
    contextFiles,
    phase,
    error,
    sendError,
    isTyping,
    incomeSent,
    send,
    retry: () => setAttempt((n) => n + 1),
    retrySend: () =>
      failedText.current ? send(failedText.current) : Promise.resolve(false),
    refreshContext,
  };
}
