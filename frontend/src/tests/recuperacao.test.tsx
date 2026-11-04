import { describe, it, expect, vi, afterEach } from "vitest";
import { cleanup, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ApiService } from "../services/api/ApiService";
import { ApiError } from "../services/api/http";
import { validatePasswordReset } from "../features/auth/validation";
import { ana, campo, montar, perfil, servico } from "./apiHelpers";

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe("regras do 'esqueci minha senha' (as mesmas do cadastro)", () => {
  const ok = {
    email: "ana@exemplo.com",
    code: "676869",
    password: "novasenha1",
    confirmPassword: "novasenha1",
  };
  it("aceita um pedido válido", () => {
    expect(validatePasswordReset(ok)).toBeNull();
  });
  it.each([
    [{ email: "ana" }, "Informe um e-mail válido."],
    [{ code: "  " }, "Informe o código recebido."],
    [
      { password: "curta", confirmPassword: "curta" },
      "A senha precisa ter pelo menos 8 caracteres.",
    ],
    [{ confirmPassword: "outra12345" }, "As senhas não conferem."],
  ])("recusa %j", (troca, mensagem) => {
    expect(validatePasswordReset({ ...ok, ...troca })).toBe(mensagem);
  });
});

describe("esqueci minha senha (modo API)", () => {
  async function abrir(service: ApiService) {
    const u = userEvent.setup();
    montar(service, "/esqueci-senha");
    await screen.findByRole("heading", { name: "Esqueci minha senha" });
    return u;
  }
  async function pedirCodigo(u: ReturnType<typeof userEvent.setup>) {
    await u.type(campo("Seu e-mail"), "ana@exemplo.com");
    await u.click(screen.getByRole("button", { name: "Enviar código" }));
    await screen.findByRole("heading", { name: "Digite o código" });
  }
  it("a tela de entrar tem o link e ele leva ao pedido de código", async () => {
    const u = userEvent.setup();
    montar(servico(null));
    await screen.findByRole("heading", { name: "Entrar" });
    await u.click(screen.getByRole("link", { name: "Esqueci minha senha" }));
    expect(
      await screen.findByRole("heading", { name: "Esqueci minha senha" }),
    ).toBeInTheDocument();
  });
  it("e-mail inválido não chama o servidor", async () => {
    const service = servico(null);
    const pedir = vi.spyOn(service, "requestPasswordReset");
    const u = await abrir(service);
    await u.type(campo("Seu e-mail"), "ana");
    await u.click(screen.getByRole("button", { name: "Enviar código" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Informe um e-mail válido.",
    );
    expect(pedir).not.toHaveBeenCalled();
  });
  it("fluxo completo: pede o código, troca a senha e manda para entrar (sem logar sozinha)", async () => {
    const service = servico(null);
    vi.spyOn(service, "requestPasswordReset").mockResolvedValue();
    vi.spyOn(service, "resetPassword").mockResolvedValue();
    const login = vi.spyOn(service, "login");
    const u = await abrir(service);
    await pedirCodigo(u);
    expect(service.requestPasswordReset).toHaveBeenCalledWith(
      "ana@exemplo.com",
    );
    await u.type(campo("Código recebido"), "676869");
    await u.type(campo("Crie uma nova senha"), "novasenha1");
    await u.type(campo("Repita a nova senha"), "novasenha1");
    await u.click(screen.getByRole("button", { name: "Salvar nova senha" }));
    expect(
      await screen.findByRole("heading", { name: "Senha alterada" }),
    ).toBeInTheDocument();
    expect(service.resetPassword).toHaveBeenCalledWith({
      email: "ana@exemplo.com",
      code: "676869",
      password: "novasenha1",
      confirmPassword: "novasenha1",
    });
    expect(login).not.toHaveBeenCalled();
    await u.click(screen.getByRole("link", { name: "Ir para entrar" }));
    expect(
      await screen.findByRole("heading", { name: "Entrar" }),
    ).toBeInTheDocument();
  });
  it("código errado: mostra a mensagem do servidor e limpa só o código", async () => {
    const service = servico(null);
    vi.spyOn(service, "requestPasswordReset").mockResolvedValue();
    vi.spyOn(service, "resetPassword").mockRejectedValue(
      new ApiError(
        "Código inválido. Confira o e-mail e o código e tente de novo.",
        400,
      ),
    );
    const u = await abrir(service);
    await pedirCodigo(u);
    await u.type(campo("Código recebido"), "111111");
    await u.type(campo("Crie uma nova senha"), "novasenha1");
    await u.type(campo("Repita a nova senha"), "novasenha1");
    await u.click(screen.getByRole("button", { name: "Salvar nova senha" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Código inválido.",
    );
    expect(campo("Código recebido")).toHaveValue("");
    expect(campo("Crie uma nova senha")).toHaveValue("novasenha1");
  });
  it("senhas diferentes: avisa na tela e não chama o servidor", async () => {
    const service = servico(null);
    vi.spyOn(service, "requestPasswordReset").mockResolvedValue();
    const trocar = vi.spyOn(service, "resetPassword");
    const u = await abrir(service);
    await pedirCodigo(u);
    await u.type(campo("Código recebido"), "676869");
    await u.type(campo("Crie uma nova senha"), "novasenha1");
    await u.type(campo("Repita a nova senha"), "outrasenha1");
    await u.click(screen.getByRole("button", { name: "Salvar nova senha" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "As senhas não conferem.",
    );
    expect(trocar).not.toHaveBeenCalled();
  });
  it("quem já está logado é levado ao início", async () => {
    montar(servico(ana), "/esqueci-senha");
    expect(
      await screen.findByRole("button", { name: /Enviar documento/ }),
    ).toBeInTheDocument();
  });
});

describe("bloqueio por muitas tentativas de login", () => {
  it("mostra a mensagem do servidor (429) e deixa tentar de novo", async () => {
    const service = servico(null);
    vi.spyOn(service, "login").mockRejectedValue(
      new ApiError(
        "Muitas tentativas. Aguarde alguns segundos e tente de novo.",
        429,
      ),
    );
    const u = userEvent.setup();
    montar(service);
    await screen.findByRole("heading", { name: "Entrar" });
    await u.type(campo("Seu e-mail"), "ana@exemplo.com");
    await u.type(campo("Sua senha"), "senha1234");
    await u.click(screen.getByRole("button", { name: "Entrar" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Muitas tentativas. Aguarde alguns segundos",
    );
    expect(screen.getByRole("button", { name: "Entrar" })).toBeEnabled();
  });
});

describe("chamadas ao backend", () => {
  function fetchOk(corpo?: unknown) {
    const fetcher = vi.fn(
      async () =>
        new Response(corpo === undefined ? null : JSON.stringify(corpo), {
          status: corpo === undefined ? 204 : 200,
        }),
    );
    vi.stubGlobal("fetch", fetcher);
    return fetcher;
  }
  it("recuperação envia POST com e-mail, código e senhas nos caminhos do contrato", async () => {
    const fetcher = fetchOk();
    const service = new ApiService();
    await service.requestPasswordReset("ana@exemplo.com");
    await service.resetPassword({
      email: "ana@exemplo.com",
      code: "676869",
      password: "novasenha1",
      confirmPassword: "novasenha1",
    });
    const [[url1, init1], [url2, init2]] = fetcher.mock.calls as unknown as [
      string,
      RequestInit,
    ][];
    expect(url1).toBe("/api/auth/recuperar/solicitar");
    expect(init1.method).toBe("POST");
    expect(JSON.parse(String(init1.body))).toEqual({
      email: "ana@exemplo.com",
    });
    expect(url2).toBe("/api/auth/recuperar/redefinir");
    expect(JSON.parse(String(init2.body))).toMatchObject({ code: "676869" });
  });
  it("exportar faz GET em /api/conta/exportar e devolve o JSON", async () => {
    const fetcher = fetchOk({ account: { email: "ana@exemplo.com" } });
    const dados = await new ApiService().exportData();
    expect((fetcher.mock.calls[0] as unknown as [string])[0]).toBe(
      "/api/conta/exportar",
    );
    expect(dados).toEqual({ account: { email: "ana@exemplo.com" } });
  });
  it("o perfil lê retentionDays (e ignora valores esquisitos)", async () => {
    const base = { ...perfil, retentionDays: undefined };
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValueOnce(
          new Response(JSON.stringify({ ...base, retentionDays: 180 })),
        )
        .mockResolvedValueOnce(
          new Response(JSON.stringify({ ...base, retentionDays: -3 })),
        )
        .mockResolvedValueOnce(new Response(JSON.stringify(base))),
    );
    const service = new ApiService();
    expect((await service.getProfile()).retentionDays).toBe(180);
    expect((await service.getProfile()).retentionDays).toBe(0);
    expect((await service.getProfile()).retentionDays).toBe(0);
  });
});

describe("baixar meus dados e aviso de prazo (perfil, modo API)", () => {
  function prepararDownload() {
    const criar = vi.fn(() => "blob:kaup");
    const revogar = vi.fn();
    vi.stubGlobal(
      "URL",
      Object.assign(URL, { createObjectURL: criar, revokeObjectURL: revogar }),
    );
    const nomes: string[] = [];
    vi.spyOn(HTMLAnchorElement.prototype, "click").mockImplementation(function (
      this: HTMLAnchorElement,
    ) {
      nomes.push(this.download);
    });
    return { criar, revogar, nomes };
  }
  it("o botão baixa o arquivo com os dados vindos do servidor", async () => {
    const { criar, revogar, nomes } = prepararDownload();
    const service = servico(ana);
    vi.spyOn(service, "exportData").mockResolvedValue({
      account: { email: "ana@exemplo.com" },
    });
    const u = userEvent.setup();
    montar(service, "/perfil");
    await u.click(
      await screen.findByRole("button", { name: /Baixar minhas informações/ }),
    );
    expect(await screen.findByRole("status")).toHaveTextContent(
      "kaup-meus-dados.json foi baixado",
    );
    expect(service.exportData).toHaveBeenCalledTimes(1);
    expect(nomes).toEqual(["kaup-meus-dados.json"]);
    expect(criar).toHaveBeenCalledTimes(1);
    const blob = (criar.mock.calls[0] as unknown as [Blob])[0];
    // O jsdom não tem Blob.text(): lê com FileReader.
    const texto = await new Promise<string>((resolve) => {
      const leitor = new FileReader();
      leitor.onload = () => resolve(String(leitor.result));
      leitor.readAsText(blob);
    });
    expect(JSON.parse(texto)).toEqual({
      account: { email: "ana@exemplo.com" },
    });
    expect(revogar).toHaveBeenCalledWith("blob:kaup");
  });
  it("falha ao exportar mostra o erro e não baixa nada", async () => {
    const { criar, nomes } = prepararDownload();
    const service = servico(ana);
    vi.spyOn(service, "exportData").mockRejectedValue(
      new ApiError("Não foi possível exportar agora.", 500),
    );
    const u = userEvent.setup();
    montar(service, "/perfil");
    await u.click(
      await screen.findByRole("button", { name: /Baixar minhas informações/ }),
    );
    expect(await screen.findByRole("status")).toHaveTextContent(
      "Não foi possível exportar agora.",
    );
    expect(criar).not.toHaveBeenCalled();
    expect(nomes).toEqual([]);
  });
  it("mostra o prazo de apagamento quando existe, e some quando é zero", async () => {
    montar(servico(ana), "/perfil");
    expect(
      await screen.findByText(/apagados depois de 180 dias/),
    ).toBeInTheDocument();
    cleanup();
    const sem = servico(ana);
    vi.spyOn(sem, "getProfile").mockResolvedValue({
      ...perfil,
      retentionDays: 0,
    });
    montar(sem, "/perfil");
    await screen.findByText(/Minha privacidade/);
    await waitFor(() =>
      expect(screen.queryByText(/apagados depois de/)).not.toBeInTheDocument(),
    );
  });
});
