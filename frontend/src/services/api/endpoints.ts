/** Caminhos do backend (ver backend/src/main/java/com/kaup/controller). */
export const endpoints = {
  health: "/api/analise/health",
  session: "/api/auth/eu",
  login: "/api/auth/entrar",
  register: "/api/auth/cadastro",
  logout: "/api/auth/sair",
  account: "/api/conta",
  documents: "/api/documentos",
  profile: "/api/perfil",
  document: (id: string) => `/api/documentos/${encodeURIComponent(id)}`,
  analysis: (id: string) => `/api/documentos/${encodeURIComponent(id)}/analise`,
  messages: (id: string) =>
    `/api/documentos/${encodeURIComponent(id)}/chat/mensagens`,
};
