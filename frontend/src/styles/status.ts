export const STATUS = {
  ok: {
    emoji: "✅",
    label: "Aprovado",
    color: "#059669",
    bg: "#d1fae5",
    border: "#a7f3d0",
  },
  warning: {
    emoji: "⚠️",
    label: "Atenção",
    color: "#d97706",
    bg: "#fef3c7",
    border: "#fde68a",
  },
  pending: {
    emoji: "🕐",
    label: "Aguardando",
    color: "#6366f1",
    bg: "#eef2ff",
    border: "#c7d2fe",
  },
};

/** Selos dos documentos que ainda não têm resumo: sendo lidos pelo servidor, ou que não puderam ser lidos. */
const READING = {
  emoji: "🕐",
  label: "Lendo…",
  color: "#6366f1",
  bg: "#eef2ff",
  border: "#c7d2fe",
};
const FAILED = {
  emoji: "❌",
  label: "Não deu para ler",
  color: "#dc2626",
  bg: "#fee2e2",
  border: "#fecaca",
};
/** O selo a mostrar: o da leitura em andamento ou da falha têm prioridade sobre o resumo. */
export function statusOf(doc: {
  status: keyof typeof STATUS;
  processingStatus: string;
}) {
  if (doc.processingStatus === "failed") return FAILED;
  if (
    doc.processingStatus === "pending" ||
    doc.processingStatus === "processing"
  )
    return READING;
  return STATUS[doc.status];
}
