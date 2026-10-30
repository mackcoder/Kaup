import type { KaupService } from "../contracts/KaupService";
import type {
  UserProfile,
  ContextFile,
  RegisterInput,
  ResetPasswordInput,
} from "../../types/domain";
import { createHttp, ApiError } from "./http";
import { endpoints as e } from "./endpoints";
import {
  array,
  documentFromDTO,
  analysisFromDTO,
  messageFromDTO,
  contextFileFromDTO,
  profileFromDTO,
  authUserFromDTO,
  string,
} from "./dto";
export class ApiService implements KaupService {
  readonly mode = "api" as const;
  onUnauthorized: (() => void) | null = null;
  private request: ReturnType<typeof createHttp>;
  constructor(baseUrl = "", timeoutMs = 20000) {
    this.request = createHttp(baseUrl, timeoutMs, () =>
      this.onUnauthorized?.(),
    );
  }
  async getSession(signal?: AbortSignal) {
    try {
      return authUserFromDTO(await this.request(e.session, { signal }));
    } catch (error) {
      // 401 aqui não é erro: só significa que ninguém está logado.
      if (error instanceof ApiError && error.status === 401) return null;
      throw error;
    }
  }
  async register(input: RegisterInput, signal?: AbortSignal) {
    return authUserFromDTO(
      await this.request(e.register, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(input),
        signal,
      }),
    );
  }
  async login(email: string, password: string, signal?: AbortSignal) {
    return authUserFromDTO(
      await this.request(e.login, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
        signal,
      }),
    );
  }
  async logout(signal?: AbortSignal) {
    await this.request(e.logout, { method: "POST", signal });
  }
  async requestPasswordReset(email: string, signal?: AbortSignal) {
    await this.request(e.resetRequest, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email }),
      signal,
    });
  }
  async resetPassword(input: ResetPasswordInput, signal?: AbortSignal) {
    await this.request(e.resetConfirm, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(input),
      signal,
    });
  }
  async exportData(signal?: AbortSignal) {
    return await this.request(e.export, { signal });
  }
  async deleteAccount(signal?: AbortSignal) {
    await this.request(e.account, { method: "DELETE", signal });
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
    // The server defines the conversation; no local income prompt or disclaimer.
    return { reply: m, followUps: [] };
  }
  async getContext(id: string, signal?: AbortSignal) {
    const [doc, anexos] = await Promise.all([
      this.getDocument(id, signal),
      this.request(e.context(id), { signal }),
    ]);
    return [
      { emoji: "📄", name: doc.name, type: "Principal", isMain: true },
      ...this.anexosDe(id, anexos),
    ];
  }
  async attachContext(id: string, file: File, signal?: AbortSignal) {
    const body = new FormData();
    body.append("file", file);
    const [anexo] = this.anexosDe(
      id,
      [await this.request(e.context(id), { method: "POST", body, signal })],
    );
    return anexo;
  }
  // Os anexos de outro documento nunca entram na lista: o id da URL manda.
  private anexosDe(id: string, value: unknown): ContextFile[] {
    return array(value).map((item) => {
      const { documentId, ...anexo } = contextFileFromDTO(item);
      if (documentId !== id)
        throw new ApiError("Anexo recebido para outro documento.");
      return anexo;
    });
  }
  async getProfile(signal?: AbortSignal) {
    return profileFromDTO(await this.request(e.profile, { signal }));
  }
  async updateProfile(patch: Partial<UserProfile>, signal?: AbortSignal) {
    return profileFromDTO(
      await this.request(e.profile, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(patch),
        signal,
      }),
    );
  }
  async health(signal?: AbortSignal) {
    return string(await this.request(e.health, { signal }, true));
  }
}
