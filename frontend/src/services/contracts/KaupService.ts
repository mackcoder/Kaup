import type {
  AuthUser,
  RegisterInput,
  ResetPasswordInput,
  ChatMessage,
  ChatSendOptions,
  ChatTurnResult,
  ContextFile,
  DocumentAnalysis,
  KaupDocument,
  UserProfile,
} from "../../types/domain";
export interface KaupService {
  readonly mode: "mock" | "api";
  /** Chamado quando o servidor diz que a sessão acabou (401 fora do login). */
  onUnauthorized: (() => void) | null;
  /** Quem está logado, ou null se ninguém. */
  getSession(signal?: AbortSignal): Promise<AuthUser | null>;
  register(input: RegisterInput, signal?: AbortSignal): Promise<AuthUser>;
  login(
    email: string,
    password: string,
    signal?: AbortSignal,
  ): Promise<AuthUser>;
  logout(signal?: AbortSignal): Promise<void>;
  /** "Esqueci minha senha", passo 1. Responde igual para qualquer e-mail. */
  requestPasswordReset(email: string, signal?: AbortSignal): Promise<void>;
  /** Passo 2: troca a senha se o código confere. Não deixa ninguém logado. */
  resetPassword(input: ResetPasswordInput, signal?: AbortSignal): Promise<void>;
  /** Tudo o que o sistema guarda da conta (JSON), para a pessoa baixar. */
  exportData(signal?: AbortSignal): Promise<unknown>;
  /** Apaga a conta, os documentos e as conversas. */
  deleteAccount(signal?: AbortSignal): Promise<void>;
  listDocuments(signal?: AbortSignal): Promise<KaupDocument[]>;
  getDocument(id: string, signal?: AbortSignal): Promise<KaupDocument>;
  upload(
    file: File,
    categoryId: string | null,
    signal?: AbortSignal,
  ): Promise<KaupDocument>;
  removeDocument(id: string, signal?: AbortSignal): Promise<void>;
  getAnalysis(id: string, signal?: AbortSignal): Promise<DocumentAnalysis>;
  getMessages(id: string, signal?: AbortSignal): Promise<ChatMessage[]>;
  sendMessage(
    id: string,
    text: string,
    signal?: AbortSignal,
    options?: ChatSendOptions,
  ): Promise<ChatTurnResult>;
  getContext(id: string, signal?: AbortSignal): Promise<ContextFile[]>;
  attachContext(
    id: string,
    file: File,
    signal?: AbortSignal,
  ): Promise<ContextFile>;
  getProfile(signal?: AbortSignal): Promise<UserProfile>;
  /** Altera só os campos enviados e devolve o perfil completo, como o servidor ficou. */
  updateProfile(
    patch: Partial<UserProfile>,
    signal?: AbortSignal,
  ): Promise<UserProfile>;
  health(signal?: AbortSignal): Promise<string>;
}
