export type VisualStatus = "ok" | "warning" | "pending";
export type ProcessingStatus =
  "pending" | "processing" | "completed" | "failed";
export type FilterKey = "all" | VisualStatus;
export type FontSize = "small" | "medium" | "large";
export type ExplainLevel = "simple" | "normal" | "complete";
export type IncomeRange =
  "less3" | "3to5" | "5to7" | "7to15" | "more15" | "prefer_not";
export interface KaupDocument {
  id: string;
  name: string;
  description: string;
  emoji: string;
  categoryId: string | null;
  status: VisualStatus;
  processingStatus: ProcessingStatus;
  createdAt: string;
  isNew?: boolean;
  demo?: boolean;
  uploaded?: boolean;
}
export interface DocumentCategory {
  key: string;
  emoji: string;
  label: string;
  color: string;
  colorLight: string;
  colorBg: string;
  gradient: [string, string];
}
/** Presentation projection; domain/remote DTOs never depend on these colors. */
export interface DocumentView extends KaupDocument {
  date: string;
  catKey: string;
  catLabel: string;
  catEmoji: string;
  catColor: string;
  catColorLight: string;
  catColorBg: string;
}
export interface CategoryView extends DocumentCategory {
  docs: DocumentView[];
}
export interface DocumentAnalysis {
  documentId: string;
  processingStatus: ProcessingStatus;
  summary: string | null;
  keyPoints: {
    id: string;
    title: string;
    description: string;
    severity: "info" | "warning" | "critical";
  }[];
  ocrQualityWarning?: string | null;
  facts?: { label: string; value: string }[];
  demo?: boolean;
}
export interface ChatMessage {
  id: string;
  documentId: string;
  role: "ai" | "user";
  kind: "text" | "summary-card" | "options" | "disclaimer";
  text?: string;
  options?: string[];
  createdAt?: string;
  state?: "sent" | "sending" | "failed";
}
export interface ContextFile {
  emoji: string;
  name: string;
  type: string;
  isMain?: boolean;
}
export interface UserProfile {
  name: string;
  email: string;
  fontSize: FontSize;
  explainLevel: ExplainLevel;
  income: IncomeRange;
  audio: boolean;
}
export type UploadState =
  "idle" | "confirming" | "uploading" | "failed" | "success";
