import { render, screen } from "@testing-library/react";
import { vi } from "vitest";
import { MemoryRouter } from "react-router-dom";
import App from "../app/App";
import { AppProvider } from "../app/AppProvider";
import { ApiService } from "../services/api/ApiService";
import type { AuthUser, KaupDocument, UserProfile } from "../types/domain";

export const ana: AuthUser = {
  id: "u1",
  name: "Ana Souza",
  email: "ana@exemplo.com",
  role: "USER",
};
export const admin: AuthUser = {
  id: "u0",
  name: "Administrador",
  email: "admin@admin",
  role: "ADMIN",
};
export const perfil: UserProfile = {
  name: "Ana Souza",
  email: "ana@exemplo.com",
  fontSize: "large",
  explainLevel: "simple",
  income: "prefer_not",
  audio: false,
  shareIncomeWithAi: true,
  retentionDays: 180,
};
export const documento: KaupDocument = {
  id: "real",
  name: "Contrato.pdf",
  description: "Resumo do contrato.",
  emoji: "📄",
  categoryId: "Casa",
  status: "ok",
  processingStatus: "completed",
  createdAt: "2026-10-30T12:00:00Z",
  ownedByMe: true,
};

/** Serviço de API com tudo simulado; cada teste decide quem está logado. */
export function servico(sessao: AuthUser | null, docs: KaupDocument[] = []) {
  const service = new ApiService();
  vi.spyOn(service, "getSession").mockResolvedValue(sessao);
  vi.spyOn(service, "listDocuments").mockResolvedValue(docs);
  vi.spyOn(service, "getProfile").mockResolvedValue(perfil);
  // O "servidor" simulado junta o que chega ao que já tem, como o PATCH parcial do backend.
  let noServidor = { ...perfil };
  vi.spyOn(service, "updateProfile").mockImplementation(async (patch) => {
    await new Promise((r) => setTimeout(r, 40)); // latência: é ela que expõe a corrida
    noServidor = { ...noServidor, ...patch };
    return { ...noServidor };
  });
  return service;
}
export function montar(service: ApiService, caminho = "/") {
  return render(
    <MemoryRouter initialEntries={[caminho]}>
      <AppProvider service={service}>
        <App />
      </AppProvider>
    </MemoryRouter>,
  );
}
export const campo = (nome: string | RegExp) => screen.getByLabelText(nome);
