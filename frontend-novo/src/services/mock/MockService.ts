import { FIXTURES } from "../../data/documents";
import {
  getContextFiles,
  getFreeTextResponse,
  INCOME_OPTIONS,
  INCOME_RESPONSES,
} from "../../data/chat";
import { presentDocument } from "../../styles/presenters";
import type { KaupService } from "../contracts/KaupService";
import type {
  ChatMessage,
  ContextFile,
  DocumentAnalysis,
  KaupDocument,
  UserProfile,
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
  private contexts = new Map<string, ContextFile[]>();
  private profile: UserProfile = {
    name: "João da Silva",
    email: "joao@email.com",
    fontSize: "large",
    explainLevel: "simple",
    income: "prefer_not",
    audio: false,
  };
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
      keyPoints: doc.uploaded
        ? []
        : [
            {
              id: "demo-alert",
              title: "Ponto de atenção fictício",
              description: "Existem juros e encargos que merecem atenção.",
              severity: "warning",
            },
          ],
      facts: doc.uploaded
        ? []
        : [
            { label: "Parcela mensal", value: "R$ 1.250,00" },
            { label: "Prazo", value: "48 meses" },
            { label: "Valor total estimado", value: "R$ 60.000,00" },
          ],
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
      this.conversations.set(
        id,
        doc.uploaded
          ? [
              msg(
                "text",
                `Recebi “${doc.name}”. Esta é uma demonstração: não li o conteúdo do arquivo e não executei OCR.`,
              ),
              msg("summary-card"),
              msg(
                "text",
                "Você pode testar o chat com perguntas. As respostas serão ilustrativas.",
              ),
            ]
          : [
              msg(
                "text",
                "Analisei seu contrato. Encontrei alguns pontos importantes e preciso de uma informação para avaliar se as parcelas comprometem muito da sua renda.",
              ),
              msg("summary-card"),
              msg("text", "Qual é a sua renda mensal aproximada?"),
              msg("options", undefined, [
                ...INCOME_OPTIONS,
                "Prefiro não informar",
              ]),
            ],
      );
    }
    return structuredClone(this.conversations.get(id) ?? []);
  }
  async sendMessage(id: string, text: string, signal?: AbortSignal) {
    const doc = await this.getDocument(id);
    const history = await this.getMessages(id);
    await new Promise<void>((resolve, reject) => {
      const t = setTimeout(resolve, 1600);
      signal?.addEventListener(
        "abort",
        () => {
          clearTimeout(t);
          reject(new DOMException("Cancelado", "AbortError"));
        },
        { once: true },
      );
    });
    signal?.throwIfAborted();
    const reply: ChatMessage = {
      id: crypto.randomUUID(),
      documentId: id,
      role: "ai",
      kind: "text",
      text: doc.uploaded
        ? "Demonstração: não tenho acesso ao texto deste arquivo. Quando o backend implementar OCR e análise, poderei responder com base no documento."
        : `Exemplo fictício: ${INCOME_RESPONSES[text] ?? getFreeTextResponse(text)}`,
      state: "sent",
      createdAt: new Date().toISOString(),
    };
    history.push(
      {
        id: crypto.randomUUID(),
        documentId: id,
        role: "user",
        kind: "text",
        text,
        state: "sent",
      },
      reply,
    );
    if (INCOME_OPTIONS.includes(text) || text === "Prefiro não informar")
      history.push({
        id: crypto.randomUUID(),
        documentId: id,
        role: "ai",
        kind: "disclaimer",
        text: "⚖️ Esta análise é demonstrativa e não substitui orientação jurídica ou financeira profissional.",
      });
    this.conversations.set(id, history);
    return structuredClone(reply);
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
