export default function ResourceState({
  error,
  loading,
  message,
  onRetry,
}: {
  error?: string | null;
  loading?: boolean;
  /** Texto no lugar do "Carregando..." padrão. */
  message?: string;
  onRetry?: () => void;
}) {
  return (
    <div className="resource-state" role={error ? "alert" : "status"}>
      <span className="resource-icon">{error ? "⚠️" : "⏳"}</span>
      <p>
        {error ??
          (loading
            ? (message ?? "Carregando...")
            : "Nenhum documento encontrado.")}
      </p>
      {error && onRetry && (
        <button className="primary-button" onClick={onRetry}>
          Tentar novamente
        </button>
      )}
    </div>
  );
}
