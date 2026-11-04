import { describe, it, expect, vi, afterEach } from "vitest";
import { act, cleanup, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ApiService } from "../services/api/ApiService";
import { ApiError } from "../services/api/http";
import { validateRegistration } from "../features/auth/validation";
import { ana, campo, montar, servico } from "./apiHelpers";

afterEach(() => cleanup());

describe("regras do cadastro (as mesmas do backend)", () => {
  const ok = {
    name: "Ana",
    email: "ana@exemplo.com",
    password: "senha1234",
    confirmPassword: "senha1234",
  };
  it("aceita um cadastro válido", () => {
    expect(validateRegistration(ok)).toBeNull();
  });
  it.each([
    [{ name: "  " }, "Informe seu nome."],
    [{ name: "a".repeat(101) }, "O nome pode ter até 100 caracteres."],
    [{ email: "ana@exemplo" }, "Informe um e-mail válido."],
    [{ email: "" }, "Informe um e-mail válido."],
    [
      { password: "curta", confirmPassword: "curta" },
      "A senha precisa ter pelo menos 8 caracteres.",
    ],
    [
      { password: "a".repeat(73), confirmPassword: "a".repeat(73) },
      "A senha pode ter até 72 caracteres.",
    ],
    [{ confirmPassword: "outra1234" }, "As senhas não conferem."],
  ])("recusa %j", (troca, mensagem) => {
    expect(validateRegistration({ ...ok, ...troca })).toBe(mensagem);
  });
});

describe("entrar (modo API)", () => {
  it("sem sessão mostra a tela de entrar e não carrega nada da pessoa", async () => {
    const service = servico(null);
    montar(service);
    expect(
      await screen.findByRole("heading", { name: "Entrar" }),
    ).toBeInTheDocument();
    expect(service.listDocuments).not.toHaveBeenCalled();
    expect(service.getProfile).not.toHaveBeenCalled();
    expect(screen.queryByText(/Enviar documento/)).not.toBeInTheDocument();
  });
  it("entrar com sucesso leva à tela inicial e carrega os documentos", async () => {
    const service = servico(null);
    vi.spyOn(service, "login").mockResolvedValue(ana);
    const u = userEvent.setup();
    montar(service);
    await screen.findByRole("heading", { name: "Entrar" });
    await u.type(campo("Seu e-mail"), "ana@exemplo.com");
    await u.type(campo("Sua senha"), "senha1234");
    await u.click(screen.getByRole("button", { name: "Entrar" }));
    expect(
      await screen.findByRole("button", { name: /Enviar documento/ }),
    ).toBeInTheDocument();
    expect(service.login).toHaveBeenCalledWith("ana@exemplo.com", "senha1234");
    expect(service.listDocuments).toHaveBeenCalled();
  });
  it("senha errada mostra a mensagem do servidor, mantém o e-mail e limpa a senha", async () => {
    const service = servico(null);
    vi.spyOn(service, "login").mockRejectedValue(
      new ApiError("E-mail ou senha incorretos.", 401),
    );
    const u = userEvent.setup();
    montar(service);
    await screen.findByRole("heading", { name: "Entrar" });
    await u.type(campo("Seu e-mail"), "ana@exemplo.com");
    await u.type(campo("Sua senha"), "errada123");
    await u.click(screen.getByRole("button", { name: "Entrar" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "E-mail ou senha incorretos.",
    );
    expect(campo("Seu e-mail")).toHaveValue("ana@exemplo.com");
    expect(campo("Sua senha")).toHaveValue("");
  });
  it("campos vazios não chamam o servidor", async () => {
    const service = servico(null);
    const login = vi.spyOn(service, "login");
    const u = userEvent.setup();
    montar(service);
    await screen.findByRole("heading", { name: "Entrar" });
    await u.click(screen.getByRole("button", { name: "Entrar" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Informe seu e-mail e sua senha.",
    );
    expect(login).not.toHaveBeenCalled();
  });
  it("o botão Mostrar revela a senha digitada", async () => {
    const u = userEvent.setup();
    montar(servico(null));
    await screen.findByRole("heading", { name: "Entrar" });
    expect(campo("Sua senha")).toHaveAttribute("type", "password");
    await u.click(
      screen.getByRole("button", { name: "Mostrar a senha digitada" }),
    );
    expect(campo("Sua senha")).toHaveAttribute("type", "text");
  });
  it("depois de entrar a pessoa continua no endereço que pediu", async () => {
    const service = servico(null);
    vi.spyOn(service, "login").mockResolvedValue(ana);
    const u = userEvent.setup();
    montar(service, "/documentos");
    await screen.findByRole("heading", { name: "Entrar" });
    await u.type(campo("Seu e-mail"), "ana@exemplo.com");
    await u.type(campo("Sua senha"), "senha1234");
    await u.click(screen.getByRole("button", { name: "Entrar" }));
    expect(
      await screen.findByRole("textbox", { name: "Buscar documento" }),
    ).toBeInTheDocument();
  });
  it("já logada, as telas de conta redirecionam para o início", async () => {
    montar(servico(ana), "/entrar");
    expect(
      await screen.findByRole("button", { name: /Enviar documento/ }),
    ).toBeInTheDocument();
  });
  it("falha ao descobrir a sessão (backend fora) não mostra o login, mostra o erro com nova tentativa", async () => {
    const service = servico(null);
    vi.spyOn(service, "getSession").mockRejectedValueOnce(
      new ApiError("Confira se o backend está rodando.", 500),
    );
    const u = userEvent.setup();
    montar(service);
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Confira se o backend está rodando.",
    );
    expect(
      screen.queryByRole("heading", { name: "Entrar" }),
    ).not.toBeInTheDocument();
    await u.click(screen.getByRole("button", { name: "Tentar novamente" }));
    expect(
      await screen.findByRole("heading", { name: "Entrar" }),
    ).toBeInTheDocument();
  });
  it("sessão que expira no meio do uso volta para a tela de entrar", async () => {
    const service = servico(ana);
    montar(service);
    await screen.findByRole("button", { name: /Enviar documento/ });
    act(() => service.onUnauthorized?.());
    expect(
      await screen.findByRole("heading", { name: "Entrar" }),
    ).toBeInTheDocument();
  });
});

describe("criar conta (modo API)", () => {
  async function abrirCadastro(service: ApiService) {
    const u = userEvent.setup();
    montar(service, "/cadastro");
    await screen.findByRole("heading", { name: "Criar conta" });
    return u;
  }
  async function preencher(
    u: ReturnType<typeof userEvent.setup>,
    senha2: string,
  ) {
    await u.type(campo("Seu nome"), "Ana Souza");
    await u.type(campo("Seu e-mail"), "ana@exemplo.com");
    await u.type(campo("Crie uma senha"), "senha1234");
    await u.type(campo("Repita a senha"), senha2);
    await u.click(screen.getByRole("button", { name: "Criar conta" }));
  }
  it("senhas diferentes: avisa na tela e não chama o servidor", async () => {
    const service = servico(null);
    const register = vi.spyOn(service, "register");
    const u = await abrirCadastro(service);
    await preencher(u, "outra1234");
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "As senhas não conferem.",
    );
    expect(register).not.toHaveBeenCalled();
  });
  it("cadastro válido envia os dados, entra e mostra a tela inicial", async () => {
    const service = servico(null);
    vi.spyOn(service, "register").mockResolvedValue(ana);
    const u = await abrirCadastro(service);
    await preencher(u, "senha1234");
    expect(
      await screen.findByRole("button", { name: /Enviar documento/ }),
    ).toBeInTheDocument();
    expect(service.register).toHaveBeenCalledWith({
      name: "Ana Souza",
      email: "ana@exemplo.com",
      password: "senha1234",
      confirmPassword: "senha1234",
    });
  });
  it("e-mail já cadastrado: mostra a mensagem do servidor e mantém o que foi digitado", async () => {
    const service = servico(null);
    vi.spyOn(service, "register").mockRejectedValue(
      new ApiError("Já existe uma conta com esse e-mail.", 409),
    );
    const u = await abrirCadastro(service);
    await preencher(u, "senha1234");
    expect(await screen.findByRole("alert")).toHaveTextContent(
      "Já existe uma conta com esse e-mail.",
    );
    expect(campo("Seu e-mail")).toHaveValue("ana@exemplo.com");
  });
  it("os links levam de uma tela à outra", async () => {
    const u = userEvent.setup();
    montar(servico(null));
    await screen.findByRole("heading", { name: "Entrar" });
    await u.click(screen.getByRole("link", { name: "Criar conta" }));
    expect(
      await screen.findByRole("heading", { name: "Criar conta" }),
    ).toBeInTheDocument();
    await u.click(screen.getByRole("link", { name: "Entrar" }));
    expect(
      await screen.findByRole("heading", { name: "Entrar" }),
    ).toBeInTheDocument();
  });
});
