import { useState } from "react";
interface FieldProps {
  id: string;
  label: string;
  value: string;
  onChange: (value: string) => void;
  autoComplete?: string;
  hint?: string;
}
export function TextField({
  type = "text",
  ...props
}: FieldProps & { type?: "text" | "email" }) {
  return (
    <div className="auth-field">
      <label htmlFor={props.id}>{props.label}</label>
      <input
        id={props.id}
        className="auth-input"
        type={type}
        value={props.value}
        autoComplete={props.autoComplete}
        onChange={(e) => props.onChange(e.target.value)}
      />
      {props.hint && <small>{props.hint}</small>}
    </div>
  );
}
/** Senha com botão para mostrar o que foi digitado (ajuda quem digita devagar ou erra). */
export function PasswordField(props: FieldProps) {
  const [shown, setShown] = useState(false);
  return (
    <div className="auth-field">
      <label htmlFor={props.id}>{props.label}</label>
      <div className="auth-password">
        <input
          id={props.id}
          className="auth-input"
          type={shown ? "text" : "password"}
          value={props.value}
          autoComplete={props.autoComplete}
          onChange={(e) => props.onChange(e.target.value)}
        />
        <button
          type="button"
          className="auth-toggle"
          aria-pressed={shown}
          aria-controls={props.id}
          aria-label={`${shown ? "Ocultar" : "Mostrar"} a senha digitada`}
          onClick={() => setShown((s) => !s)}
        >
          {shown ? "Ocultar" : "Mostrar"}
        </button>
      </div>
      {props.hint && <small>{props.hint}</small>}
    </div>
  );
}
