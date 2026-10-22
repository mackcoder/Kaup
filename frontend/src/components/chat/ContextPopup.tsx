import type { DocumentView, ContextFile } from "../../types/domain";
export default function ContextPopup({
  doc,
  files,
  onClose,
  onAddFile,
}: {
  doc: DocumentView;
  files: ContextFile[];
  onClose: () => void;
  onAddFile: () => void;
}) {
  return (
    <>
      <div onClick={onClose} className="kaup-141" />
      <div className="kaup-142" role="dialog" aria-label="Contexto da análise">
        <div className="kaup-143">
          <div>
            <div className="kaup-83">Contexto da análise</div>
            <div className="kaup-144">
              {files.length} {files.length === 1 ? "arquivo" : "arquivos"}{" "}
              utilizados
            </div>
          </div>
          <button
            aria-label="Fechar contexto"
            onClick={onClose}
            className="kaup-145"
          >
            ✕
          </button>
        </div>
        <div className="kaup-146">
          {files.map((f, i) => (
            <div
              key={i}
              className="kaup-147"
              style={{
                background: f.isMain ? doc.catColorBg + "80" : "transparent",
              }}
            >
              <div
                className="kaup-148"
                style={{
                  background: f.isMain ? doc.catColorBg : "#f1f5f9",
                  border: f.isMain
                    ? `1.5px solid ${doc.catColorLight}50`
                    : "1.5px solid #e2e8f0",
                }}
              >
                {f.emoji}
              </div>
              <div className="kaup-25">
                <div className="kaup-149">{f.name}</div>
                <div className="kaup-150">
                  {f.type}
                  {f.isMain && (
                    <span
                      className="kaup-151"
                      style={{
                        background: doc.catColorBg,
                        color: doc.catColor,
                      }}
                    >
                      Principal
                    </span>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
        <div className="kaup-152">
          <button
            onClick={onAddFile}
            className="kaup-153"
            style={{
              background: doc.catColorBg,
              border: `1.5px dashed ${doc.catColorLight}`,
            }}
          >
            <span className="kaup-41">➕</span>
            <span className="kaup-42" style={{ color: doc.catColor }}>
              Adicionar arquivo ao contexto
            </span>
          </button>
        </div>
      </div>
    </>
  );
}
