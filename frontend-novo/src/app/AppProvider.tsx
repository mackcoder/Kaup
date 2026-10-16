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
  KaupDocument,
  UserProfile,
  DocumentView,
  CategoryView,
} from "../types/domain";
import { createService, errorMessage } from "../services";
import { CATEGORY_STYLES } from "../styles/categories";
import { presentDocument } from "../styles/presenters";
const emptyProfile: UserProfile = {
  name: "",
  email: "",
  fontSize: "large",
  explainLevel: "simple",
  income: "prefer_not",
  audio: false,
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
  reload: () => void;
  refreshDocuments: () => Promise<void>;
  acceptDocument: (d: KaupDocument) => void;
  saveProfile: (profile: UserProfile) => Promise<boolean>;
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
  useEffect(() => {
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
  }, [service, attempt]);
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
  async function saveProfile(next: UserProfile) {
    try {
      const p = await service.updateProfile(next);
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
