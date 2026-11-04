import { useEffect, type ReactNode } from "react";
import {
  Link,
  Navigate,
  Route,
  Routes,
  useLocation,
  useNavigate,
  useParams,
  useSearchParams,
} from "react-router-dom";
import { useApp } from "./AppProvider";
import HomePage from "../pages/HomePage";
import CategoryPage from "../pages/CategoryPage";
import DocumentsPage from "../pages/DocumentsPage";
import ChatPage from "../pages/ChatPage";
import ProfilePage from "../pages/ProfilePage";
import LoginPage from "../pages/LoginPage";
import RegisterPage from "../pages/RegisterPage";
import ForgotPasswordPage from "../pages/ForgotPasswordPage";
import ResourceState from "../components/ResourceState";
import type { DocumentView, FilterKey } from "../types/domain";
function useActions() {
  const navigate = useNavigate();
  const location = useLocation();
  return {
    back: () =>
      window.history.state?.idx > 0
        ? navigate(-1)
        : navigate("/", { replace: true }),
    open: (d: DocumentView) =>
      navigate(`/documentos/${encodeURIComponent(d.id)}/chat`, {
        state: { from: location.pathname },
      }),
    all: (filter: FilterKey = "all") =>
      navigate(`/documentos?filtro=${filter}`),
  };
}
function Guard({ children }: { children: ReactNode }) {
  const { loading, error, reload } = useApp();
  return loading || error ? (
    <ResourceState loading={loading} error={error} onRetry={reload} />
  ) : (
    children
  );
}
function CategoryRoute() {
  const { categoryId } = useParams();
  const { categories } = useApp();
  const { back, open } = useActions();
  const cat = categories.find((c) => c.key === categoryId);
  return (
    <Guard>
      {cat ? (
        <CategoryPage cat={cat} onBack={back} onOpenDoc={open} />
      ) : (
        <NotFound />
      )}
    </Guard>
  );
}
function DocumentsRoute() {
  const [params, setParams] = useSearchParams();
  const { back, open } = useActions();
  const raw = params.get("filtro");
  const filter: FilterKey =
    raw === "ok" || raw === "warning" || raw === "pending" ? raw : "all";
  const search = params.get("busca") ?? "";
  const update = (key: string, value: string) => {
    setParams(
      (old) => {
        const p = new URLSearchParams(old);
        if (value) p.set(key, value);
        else p.delete(key);
        return p;
      },
      { replace: true },
    );
  };
  return (
    <Guard>
      <DocumentsPage
        onReset={() => setParams({}, { replace: true })}
        onBack={back}
        onOpenDoc={open}
        filter={filter}
        setFilter={(v) => update("filtro", v)}
        search={search}
        setSearch={(v) => update("busca", v)}
      />
    </Guard>
  );
}
function ProfileRoute() {
  const { profileLoading, profileError, profile, reload } = useApp();
  const { back, all } = useActions();
  if (profileLoading || (profileError && !profile.name))
    return (
      <ResourceState
        loading={profileLoading}
        error={profileError}
        onRetry={reload}
      />
    );
  return <ProfilePage onBack={back} onOpenAllDocs={all} />;
}
function NotFound() {
  return (
    <div className="resource-state">
      <span className="resource-icon">📄</span>
      <h1>Página não encontrada</h1>
      <p>Este endereço ou categoria não existe.</p>
      <Link className="primary-button" to="/">
        Ir para o início
      </Link>
    </div>
  );
}
export default function App() {
  const { service, error, loading, reload, user, authLoading, authError } =
    useApp();
  const location = useLocation();
  useEffect(() => {
    window.scrollTo(0, 0);
  }, [location.pathname]);
  const modeLabel = (
    <div className="mode-label">
      {service.mode === "mock"
        ? "Demonstração · dados fictícios"
        : "Modo API · integração Spring Boot"}
    </div>
  );
  // Ainda descobrindo se há sessão, ou sem conseguir falar com o servidor: não é "deslogado".
  if (authLoading || authError)
    return (
      <>
        {modeLabel}
        <ResourceState
          loading={authLoading}
          error={authError}
          onRetry={reload}
        />
      </>
    );
  // Sem login só existem as telas de conta. O endereço pedido é mantido: depois de entrar,
  // a pessoa continua onde queria (ex.: um link direto para um documento).
  if (!user)
    return (
      <>
        {modeLabel}
        <Routes>
          <Route path="/cadastro" element={<RegisterPage />} />
          <Route path="/esqueci-senha" element={<ForgotPasswordPage />} />
          <Route path="*" element={<LoginPage />} />
        </Routes>
      </>
    );
  return (
    <>
      {modeLabel}
      <Routes>
        <Route path="/entrar" element={<Navigate to="/" replace />} />
        <Route path="/cadastro" element={<Navigate to="/" replace />} />
        <Route path="/esqueci-senha" element={<Navigate to="/" replace />} />
        <Route
          path="/"
          element={
            <>
              {(loading || error) && (
                <ResourceState
                  loading={loading}
                  error={error}
                  onRetry={reload}
                />
              )}
              <HomePage />
            </>
          }
        />
        <Route path="/categorias/:categoryId" element={<CategoryRoute />} />
        <Route path="/documentos" element={<DocumentsRoute />} />
        <Route path="/documentos/:documentId/chat" element={<ChatPage />} />
        <Route path="/perfil" element={<ProfileRoute />} />
        <Route path="*" element={<NotFound />} />
      </Routes>
    </>
  );
}
