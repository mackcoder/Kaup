import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { useApp } from "../app/AppProvider";
import { errorMessage } from "../services";
import {
  PASSWORD_MIN,
  isValidEmail,
  validatePasswordReset,
} from "../features/auth/validation";
import AuthLayout from "../components/auth/AuthLayout";
import { PasswordField, TextField } from "../components/auth/Fields";
// Enquanto o e-mail não é enviado de verdade, o código é sempre o mesmo. Esta variável (vazia por
// padrão) só serve para a tela lembrar qual é, em ambiente de teste; nunca defina em produção.
const FAKE_CODE_HINT = import.meta.env.VITE_FAKE_RESET_CODE as
  string | undefined;
type Step = "email" | "code" | "done";
export default function ForgotPasswordPage() {
  const { service } = useApp();
  const [step, setStep] = useState<Step>("email");
  const [email, setEmail] = useState("");
  const [code, setCode] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  async function askCode(event: FormEvent) {
    event.preventDefault();
    if (busy) return;
    if (!isValidEmail(email)) {
      setError("Informe um e-mail válido.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await service.requestPasswordReset(email.trim());
      setStep("code");
    } catch (e) {
      setError(errorMessage(e));
    }
    setBusy(false);
  }
  async function changePassword(event: FormEvent) {
    event.preventDefault();
    if (busy) return;
    const input = {
      email: email.trim(),
      code: code.trim(),
      password,
      confirmPassword,
    };
    const problem = validatePasswordReset(input);
    if (problem) {
      setError(problem);
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await service.resetPassword(input);
      setStep("done");
    } catch (e) {
      // Código errado ou bloqueio de alguns segundos: a pessoa corrige e tenta de novo.
      setError(errorMessage(e));
      setCode("");
    }
    setBusy(false);
  }
  const alert = error && (
    <p role="alert" className="auth-error">
      {error}
    </p>
  );
  if (step === "done")
    return (
      <AuthLayout
        title="Senha alterada"
        subtitle="Pronto! Agora é só entrar com a sua nova senha."
      >
        <Link className="primary-button wide" to="/entrar">
          Ir para entrar
        </Link>
      </AuthLayout>
    );
  if (step === "code")
    return (
      <AuthLayout
        title="Digite o código"
        subtitle={`Enviamos um código para ${email.trim()}. Digite-o abaixo e crie uma nova senha.`}
      >
        <form
          className="auth-card"
          onSubmit={(e) => void changePassword(e)}
          noValidate
        >
          {FAKE_CODE_HINT && (
            <p className="auth-note">
              Versão de testes: o e-mail ainda não é enviado. O código é{" "}
              <strong>{FAKE_CODE_HINT}</strong>.
            </p>
          )}
          {alert}
          <TextField
            id="recuperar-codigo"
            label="Código recebido"
            value={code}
            onChange={setCode}
            autoComplete="one-time-code"
          />
          <PasswordField
            id="recuperar-senha"
            label="Crie uma nova senha"
            value={password}
            onChange={setPassword}
            autoComplete="new-password"
            hint={`Pelo menos ${PASSWORD_MIN} caracteres.`}
          />
          <PasswordField
            id="recuperar-confirmacao"
            label="Repita a nova senha"
            value={confirmPassword}
            onChange={setConfirmPassword}
            autoComplete="new-password"
          />
          <button className="primary-button wide" type="submit" disabled={busy}>
            {busy ? "Salvando..." : "Salvar nova senha"}
          </button>
        </form>
        <p className="auth-switch">
          <button
            type="button"
            className="auth-link"
            onClick={() => {
              setStep("email");
              setError(null);
            }}
          >
            Usar outro e-mail
          </button>
        </p>
      </AuthLayout>
    );
  return (
    <AuthLayout
      title="Esqueci minha senha"
      subtitle="Informe o e-mail da sua conta e enviaremos um código."
    >
      <form className="auth-card" onSubmit={(e) => void askCode(e)} noValidate>
        {alert}
        <TextField
          id="recuperar-email"
          label="Seu e-mail"
          type="email"
          value={email}
          onChange={setEmail}
          autoComplete="username"
        />
        <button className="primary-button wide" type="submit" disabled={busy}>
          {busy ? "Enviando..." : "Enviar código"}
        </button>
      </form>
      <p className="auth-switch">
        <Link to="/entrar">Voltar para entrar</Link>
      </p>
    </AuthLayout>
  );
}
