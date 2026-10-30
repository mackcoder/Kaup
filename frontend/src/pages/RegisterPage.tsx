import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { useApp } from "../app/AppProvider";
import { errorMessage } from "../services";
import {
  PASSWORD_MIN,
  validateRegistration,
} from "../features/auth/validation";
import AuthLayout from "../components/auth/AuthLayout";
import { PasswordField, TextField } from "../components/auth/Fields";
export default function RegisterPage() {
  const { register } = useApp();
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  async function submit(event: FormEvent) {
    event.preventDefault();
    if (busy) return;
    const input = {
      name: name.trim(),
      email: email.trim(),
      password,
      confirmPassword,
    };
    const problem = validateRegistration(input);
    if (problem) {
      setError(problem);
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await register(input);
    } catch (e) {
      setError(errorMessage(e));
      setBusy(false);
    }
  }
  return (
    <AuthLayout
      title="Criar conta"
      subtitle="Leva um minuto. Seus contratos ficam só na sua conta."
    >
      <form className="auth-card" onSubmit={(e) => void submit(e)} noValidate>
        {error && (
          <p role="alert" className="auth-error">
            {error}
          </p>
        )}
        <TextField
          id="cadastro-nome"
          label="Seu nome"
          value={name}
          onChange={setName}
          autoComplete="name"
        />
        <TextField
          id="cadastro-email"
          label="Seu e-mail"
          type="email"
          value={email}
          onChange={setEmail}
          autoComplete="username"
        />
        <PasswordField
          id="cadastro-senha"
          label="Crie uma senha"
          value={password}
          onChange={setPassword}
          autoComplete="new-password"
          hint={`Pelo menos ${PASSWORD_MIN} caracteres.`}
        />
        <PasswordField
          id="cadastro-confirmacao"
          label="Repita a senha"
          value={confirmPassword}
          onChange={setConfirmPassword}
          autoComplete="new-password"
        />
        <button className="primary-button wide" type="submit" disabled={busy}>
          {busy ? "Criando conta..." : "Criar conta"}
        </button>
      </form>
      <p className="auth-switch">
        Já tem conta? <Link to="/entrar">Entrar</Link>
      </p>
    </AuthLayout>
  );
}
