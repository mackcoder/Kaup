/** Only health exists today. All other paths are PROPOSED contracts. */
export const endpoints = {
  health: "/api/analise/health",
  documents: "/api/documentos",
  profile: "/api/perfil",
  document: (id: string) => `/api/documentos/${encodeURIComponent(id)}`,
  analysis: (id: string) => `/api/documentos/${encodeURIComponent(id)}/analise`,
  messages: (id: string) =>
    `/api/documentos/${encodeURIComponent(id)}/chat/mensagens`,
};
