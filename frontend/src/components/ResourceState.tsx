export default function ResourceState({
  error,
  loading,
  onRetry,
}: {
  error?: string | null;
  loading?: boolean;
  onRetry?: () => void;
}) {
  return (
    <div className="resource-state" role={error ? "alert" : "status"}>
      <span className="resource-icon">{error ? "⚠️" : "⏳"}</span>
      <p>
        {error ?? (loading ? "Carregando..." : "Nenhum documento encontrado.")}
      </p>
      {error && onRetry && (
        <button className="primary-button" onClick={onRetry}>
          Tentar novamente
        </button>
      )}
    </div>
  );
}
