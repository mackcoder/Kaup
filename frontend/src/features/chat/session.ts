import type { KaupService } from "../../services/contracts/KaupService";
import type {
  ChatMessage,
  ChatSendOptions,
  GuidedStage,
} from "../../types/domain";
export interface PendingTurn extends ChatSendOptions {
  text: string;
}
export interface ChatSession {
  initialized: boolean;
  messages: ChatMessage[];
  stage: GuidedStage;
  incomeSelected: string | null;
  draft: string;
  pendingTurn: PendingTurn | null;
  isSending: boolean;
  isTyping: boolean;
  sendError: string | null;
}
function createSession(mode: KaupService["mode"]) {
  let snapshot: ChatSession = {
    initialized: false,
    messages: [],
    stage: mode === "mock" ? "intro" : "free-chat",
    incomeSelected: null,
    draft: "",
    pendingTurn: null,
    isSending: false,
    isTyping: false,
    sendError: null,
  };
  const listeners = new Set<() => void>();
  return {
    getSnapshot: () => snapshot,
    subscribe: (listener: () => void) => {
      listeners.add(listener);
      return () => {
        listeners.delete(listener);
      };
    },
    update: (
      patch:
        Partial<ChatSession> | ((current: ChatSession) => Partial<ChatSession>),
    ) => {
      snapshot = {
        ...snapshot,
        ...(typeof patch === "function" ? patch(snapshot) : patch),
      };
      listeners.forEach((listener) => listener());
    },
  };
}
// Lifetime is the service/provider session. Nothing is serialized or logged.
const sessions = new WeakMap<
  KaupService,
  Map<string, ReturnType<typeof createSession>>
>();
/** Ao sair da conta, as conversas em memória não podem ficar para a próxima pessoa. */
export function clearChatSessions(service: KaupService) {
  sessions.delete(service);
}
export function chatSession(service: KaupService, documentId: string) {
  let documents = sessions.get(service);
  if (!documents) {
    documents = new Map();
    sessions.set(service, documents);
  }
  let session = documents.get(documentId);
  if (!session) {
    session = createSession(service.mode);
    documents.set(documentId, session);
  }
  return session;
}
export function stageFromHistory(
  mode: KaupService["mode"],
  messages: ChatMessage[],
): GuidedStage {
  if (
    mode === "api" ||
    messages.some(
      (m) => m.intent === "income" && m.role === "ai" && m.kind === "text",
    )
  )
    return "free-chat";
  return messages.some((m) => m.intent === "income" && m.kind === "options")
    ? "waiting-income"
    : "intro";
}
export function appendMessages(
  messages: ChatMessage[],
  incoming: ChatMessage[],
) {
  const ids = new Set(messages.map((m) => m.id));
  return [...messages, ...incoming.filter((m) => !ids.has(m.id))];
}
