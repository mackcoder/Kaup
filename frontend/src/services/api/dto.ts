import type {
  DocumentAnalysis,
  KaupDocument,
  ProcessingStatus,
  UserProfile,
  ChatMessage,
} from "../../types/domain";
import { ApiError } from "./http";
export interface DocumentDTO {
  id: string;
  name: string;
  categoryId: string | null;
  processingStatus: ProcessingStatus;
  createdAt: string;
  description?: string;
  visualStatus?: "ok" | "warning" | "pending";
}
export interface ChatReplyDTO {
  id: string;
  documentId: string;
  role: "assistant" | "user";
  content: string;
  createdAt: string;
}
export interface ApiErrorDTO {
  code: string;
  message: string;
  requestId?: string;
}
export function record(value: unknown): Record<string, unknown> {
  if (!value || typeof value !== "object" || Array.isArray(value))
    throw new ApiError(
      "Formato de resposta incompatível com o contrato proposto.",
    );
  return value as Record<string, unknown>;
}
export function string(value: unknown): string {
  if (typeof value !== "string")
    throw new ApiError("Campo textual ausente ou inválido na resposta da API.");
  return value;
}
export function array(value: unknown): unknown[] {
  if (!Array.isArray(value))
    throw new ApiError("A API deve retornar uma lista.");
  return value;
}
export function processing(value: unknown): ProcessingStatus {
  if (
    value !== "pending" &&
    value !== "processing" &&
    value !== "completed" &&
    value !== "failed"
  )
    throw new ApiError("Status de processamento inválido.");
  return value;
}
export function documentFromDTO(value: unknown): KaupDocument {
  const d = record(value);
  const state = processing(d.processingStatus);
  const createdAt = string(d.createdAt);
  if (Number.isNaN(Date.parse(createdAt)))
    throw new ApiError("Data inválida na resposta da API.");
  return {
    id: string(d.id),
    name: string(d.name),
    categoryId: d.categoryId === null ? null : string(d.categoryId),
    processingStatus: state,
    createdAt,
    description:
      typeof d.description === "string" ? d.description : "Documento enviado",
    emoji: "📄",
    status:
      d.visualStatus === "ok" ||
      d.visualStatus === "warning" ||
      d.visualStatus === "pending"
        ? d.visualStatus
        : state === "failed"
          ? "warning"
          : "pending",
  };
}
export function analysisFromDTO(value: unknown): DocumentAnalysis {
  const a = record(value);
  return {
    documentId: string(a.documentId),
    processingStatus: processing(a.processingStatus),
    summary: a.summary === null ? null : string(a.summary),
    keyPoints: array(a.keyPoints).map((v) => {
      const p = record(v);
      if (
        p.severity !== "info" &&
        p.severity !== "warning" &&
        p.severity !== "critical"
      )
        throw new ApiError("Severidade inválida.");
      return {
        id: string(p.id),
        title: string(p.title),
        description: string(p.description),
        severity: p.severity,
      };
    }),
    ocrQualityWarning:
      typeof a.ocrQualityWarning === "string" ? a.ocrQualityWarning : null,
  };
}
export function messageFromDTO(value: unknown): ChatMessage {
  const m = record(value);
  if (m.role !== "assistant" && m.role !== "user")
    throw new ApiError("Papel de mensagem inválido.");
  return {
    id: string(m.id),
    documentId: string(m.documentId),
    role: m.role === "assistant" ? "ai" : "user",
    kind: "text",
    text: string(m.content),
    createdAt: string(m.createdAt),
    state: "sent",
  };
}
export function profileFromDTO(value: unknown): UserProfile {
  const p = record(value);
  if (
    !["small", "medium", "large"].includes(String(p.fontSize)) ||
    !["simple", "normal", "complete"].includes(String(p.explainLevel)) ||
    !["less3", "3to5", "5to7", "7to15", "more15", "prefer_not"].includes(
      String(p.income),
    ) ||
    typeof p.audio !== "boolean"
  )
    throw new ApiError("Preferências incompatíveis com o contrato de perfil.");
  return {
    name: string(p.name),
    email: string(p.email),
    fontSize: p.fontSize as UserProfile["fontSize"],
    explainLevel: p.explainLevel as UserProfile["explainLevel"],
    income: p.income as UserProfile["income"],
    audio: p.audio,
  };
}
