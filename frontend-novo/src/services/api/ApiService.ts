import type { KaupService } from "../contracts/KaupService";
import type { UserProfile, ContextFile } from "../../types/domain";
import { createHttp, ApiError } from "./http";
import { endpoints as e } from "./endpoints";
import {
  array,
  documentFromDTO,
  analysisFromDTO,
  messageFromDTO,
  profileFromDTO,
  string,
} from "./dto";
export class ApiService implements KaupService {
  readonly mode = "api" as const;
  private request: ReturnType<typeof createHttp>;
  constructor(baseUrl = "", timeoutMs = 20000) {
    this.request = createHttp(baseUrl, timeoutMs);
  }
  async listDocuments(signal?: AbortSignal) {
    return array(await this.request(e.documents, { signal })).map(
      documentFromDTO,
    );
  }
  async getDocument(id: string, signal?: AbortSignal) {
    return documentFromDTO(await this.request(e.document(id), { signal }));
  }
  async upload(file: File, categoryId: string | null, signal?: AbortSignal) {
    const body = new FormData();
    body.append("file", file);
    if (categoryId) body.append("categoryId", categoryId);
    return documentFromDTO(
      await this.request(e.documents, { method: "POST", body, signal }),
    );
  }
  async removeDocument(id: string, signal?: AbortSignal) {
    await this.request(e.document(id), { method: "DELETE", signal });
  }
  async getAnalysis(id: string, signal?: AbortSignal) {
    const a = analysisFromDTO(await this.request(e.analysis(id), { signal }));
    if (a.documentId !== id)
      throw new ApiError("Análise recebida para outro documento.");
    return a;
  }
  async getMessages(id: string, signal?: AbortSignal) {
    const messages = array(await this.request(e.messages(id), { signal })).map(
      messageFromDTO,
    );
    if (messages.some((m) => m.documentId !== id))
      throw new ApiError("Histórico recebido para outro documento.");
    return messages;
  }
  async sendMessage(id: string, text: string, signal?: AbortSignal) {
    const m = messageFromDTO(
      await this.request(e.messages(id), {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ message: text }),
        signal,
      }),
    );
    if (m.documentId !== id || m.role !== "ai")
      throw new ApiError("Resposta de chat incompatível com o documento.");
    return m;
  }
  async getContext(id: string, signal?: AbortSignal) {
    const doc = await this.getDocument(id, signal);
    return [{ emoji: "📄", name: doc.name, type: "Principal", isMain: true }];
  }
  async attachContext(): Promise<ContextFile> {
    throw new ApiError(
      "Anexar contexto depende de um contrato backend ainda não definido. Nenhum arquivo foi enviado.",
    );
  }
  async getProfile(signal?: AbortSignal) {
    return profileFromDTO(await this.request(e.profile, { signal }));
  }
  async updateProfile(profile: UserProfile, signal?: AbortSignal) {
    return profileFromDTO(
      await this.request(e.profile, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(profile),
        signal,
      }),
    );
  }
  async health(signal?: AbortSignal) {
    return string(await this.request(e.health, { signal }, true));
  }
}
