import type { ReactNode } from "react";
/** Moldura das telas de entrar e criar conta. */
export default function AuthLayout({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle: string;
  children: ReactNode;
}) {
  return (
    <main className="auth-page">
      <div className="auth-brand" aria-hidden="true">
        📄
      </div>
      <p className="auth-app-name">Contrato Fácil</p>
      <h1 className="auth-title">{title}</h1>
      <p className="auth-subtitle">{subtitle}</p>
      {children}
    </main>
  );
}
