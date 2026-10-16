import type {
  ChatMessage,
  ContextFile,
  DocumentAnalysis,
  KaupDocument,
  UserProfile,
} from "../../types/domain";
export interface KaupService {
  readonly mode: "mock" | "api";
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
  ): Promise<ChatMessage>;
  getContext(id: string, signal?: AbortSignal): Promise<ContextFile[]>;
  attachContext(
    id: string,
    file: File,
    signal?: AbortSignal,
  ): Promise<ContextFile>;
  getProfile(signal?: AbortSignal): Promise<UserProfile>;
  updateProfile(
    profile: UserProfile,
    signal?: AbortSignal,
  ): Promise<UserProfile>;
  health(signal?: AbortSignal): Promise<string>;
}
