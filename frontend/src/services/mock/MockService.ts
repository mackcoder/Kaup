import { FIXTURES } from "../../data/documents";
import {
  getContextFiles,
  getFreeTextResponse,
  INCOME_OPTIONS,
  INCOME_RESPONSES,
  DEMO_FACTS,
  DEMO_KEY_POINTS,
  DEMO_DISCLAIMER,
  demoIntroduction,
} from "../../data/chat";
import { presentDocument } from "../../styles/presenters";
import type { KaupService } from "../contracts/KaupService";
import type {
  ChatMessage,
  ChatSendOptions,
  ChatTurnResult,
  ContextFile,
  DocumentAnalysis,
  KaupDocument,
  UserProfile,
  AuthUser,
} from "../../types/domain";
const months = [
  "jan",
  "fev",
  "mar",
  "abr",
  "mai",
  "jun",
  "jul",
  "ago",
  "set",
  "out",
  "nov",
  "dez",
];
function parseDate(date: string) {
  const [d, m, y] = date.split(" ");
  return `${y}-${String(months.indexOf(m) + 1).padStart(2, "0")}-${d}T12:00:00Z`;
}
export class MockService implements KaupService {
  readonly mode = "mock" as const;
  private docs: KaupDocument[] = FIXTURES.flatMap((c) =>
    c.docs.map((d, i) => ({
      id: `${c.key.toLowerCase()}-${i + 1}`,
      ...d,
      categoryId: c.key,
      createdAt: parseDate(d.date),
      processingStatus: d.status === "pending" ? "pending" : "completed",
      demo: true,
    })),
  );
  private conversations = new Map<string, ChatMessage[]>();
  private completedTurns = new Map<string, ChatTurnResult>();
  private activeTurns = new Map<string, Promise<ChatTurnResult>>();
  private contexts = new Map<string, ContextFile[]>();
  private profile: UserProfile = {
    name: "João da Silva",
    email: "joao@email.com",
    fontSize: "large",
    explainLevel: "simple",
    income: "prefer_not",
    audio: false,
    shareIncomeWithAi: true,
  };
  // A demonstração não tem contas: há sempre alguém "logado" e sair é só uma simulação na tela.
  onUnauthorized: (() => void) | null = null;
  private demoUser: AuthUser = {
    id: "demo",
    name: "João da Silva",
    email: "joao@email.com",
    role: "USER",
  };
  async getSession() {
    return structuredClone(this.demoUser);
  }
  async register() {
    return structuredClone(this.demoUser);
  }
  async login() {
    return structuredClone(this.demoUser);
  }
  async logout() {}
  async deleteAccount() {}
  async listDocuments() {
    return structuredClone(this.docs);
  }
  async getDocument(id: string) {
    const doc = this.docs.find((d) => d.id === id);
    if (!doc) throw new Error("Documento não encontrado nesta sessão.");
    return structuredClone(doc);
  }
  async upload(file: File, categoryId: string | null, signal?: AbortSignal) {
    await new Promise<void>((resolve, reject) => {
      const t = setTimeout(resolve, 500);
      signal?.addEventListener(
        "abort",
        () => {
          clearTimeout(t);
          reject(new DOMException("Envio cancelado", "AbortError"));
        },
        { once: true },
      );
    });
    signal?.throwIfAborted();
    const doc: KaupDocument = {
      id: crypto.randomUUID(),
      name: file.name,
      description: "Documento enviado agora",
      emoji: file.type.startsWith("image/") ? "📷" : "📄",
      categoryId: categoryId ?? "Outros",
      status: "pending",
      processingStatus: "completed",
      createdAt: new Date().toISOString(),
      isNew: true,
      uploaded: true,
      demo: true,
    };
    this.docs.push(doc);
    return structuredClone(doc);
  }
  async removeDocument(id: string) {
    await this.getDocument(id);
    this.docs = this.docs.filter((d) => d.id !== id);
    this.conversations.delete(id);
    this.contexts.delete(id);
  }
  async getAnalysis(id: string): Promise<DocumentAnalysis> {
    const doc = await this.getDocument(id);
    return {
      documentId: id,
      processingStatus: "completed",
      summary: doc.uploaded
        ? "Arquivo recebido para demonstração. Nenhum texto foi extraído e nenhuma cláusula foi analisada."
        : "Resumo fictício do protótipo. Valores ilustrativos, sem avaliação jurídica ou financeira real.",
      keyPoints: structuredClone(DEMO_KEY_POINTS),
      facts: structuredClone(DEMO_FACTS),
      exampleNotice: doc.uploaded
        ? `Valores de um contrato fictício de exemplo. Não foram extraídos de “${doc.name}”.`
        : undefined,
      demo: true,
    };
  }
  async getMessages(id: string) {
    const doc = await this.getDocument(id);
    if (!this.conversations.has(id)) {
      const msg = (
        kind: ChatMessage["kind"],
        text?: string,
        options?: string[],
      ): ChatMessage => ({
        id: crypto.randomUUID(),
        documentId: id,
        role: "ai",
        kind,
        text,
        options,
        state: "sent",
      });
      this.conversations.set(id, [
        msg("text", demoIntroduction(doc.name, !!doc.uploaded)),
        msg("summary-card"),
        msg("text", "Qual é a sua renda mensal aproximada?"),
        { ...msg("options", undefined, [...INCOME_OPTIONS]), intent: "income" },
      ]);
    }
    return structuredClone(this.conversations.get(id) ?? []);
  }
  async sendMessage(
    id: string,
    text: string,
    signal?: AbortSignal,
    options?: ChatSendOptions,
  ): Promise<ChatTurnResult> {
    await this.getDocument(id);
    const turn = options ?? {
      turnId: crypto.randomUUID(),
      intent: "free-text" as const,
    };
    if (turn.intent === "income" && !INCOME_OPTIONS.includes(text))
      throw new Error("Escolha uma das opções de renda.");
    const key = `${id}:${turn.turnId}`;
    const completed = this.completedTurns.get(key);
    if (completed) return structuredClone(completed);
    const active = this.activeTurns.get(key);
    if (active) return structuredClone(await active);
    const sending = this.completeTurn(id, text, turn, signal);
    this.activeTurns.set(key, sending);
    try {
      return structuredClone(await sending);
    } finally {
      this.activeTurns.delete(key);
    }
  }
  private async completeTurn(
    id: string,
    text: string,
    turn: ChatSendOptions,
    signal?: AbortSignal,
  ): Promise<ChatTurnResult> {
    await this.getMessages(id);
    await waitForDemo(turn.intent === "income" ? 1800 : 1600, signal);
    const reply: ChatMessage = {
      id: `reply-${turn.turnId}`,
      documentId: id,
      role: "ai",
      kind: "text",
      text: `Exemplo fictício: ${turn.intent === "income" ? INCOME_RESPONSES[text] : getFreeTextResponse(text)}`,
      intent: turn.intent,
      turnId: turn.turnId,
      state: "sent",
      createdAt: new Date().toISOString(),
    };
    const followUps: ChatMessage[] =
      turn.intent === "income"
        ? [
            {
              id: `disclaimer-${turn.turnId}`,
              documentId: id,
              role: "ai",
              kind: "disclaimer",
              text: DEMO_DISCLAIMER,
              intent: "income",
              turnId: turn.turnId,
              state: "sent",
            },
          ]
        : [];
    const history = this.conversations.get(id) ?? [];
    history.push(
      {
        id: `user-${turn.turnId}`,
        documentId: id,
        role: "user",
        kind: "text",
        text,
        intent: turn.intent,
        turnId: turn.turnId,
        state: "sent",
      },
      reply,
      ...followUps,
    );
    this.conversations.set(id, history);
    const result = { reply, followUps };
    this.completedTurns.set(`${id}:${turn.turnId}`, result);
    return result;
  }
  async getContext(id: string) {
    const doc = await this.getDocument(id);
    return structuredClone(
      this.contexts.get(id) ??
        (doc.uploaded
          ? [
              {
                emoji: doc.emoji,
                name: doc.name,
                type: "Principal — demonstração",
                isMain: true,
              },
            ]
          : getContextFiles(presentDocument(doc))),
    );
  }
  async attachContext(id: string, file: File) {
    const files = await this.getContext(id);
    const entry = {
      emoji: "📑",
      name: file.name,
      type: "Anexo demonstrativo — sem leitura OCR",
    };
    this.contexts.set(id, [...files, entry]);
    return entry;
  }
  async getProfile() {
    return structuredClone(this.profile);
  }
  async updateProfile(profile: UserProfile) {
    this.profile = structuredClone(profile);
    return this.getProfile();
  }
  async health() {
    return "Modo mock: nenhum backend conectado.";
  }
}

function waitForDemo(ms: number, signal?: AbortSignal) {
  return new Promise<void>((resolve, reject) => {
    if (signal?.aborted) {
      reject(signal.reason);
      return;
    }
    const cancel = () => {
      clearTimeout(timer);
      reject(signal?.reason);
    };
    const timer = setTimeout(() => {
      signal?.removeEventListener("abort", cancel);
      resolve();
    }, ms);
    signal?.addEventListener("abort", cancel, { once: true });
  });
}
