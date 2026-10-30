import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  useCallback,
  type ReactNode,
} from "react";
import type { KaupService } from "../services/contracts/KaupService";
import type {
  AuthUser,
  RegisterInput,
  KaupDocument,
  UserProfile,
  DocumentView,
  CategoryView,
} from "../types/domain";
import { createService, errorMessage } from "../services";
import { clearChatSessions } from "../features/chat/session";
import { CATEGORY_STYLES } from "../styles/categories";
import { presentDocument } from "../styles/presenters";
const emptyProfile: UserProfile = {
  name: "",
  email: "",
  fontSize: "large",
  explainLevel: "simple",
  income: "prefer_not",
  audio: false,
  shareIncomeWithAi: true,
  retentionDays: 0,
};
interface AppState {
  service: KaupService;
  documents: DocumentView[];
  categories: CategoryView[];
  loading: boolean;
  error: string | null;
  profileError: string | null;
  profile: UserProfile;
  profileLoading: boolean;
  /** Quem está logado (null: ninguém; no modo demonstração há sempre alguém). */
  user: AuthUser | null;
  authLoading: boolean;
  /** Falha ao descobrir se há sessão (ex.: backend fora do ar); não é "não logado". */
  authError: string | null;
  login: (email: string, password: string) => Promise<void>;
  register: (input: RegisterInput) => Promise<void>;
  logout: () => Promise<void>;
  deleteAccount: () => Promise<void>;
  deleteDocument: (id: string) => Promise<void>;
  reload: () => void;
  refreshDocuments: () => Promise<void>;
  acceptDocument: (d: KaupDocument) => void;
  /** Salva só o que mudou; assim duas alterações seguidas não desfazem uma à outra. */
  saveProfile: (patch: Partial<UserProfile>) => Promise<boolean>;
}
const Context = createContext<AppState | null>(null);
export function AppProvider({
  children,
  service: provided,
}: {
  children: ReactNode;
  service?: KaupService;
}) {
  const [service] = useState(() => provided ?? createService());
  const [docs, setDocs] = useState<KaupDocument[]>([]);
  const [profile, setProfile] = useState<UserProfile>(emptyProfile);
  const [loading, setLoading] = useState(true);
  const [profileLoading, setProfileLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [profileError, setProfileError] = useState<string | null>(null);
  const [attempt, setAttempt] = useState(0);
  const [user, setUser] = useState<AuthUser | null>(null);
  const [authLoading, setAuthLoading] = useState(true);
  const [authError, setAuthError] = useState<string | null>(null);
  // Descobre se já há sessão (ex.: a pessoa recarregou a página). 401 é "não logado", não erro.
  useEffect(() => {
    const c = new AbortController();
    setAuthLoading(true);
    // Sessão expirada no meio do uso: volta para a tela de login.
    service.onUnauthorized = () => setUser(null);
    service
      .getSession(c.signal)
      .then((u) => {
        if (c.signal.aborted) return;
        setUser(u);
        setAuthError(null);
      })
      .catch((e) => {
        if (c.signal.aborted) return;
        setUser(null);
        setAuthError(errorMessage(e));
      })
      .finally(() => {
        if (!c.signal.aborted) setAuthLoading(false);
      });
    return () => {
      c.abort();
      service.onUnauthorized = null;
    };
  }, [service, attempt]);
  const userId = user?.id ?? null;
  useEffect(() => {
    // Sem ninguém logado não há o que carregar; ao entrar, o efeito roda de novo.
    if (!userId) {
      setLoading(false);
      setProfileLoading(false);
      return;
    }
    const c = new AbortController();
    setLoading(true);
    setProfileLoading(true);
    service
      .listDocuments(c.signal)
      .then((d) => {
        if (!c.signal.aborted) {
          setDocs(d);
          setError(null);
        }
      })
      .catch((e) => {
        if (!c.signal.aborted) setError(errorMessage(e));
      })
      .finally(() => {
        if (!c.signal.aborted) setLoading(false);
      });
    service
      .getProfile(c.signal)
      .then((p) => {
        if (!c.signal.aborted) {
          setProfile(p);
          setProfileError(null);
        }
      })
      .catch((e) => {
        if (!c.signal.aborted) setProfileError(errorMessage(e));
      })
      .finally(() => {
        if (!c.signal.aborted) setProfileLoading(false);
      });
    return () => c.abort();
  }, [service, attempt, userId]);
  const refreshDocuments = useCallback(async () => {
    try {
      setDocs(await service.listDocuments());
      setError(null);
    } catch (e) {
      setError(errorMessage(e));
    }
  }, [service]);
  const acceptDocument = useCallback(
    (d: KaupDocument) =>
      setDocs((old) => [...old.filter((v) => v.id !== d.id), d]),
    [],
  );
  // Ao entrar ou sair, nada da pessoa anterior (documentos, perfil, conversas) pode sobrar na tela.
  function esquecerTudo() {
    clearChatSessions(service);
    setDocs([]);
    setProfile(emptyProfile);
    setError(null);
    setProfileError(null);
  }
  async function login(email: string, password: string) {
    const u = await service.login(email, password);
    esquecerTudo();
    setUser(u);
  }
  async function register(input: RegisterInput) {
    const u = await service.register(input);
    esquecerTudo();
    setUser(u);
  }
  async function logout() {
    await service.logout();
    esquecerTudo();
    setUser(null);
  }
  async function deleteAccount() {
    await service.deleteAccount();
    esquecerTudo();
    setUser(null);
  }
  async function deleteDocument(id: string) {
    await service.removeDocument(id);
    setDocs((old) => old.filter((d) => d.id !== id));
  }
  async function saveProfile(patch: Partial<UserProfile>) {
    try {
      const p = await service.updateProfile(patch);
      setProfile(p);
      setProfileError(null);
      return true;
    } catch (e) {
      setProfileError(errorMessage(e));
      return false;
    }
  }
  const documents = useMemo(() => docs.map(presentDocument), [docs]);
  const categories = useMemo(
    () =>
      CATEGORY_STYLES.map((c) => ({
        ...c,
        docs: documents.filter((d) => d.catKey === c.key),
      })),
    [documents],
  );
  return (
    <Context.Provider
      value={{
        service,
        documents,
        categories,
        loading,
        error,
        profile,
        profileError,
        profileLoading,
        user,
        authLoading,
        authError,
        login,
        register,
        logout,
        deleteAccount,
        deleteDocument,
        reload: () => setAttempt((n) => n + 1),
        refreshDocuments,
        acceptDocument,
        saveProfile,
      }}
    >
      {children}
    </Context.Provider>
  );
}
export function useApp() {
  const ctx = useContext(Context);
  if (!ctx) throw new Error("AppProvider ausente.");
  return ctx;
}
