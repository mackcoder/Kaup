import { useEffect, useRef, useState, type ChangeEvent } from "react";
import { useNavigate } from "react-router-dom";
import { useApp } from "../app/AppProvider";
import { ACCEPTED_FILES, validateFile } from "../features/documents/upload";
import type { UploadState } from "../types/domain";
import { errorMessage } from "../services";
import Modal from "./Modal";
export default function UploadDialog({
  onClose,
  categoryId = null,
  contextId,
  initialCapture = false,
  onAttached,
}: {
  onClose: () => void;
  categoryId?: string | null;
  contextId?: string;
  initialCapture?: boolean;
  onAttached?: () => void;
}) {
  const { service, acceptDocument, categories } = useApp();
  const navigate = useNavigate();
  const [file, setFile] = useState<File | null>(null),
    [preview, setPreview] = useState<string | null>(null),
    [error, setError] = useState<string | null>(null),
    [state, setState] = useState<UploadState>("idle");
  const fileRef = useRef<HTMLInputElement>(null),
    photoRef = useRef<HTMLInputElement>(null),
    request = useRef<AbortController | null>(null);
  const cat = categories.find((c) => c.key === categoryId);
  const confirmTitle = contextId
    ? "Adicionar ao contexto"
    : "Confirmar documento";
  useEffect(() => {
    if (initialCapture) photoRef.current?.click();
  }, [initialCapture]);
  useEffect(() => {
    if (!file?.type.startsWith("image/")) {
      setPreview(null);
      return;
    }
    const url = URL.createObjectURL(file);
    setPreview(url);
    return () => URL.revokeObjectURL(url);
  }, [file]);
  useEffect(() => () => request.current?.abort(), []);
  function choose(e: ChangeEvent<HTMLInputElement>) {
    const candidate = e.target.files?.[0];
    e.target.value = "";
    if (!candidate) return;
    const message = validateFile(candidate);
    setError(message);
    if (message) return;
    setFile(candidate);
    setState("confirming");
  }
  function close() {
    request.current?.abort();
    onClose();
  }
  async function submit() {
    if (!file || state === "uploading") return;
    const controller = new AbortController();
    request.current = controller;
    setState("uploading");
    setError(null);
    try {
      if (contextId) {
        await service.attachContext(contextId, file, controller.signal);
        if (controller.signal.aborted) return;
        onAttached?.();
        close();
      } else {
        const doc = await service.upload(file, categoryId, controller.signal);
        if (controller.signal.aborted) return;
        acceptDocument(doc);
        setState("success");
        onClose();
        navigate(`/documentos/${encodeURIComponent(doc.id)}/chat`);
      }
    } catch (e) {
      if (!controller.signal.aborted) {
        setError(errorMessage(e));
        setState("failed");
      }
    }
  }
  return (
    <Modal label={file ? confirmTitle : "Como deseja enviar?"} onClose={close}>
      {cat && (
        <div
          className="category-pill"
          style={{
            color: cat.color,
            background: cat.colorBg,
            borderColor: cat.colorLight,
          }}
        >
          {cat.emoji} {cat.label}
        </div>
      )}
      <h2 className="sheet-title">
        {file ? confirmTitle : "Como deseja enviar?"}
      </h2>
      <p className="sheet-subtitle">
        {file
          ? "Confira se o texto está nítido e completo."
          : "Escolha uma opção abaixo"}
      </p>
      {error && (
        <p className="feedback" role="alert">
          {error}
        </p>
      )}
      {file ? (
        <>
          {preview && (
            <img
              className="upload-preview"
              src={preview}
              alt="Prévia do documento selecionado"
            />
          )}
          <p className="file-name">
            📄 {file.name} · {(file.size / 1024).toFixed(1)} KiB
          </p>
          <p className="demo-copy">
            {service.mode === "mock"
              ? "Demonstração: o arquivo não sai do navegador. Não haverá OCR nem análise real."
              : contextId
                ? "O texto do arquivo é lido no servidor e passa a ser usado nas próximas respostas do chat."
                : "O arquivo é lido no servidor para gerar o resumo. Isso pode levar alguns segundos."}
          </p>
          <button
            className="primary-button wide"
            disabled={state === "uploading"}
            onClick={() => void submit()}
          >
            {state === "uploading"
              ? "Enviando..."
              : state === "failed"
                ? "Tentar novamente"
                : "Confirmar e enviar"}
          </button>
          <button
            className="secondary-button wide"
            disabled={state === "uploading"}
            onClick={() => {
              setFile(null);
              setState("idle");
              setError(null);
            }}
          >
            Escolher outra foto ou arquivo
          </button>
          <button className="secondary-button wide" onClick={close}>
            {state === "uploading" ? "Cancelar envio" : "Cancelar"}
          </button>
        </>
      ) : (
        <div className="upload-options">
          <button
            className="upload-option photo"
            onClick={() => photoRef.current?.click()}
          >
            <span>📷</span>
            <div>
              <strong>Tirar uma foto</strong>
              <small>Aponte a câmera para o documento</small>
            </div>
          </button>
          <button
            className="upload-option file"
            onClick={() => fileRef.current?.click()}
          >
            <span>📁</span>
            <div>
              <strong>Escolher arquivo</strong>
              <small>PDF, imagem ou documento</small>
            </div>
          </button>
          <small className="demo-copy">PDF, PNG ou JPEG, até 10 MiB.</small>
        </div>
      )}
      <input
        aria-label="Escolher arquivo"
        ref={fileRef}
        type="file"
        accept={ACCEPTED_FILES}
        onChange={choose}
        hidden
      />
      <input
        aria-label="Capturar foto"
        ref={photoRef}
        type="file"
        accept="image/png,image/jpeg"
        capture="environment"
        onChange={choose}
        hidden
      />
    </Modal>
  );
}
