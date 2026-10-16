import { useState } from "react";
import { useApp } from "../../app/AppProvider";
import { countDocuments } from "../documents/selectors";
import { profileStyles, FONT_SCALE } from "../../components/profile/styles";
import type {
  FontSize,
  ExplainLevel,
  IncomeRange,
  FilterKey,
} from "../../types/domain";
export function useProfile({
  onBack,
  onOpenAllDocs,
}: {
  onBack: () => void;
  onOpenAllDocs: (filter?: FilterKey) => void;
}) {
  const { profile, saveProfile, documents, service, profileError } = useApp();
  const { name, email, fontSize, explainLevel, income } = profile;
  const setFontSize = (fontSize: FontSize) =>
    void saveProfile({ ...profile, fontSize });
  const setExplainLevel = (explainLevel: ExplainLevel) =>
    void saveProfile({ ...profile, explainLevel });
  const setIncome = (income: IncomeRange) =>
    void saveProfile({ ...profile, income });
  const [editName, setEditName] = useState(name);
  const [editEmail, setEditEmail] = useState(email);
  const [showEdit, setShowEdit] = useState(false);
  const [editError, setEditError] = useState("");
  const [savedMsg, setSavedMsg] = useState(false);

  // Audio simulation
  const [audioKey, setAudioKey] = useState<string | null>(null);

  // Modals
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
  const [deleteSimulated, setDeleteSimulated] = useState(false);
  const [showLogoutConfirm, setShowLogoutConfirm] = useState(false);
  const [loggedOut, setLoggedOut] = useState(false);
  const [helpMsg, setHelpMsg] = useState<string | null>(null);

  // Tap feedback
  const [tapped, setTapped] = useState<string | null>(null);

  const scale = FONT_SCALE[fontSize];
  const fs = (base: number) => base * scale;

  function tap(key: string, cb?: () => void) {
    setTapped(key);
    setTimeout(() => {
      setTapped(null);
      cb?.();
    }, 140);
  }

  function playAudio(key: string) {
    setHelpMsg("Áudio demonstrativo: a narração ainda não está implementada.");
    setAudioKey(key);
    setTimeout(() => setAudioKey(null), 2000);
  }

  async function handleSave() {
    if (
      !editName.trim() ||
      !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(editEmail.trim())
    ) {
      setEditError("Informe nome e e-mail válidos.");
      return;
    }
    if (
      !(await saveProfile({
        ...profile,
        name: editName.trim(),
        email: editEmail.trim(),
      }))
    )
      return;
    setShowEdit(false);
    setSavedMsg(true);
    setTimeout(() => setSavedMsg(false), 2500);
  }

  const docStats = [
    {
      key: "ok" as const,
      emoji: "✅",
      label: "Aprovados",
      sub: `${countDocuments(documents).ok} documentos`,
      color: "#059669",
      bg: "#d1fae5",
      border: "#a7f3d0",
    },
    {
      key: "warning" as const,
      emoji: "⚠️",
      label: "Precisam de atenção",
      sub: `${countDocuments(documents).warning} documentos`,
      color: "#d97706",
      bg: "#fef3c7",
      border: "#fde68a",
    },
    {
      key: "pending" as const,
      emoji: "🕐",
      label: "Esperando análise",
      sub: `${countDocuments(documents).pending} documentos`,
      color: "#6366f1",
      bg: "#eef2ff",
      border: "#c7d2fe",
    },
  ];

  const explainOptions: {
    key: ExplainLevel;
    emoji: string;
    title: string;
    sub: string;
  }[] = [
    {
      key: "simple",
      emoji: "😊",
      title: "Bem simples",
      sub: "Frases pequenas e palavras fáceis.",
    },
    {
      key: "normal",
      emoji: "📘",
      title: "Normal",
      sub: "Explicações com um pouco mais de detalhes.",
    },
    {
      key: "complete",
      emoji: "🔎",
      title: "Completa",
      sub: "Mostra todos os detalhes do contrato.",
    },
  ];

  const incomeOptions: { key: IncomeRange; label: string }[] = [
    { key: "less3", label: "Menos de R$ 3.000" },
    { key: "3to5", label: "R$ 3.000 a R$ 5.000" },
    { key: "5to7", label: "R$ 5.000 a R$ 7.000" },
    { key: "7to15", label: "R$ 7.000 a R$ 15.000" },
    { key: "more15", label: "Mais de R$ 15.000" },
    { key: "prefer_not", label: "Prefiro não informar" },
  ];

  const privacyItems = [
    {
      key: "docs",
      emoji: "📂",
      title: "Cuidar dos meus documentos",
      sub: "Veja ou apague arquivos enviados.",
    },
    {
      key: "history",
      emoji: "🕘",
      title: "Ver minhas análises",
      sub: "Veja contratos analisados antes.",
    },
    {
      key: "download",
      emoji: "⬇️",
      title: "Baixar minhas informações",
      sub: "Guarde uma cópia dos seus dados.",
    },
    {
      key: "delete",
      emoji: "🗑️",
      title: "Apagar minha conta",
      sub: "Apague seus dados do aplicativo.",
      danger: true,
    },
  ];

  const helpItems = [
    {
      key: "video",
      emoji: "▶️",
      title: "Ver como usar",
      sub: "Assista a uma explicação simples.",
      msg: "▶️ Abrindo vídeo explicativo...",
    },
    {
      key: "audio",
      emoji: "🎧",
      title: "Ouvir ajuda",
      sub: "Escute como o aplicativo funciona.",
      msg: "🎧 Preparando o áudio de ajuda...",
    },
    {
      key: "person",
      emoji: "👥",
      title: "Falar com alguém",
      sub: "Peça ajuda para uma pessoa.",
      msg: "👥 Conectando com a equipe de suporte...",
    },
  ];

  const s = profileStyles(fs, audioKey);

  return {
    profileError,
    service,
    name,
    email,
    fontSize,
    explainLevel,
    income,
    setFontSize,
    setExplainLevel,
    setIncome,
    editName,
    setEditName,
    editEmail,
    setEditEmail,
    showEdit,
    setShowEdit,
    editError,
    setEditError,
    savedMsg,
    audioKey,
    showDeleteConfirm,
    setShowDeleteConfirm,
    deleteSimulated,
    setDeleteSimulated,
    showLogoutConfirm,
    setShowLogoutConfirm,
    loggedOut,
    setLoggedOut,
    helpMsg,
    setHelpMsg,
    tapped,
    fs,
    tap,
    playAudio,
    handleSave,
    docStats,
    explainOptions,
    incomeOptions,
    privacyItems,
    helpItems,
    s,
    onBack,
    onOpenAllDocs,
  };
}
export type ProfileModel = ReturnType<typeof useProfile>;
