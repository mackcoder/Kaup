import { CATEGORY_STYLES } from "./categories";
import type { DocumentView, KaupDocument } from "../types/domain";
export function presentDocument(doc: KaupDocument): DocumentView {
  const cat =
    CATEGORY_STYLES.find((c) => c.key === doc.categoryId) ??
    CATEGORY_STYLES[CATEGORY_STYLES.length - 1];
  const date = new Date(doc.createdAt)
    .toLocaleDateString("pt-BR", {
      day: "2-digit",
      month: "short",
      year: "numeric",
      timeZone: "America/Sao_Paulo",
    })
    .replaceAll(".", "")
    .replaceAll(" de ", " ");
  return {
    ...doc,
    date,
    catKey: cat.key,
    catLabel: cat.label,
    catEmoji: cat.emoji,
    catColor: cat.color,
    catColorLight: cat.colorLight,
    catColorBg: cat.colorBg,
  };
}
