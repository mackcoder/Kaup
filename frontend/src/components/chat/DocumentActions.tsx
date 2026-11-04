import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useApp } from "../../app/AppProvider";
import { errorMessage } from "../../services";
import type { DocumentView } from "../../types/domain";
import Modal from "../Modal";
/**
 * Ações do documento aberto. Só o dono pode excluir; o administrador, quando abre o documento
 * de outra pessoa, vê apenas um aviso de somente leitura.
 */
export default function DocumentActions({ doc }: { doc: DocumentView }) {
  const { deleteDocument } = useApp();
  const navigate = useNavigate();
  const [confirming, setConfirming] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  if (doc.ownedByMe === false)
    return (
      <p className="readonly-note" role="note">
        👁️ Documento de outra pessoa: você só pode consultar.
      </p>
    );
  async function confirm() {
    setBusy(true);
    setError(null);
    try {
      await deleteDocument(doc.id);
      navigate("/documentos", { replace: true });
    } catch (e) {
      setError(errorMessage(e));
      setBusy(false);
    }
  }
  return (
    <>
      <button
        type="button"
        className="delete-doc-button"
        onClick={() => setConfirming(true)}
      >
        🗑️ Excluir documento
      </button>
      {confirming && (
        <Modal
          label="Excluir documento"
          onClose={() => !busy && setConfirming(false)}
        >
          <h2 className="sheet-title">Excluir este documento?</h2>
          <p className="sheet-subtitle">
            O documento e a conversa sobre ele serão apagados. Não dá para
            desfazer.
          </p>
          {error && (
            <p role="alert" className="feedback">
              {error}
            </p>
          )}
          <button
            className="primary-button danger wide"
            disabled={busy}
            onClick={() => void confirm()}
          >
            {busy ? "Excluindo..." : "Sim, excluir"}
          </button>
          <button
            className="secondary-button wide"
            disabled={busy}
            onClick={() => setConfirming(false)}
          >
            Não, voltar
          </button>
        </Modal>
      )}
    </>
  );
}
