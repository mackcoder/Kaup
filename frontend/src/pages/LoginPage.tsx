import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { useApp } from "../app/AppProvider";
import { errorMessage } from "../services";
import AuthLayout from "../components/auth/AuthLayout";
import { PasswordField, TextField } from "../components/auth/Fields";
export default function LoginPage() {
  const { login } = useApp();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (busy) return;
    if (!email.trim() || !password) {
      setError("Informe seu e-mail e sua senha.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await login(email.trim(), password);
    } catch (e) {
      // Quem errou a senha digita de novo; o e-mail continua preenchido.
      setError(errorMessage(e));
      setPassword("");
      setBusy(false);
    }
  }
  return (
    <AuthLayout
      title="Entrar"
      subtitle="Entre para ver seus contratos e conversar sobre eles."
    >
      <form className="auth-card" onSubmit={(e) => void submit(e)} noValidate>
        {error && (
          <p role="alert" className="auth-error">
            {error}
          </p>
        )}
        <TextField
          id="login-email"
          label="Seu e-mail"
          type="email"
          value={email}
          onChange={setEmail}
          autoComplete="username"
        />
        <PasswordField
          id="login-senha"
          label="Sua senha"
          value={password}
          onChange={setPassword}
          autoComplete="current-password"
        />
        <button className="primary-button wide" type="submit" disabled={busy}>
          {busy ? "Entrando..." : "Entrar"}
        </button>
      </form>
      <p className="auth-switch">
        Ainda não tem conta? <Link to="/cadastro">Criar conta</Link>
      </p>
    </AuthLayout>
  );
}
