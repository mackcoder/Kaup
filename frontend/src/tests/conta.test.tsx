import { describe, it, expect, vi, afterEach } from "vitest";
import { cleanup, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ApiError } from "../services/api/http";
import type { AuthUser, KaupDocument } from "../types/domain";
import { admin, ana, documento, montar, servico } from "./apiHelpers";

afterEach(() => cleanup());

describe("perfil e conta (modo API)", () => {
  it("o interruptor da renda salva o bloqueio no perfil", async () => {
    const service = servico(ana);
    const u = userEvent.setup();
    montar(service, "/perfil");
    const chave = await screen.findByRole("switch", {
      name: /Deixar a IA usar minha renda/,
    });
    expect(chave).toHaveAttribute("aria-checked", "true");
    await u.click(chave);
    await waitFor(() =>
      expect(service.updateProfile).toHaveBeenCalledWith(
        expect.objectContaining({ shareIncomeWithAi: false }),
      ),
    );
    await waitFor(() =>
      expect(
        screen.getByRole("switch", { name: /Deixar a IA usar minha renda/ }),
      ).toHaveAttribute("aria-checked", "false"),
    );
  });
  it("duas alterações seguidas não desfazem uma à outra (cada uma envia só o que mudou)", async () => {
    const service = servico(ana);
    const u = userEvent.setup();
    montar(service, "/perfil");
    await screen.findByRole("switch", { name: /Deixar a IA usar minha renda/ });
    // Dois cliques rápidos: o segundo parte de uma tela que ainda não viu o resultado do primeiro.
    await u.click(screen.getByRole("button", { name: /Completa/ }));
    await u.click(
      screen.getByRole("button", { name: /R\$ 3\.000 a R\$ 5\.000/ }),
    );
    await waitFor(() => expect(service.updateProfile).toHaveBeenCalledTimes(2));
    expect(service.updateProfile).toHaveBeenNthCalledWith(1, {
      explainLevel: "complete",
    });
    expect(service.updateProfile).toHaveBeenNthCalledWith(2, {
      income: "3to5",
    });
    // O que ficou no servidor tem as duas mudanças.
    await waitFor(async () => {
      const salvo = await vi.mocked(service.updateProfile).mock.results[1]
        .value;
      expect(salvo).toMatchObject({ explainLevel: "complete", income: "3to5" });
    });
  });
  it("só o administrador vê o aviso de que enxerga tudo", async () => {
    montar(servico(admin), "/perfil");
    expect(
      await screen.findByText(/Administrador: você vê os documentos de todos/),
    ).toBeInTheDocument();
    cleanup();
    montar(servico(ana), "/perfil");
    await screen.findByRole("switch", { name: /Deixar a IA usar minha renda/ });
    expect(screen.queryByText(/Administrador:/)).not.toBeInTheDocument();
  });
  it("sair encerra a sessão no servidor e volta para a tela de entrar", async () => {
    const service = servico(ana);
    vi.spyOn(service, "logout").mockResolvedValue();
    const u = userEvent.setup();
    montar(service, "/perfil");
    await u.click(await screen.findByRole("button", { name: /Sair da conta/ }));
    await u.click(await screen.findByRole("button", { name: "Sim, sair" }));
    expect(
      await screen.findByRole("heading", { name: "Entrar" }),
    ).toBeInTheDocument();
    expect(service.logout).toHaveBeenCalledTimes(1);
  });
  it("apagar a conta chama o servidor e volta para a tela de entrar", async () => {
    const service = servico(ana);
    vi.spyOn(service, "deleteAccount").mockResolvedValue();
    const u = userEvent.setup();
    montar(service, "/perfil");
    await u.click(
      await screen.findByRole("button", { name: /Apagar minha conta/ }),
    );
    await u.click(await screen.findByRole("button", { name: /Sim, apagar/ }));
    expect(
      await screen.findByRole("heading", { name: "Entrar" }),
    ).toBeInTheDocument();
    expect(service.deleteAccount).toHaveBeenCalledTimes(1);
  });
  it("conta de administrador: mostra o motivo da recusa e continua logado", async () => {
    const service = servico(admin);
    vi.spyOn(service, "deleteAccount").mockRejectedValue(
      new ApiError("A conta de administrador não pode ser excluída.", 403),
    );
    const u = userEvent.setup();
    montar(service, "/perfil");
    await u.click(
      await screen.findByRole("button", { name: /Apagar minha conta/ }),
    );
    await u.click(await screen.findByRole("button", { name: /Sim, apagar/ }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "A conta de administrador não pode ser excluída.",
    );
    expect(
      screen.queryByRole("heading", { name: "Entrar" }),
    ).not.toBeInTheDocument();
  });
});

describe("excluir documento (modo API)", () => {
  function comDocumento(sessao: AuthUser, doc: KaupDocument) {
    const service = servico(sessao, [doc]);
    vi.spyOn(service, "getDocument").mockResolvedValue(doc);
    vi.spyOn(service, "getAnalysis").mockResolvedValue({
      documentId: doc.id,
      processingStatus: "completed",
      summary: "Resumo.",
      keyPoints: [],
    });
    vi.spyOn(service, "getMessages").mockResolvedValue([]);
    vi.spyOn(service, "getContext").mockResolvedValue([]);
    return service;
  }
  it("o dono confirma, o documento é apagado e a pessoa volta para a lista", async () => {
    const service = comDocumento(ana, documento);
    const remover = vi.spyOn(service, "removeDocument").mockResolvedValue();
    const u = userEvent.setup();
    montar(service, "/documentos/real/chat");
    await u.click(
      await screen.findByRole("button", { name: /Excluir documento/ }),
    );
    expect(
      screen.getByText(/O documento e a conversa sobre ele serão apagados/),
    ).toBeInTheDocument();
    await u.click(screen.getByRole("button", { name: "Sim, excluir" }));
    expect(
      await screen.findByRole("textbox", { name: "Buscar documento" }),
    ).toBeInTheDocument();
    expect(remover).toHaveBeenCalledWith("real");
  });
  it("cancelar não apaga nada", async () => {
    const service = comDocumento(ana, documento);
    const remover = vi.spyOn(service, "removeDocument").mockResolvedValue();
    const u = userEvent.setup();
    montar(service, "/documentos/real/chat");
    await u.click(
      await screen.findByRole("button", { name: /Excluir documento/ }),
    );
    await u.click(screen.getByRole("button", { name: "Não, voltar" }));
    expect(remover).not.toHaveBeenCalled();
    expect(
      screen.queryByText(/O documento e a conversa sobre ele/),
    ).not.toBeInTheDocument();
  });
  it("erro do servidor aparece na confirmação e o documento continua", async () => {
    const service = comDocumento(ana, documento);
    vi.spyOn(service, "removeDocument").mockRejectedValue(
      new ApiError("Documento não encontrado.", 404),
    );
    const u = userEvent.setup();
    montar(service, "/documentos/real/chat");
    await u.click(
      await screen.findByRole("button", { name: /Excluir documento/ }),
    );
    await u.click(screen.getByRole("button", { name: "Sim, excluir" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Documento não encontrado.",
    );
  });
  it("administrador no documento de outra pessoa: só um aviso, sem botão de excluir", async () => {
    const service = comDocumento(admin, { ...documento, ownedByMe: false });
    montar(service, "/documentos/real/chat");
    expect(
      await screen.findByText(
        /Documento de outra pessoa: você só pode consultar/,
      ),
    ).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: /Excluir documento/ }),
    ).not.toBeInTheDocument();
  });
});
