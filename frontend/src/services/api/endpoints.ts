/** Caminhos do backend (ver backend/src/main/java/com/kaup/controller). */
export const endpoints = {
  health: "/api/analise/health",
  session: "/api/auth/eu",
  login: "/api/auth/entrar",
  register: "/api/auth/cadastro",
  logout: "/api/auth/sair",
  resetRequest: "/api/auth/recuperar/solicitar",
  resetConfirm: "/api/auth/recuperar/redefinir",
  account: "/api/conta",
  export: "/api/conta/exportar",
  documents: "/api/documentos",
  profile: "/api/perfil",
  document: (id: string) => `/api/documentos/${encodeURIComponent(id)}`,
  analysis: (id: string) => `/api/documentos/${encodeURIComponent(id)}/analise`,
  context: (id: string) =>
    `/api/documentos/${encodeURIComponent(id)}/contexto`,
  messages: (id: string) =>
    `/api/documentos/${encodeURIComponent(id)}/chat/mensagens`,
};
