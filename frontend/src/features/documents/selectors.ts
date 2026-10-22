import type { FilterKey, DocumentView, KaupDocument } from "../../types/domain";
export function countDocuments(docs: KaupDocument[]) {
  return {
    all: docs.length,
    ok: docs.filter((d) => d.status === "ok").length,
    warning: docs.filter((d) => d.status === "warning").length,
    pending: docs.filter((d) => d.status === "pending").length,
  };
}
export function filterDocuments(
  docs: DocumentView[],
  filter: FilterKey,
  search: string,
) {
  const q = search.trim().toLocaleLowerCase("pt-BR");
  return docs.filter(
    (d) =>
      (filter === "all" || d.status === filter) &&
      [d.name, d.catLabel, d.description].some((v) =>
        v.toLocaleLowerCase("pt-BR").includes(q),
      ),
  );
}
