import type { RegisterInput, ResetPasswordInput } from "../../types/domain";
// Mesmas regras do backend (ContaService): a tela avisa antes, o servidor confere de novo.
export const PASSWORD_MIN = 8;
export const PASSWORD_MAX = 72;
export const NAME_MAX = 100;
export function isValidEmail(email: string) {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim());
}
/** Primeira mensagem de erro do cadastro, ou null se estiver tudo certo. */
export function validateRegistration(input: RegisterInput): string | null {
  const name = input.name.trim();
  if (!name) return "Informe seu nome.";
  if (name.length > NAME_MAX)
    return `O nome pode ter até ${NAME_MAX} caracteres.`;
  if (!isValidEmail(input.email)) return "Informe um e-mail válido.";
  if (input.password.length < PASSWORD_MIN)
    return `A senha precisa ter pelo menos ${PASSWORD_MIN} caracteres.`;
  if (input.password.length > PASSWORD_MAX)
    return `A senha pode ter até ${PASSWORD_MAX} caracteres.`;
  if (input.password !== input.confirmPassword)
    return "As senhas não conferem.";
  return null;
}
/** Primeira mensagem de erro de "esqueci minha senha" (passo 2), ou null se estiver tudo certo. */
export function validatePasswordReset(
  input: ResetPasswordInput,
): string | null {
  if (!isValidEmail(input.email)) return "Informe um e-mail válido.";
  if (!input.code.trim()) return "Informe o código recebido.";
  if (input.password.length < PASSWORD_MIN)
    return `A senha precisa ter pelo menos ${PASSWORD_MIN} caracteres.`;
  if (input.password.length > PASSWORD_MAX)
    return `A senha pode ter até ${PASSWORD_MAX} caracteres.`;
  if (input.password !== input.confirmPassword)
    return "As senhas não conferem.";
  return null;
}
