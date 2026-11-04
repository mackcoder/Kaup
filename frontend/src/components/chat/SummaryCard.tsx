import type { DocumentView, DocumentAnalysis } from "../../types/domain";
// ── Summary card ──────────────────────────────────────────────────────────────
export default function SummaryCard({
  doc,
  analysis,
}: {
  doc: DocumentView;
  analysis: DocumentAnalysis | null;
}) {
  const isOk = doc.status === "ok";
  return (
    <div className="kaup-179">
      <div
        className="kaup-180"
        style={{
          background: isOk
            ? "linear-gradient(135deg,#ecfdf5,#d1fae5)"
            : "linear-gradient(135deg,#fffbeb,#fef3c7)",
          borderBottom: `1.5px solid ${isOk ? "#a7f3d0" : "#fde68a"}`,
        }}
      >
        <span className="kaup-181">{isOk ? "✅" : "⚠️"}</span>
        <div>
          <div
            className="kaup-182"
            style={{ color: isOk ? "#065f46" : "#92400e" }}
          >
            {analysis?.demo ? "ANÁLISE ILUSTRATIVA" : "ANÁLISE INICIAL"}
          </div>
          <div
            className="kaup-42"
            style={{ color: isOk ? "#059669" : "#d97706" }}
          >
            {analysis?.demo
              ? "Exemplo fictício"
              : analysis?.processingStatus === "completed"
                ? "Análise concluída"
                : "Aguardando análise"}
          </div>
        </div>
      </div>
      {analysis?.demo && analysis.exampleNotice && (
        <p className="demo-summary-notice" role="note">
          {analysis.exampleNotice}
        </p>
      )}
      {(analysis?.facts ?? []).map((row, i) => (
        <div key={i} className="kaup-183">
          <span className="kaup-184">{row.label}</span>
          <span className="kaup-178">{row.value}</span>
        </div>
      ))}
      <div className="kaup-185">
        <span className="kaup-186">💡</span>
        <span className="kaup-134" style={{ whiteSpace: "pre-line" }}>
          {analysis?.ocrQualityWarning ??
            (analysis?.demo && analysis.facts?.length
              ? analysis.keyPoints[0]?.description
              : analysis?.summary) ??
            "Resumo ainda não disponível."}
        </span>
      </div>
    </div>
  );
}
