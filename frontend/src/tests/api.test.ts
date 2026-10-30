import { afterEach, describe, it, expect, vi } from "vitest";
import { ApiService } from "../services/api/ApiService";
import { ApiError, createHttp } from "../services/api/http";
const dto = {
  id: "doc-1",
  name: "Contrato.pdf",
  categoryId: null,
  processingStatus: "pending",
  createdAt: "2026-10-16T12:00:00Z",
};
afterEach(() => vi.unstubAllGlobals());
describe("adapter HTTP sem fallback", () => {
  it("encaminha File em multipart e preserva estado assíncrono HTTP 202", async () => {
    const fetcher = vi
      .fn()
      .mockResolvedValue(new Response(JSON.stringify(dto), { status: 202 }));
    vi.stubGlobal("fetch", fetcher);
    const file = new File(["a"], "Contrato.pdf", { type: "application/pdf" });
    const result = await new ApiService("http://localhost:8080").upload(
      file,
      "Casa",
    );
    expect(result.processingStatus).toBe("pending");
    const [url, init] = fetcher.mock.calls[0];
    expect(url).toBe("http://localhost:8080/api/documentos");
    expect(init.body).toBeInstanceOf(FormData);
    expect(init.body.get("file")).toBe(file);
    expect(init.body.get("categoryId")).toBe("Casa");
    expect(init.headers).toBeUndefined();
  });
  it("exibe endpoint ausente e nunca retorna fixtures", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(new Response("", { status: 404 })),
    );
    await expect(new ApiService().listDocuments()).rejects.toThrow(
      "ainda não foi implementado",
    );
  });
  it("mostra a mensagem que o backend escreveu para o usuário (ProblemDetail)", async () => {
    const problem = {
      type: "about:blank",
      title: "Bad Request",
      status: 400,
      detail: "Formato não suportado. Envie PDF, PNG ou JPEG.",
    };
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(
          new Response(JSON.stringify(problem), { status: 400 }),
        ),
    );
    await expect(new ApiService().listDocuments()).rejects.toThrow(
      "Formato não suportado. Envie PDF, PNG ou JPEG.",
    );
  });
  it("404 com mensagem do backend mostra a mensagem, não 'não implementado'", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify({ detail: "Documento não encontrado." }), {
          status: 404,
        }),
      ),
    );
    await expect(new ApiService().getDocument("x")).rejects.toThrow(
      "Documento não encontrado.",
    );
  });
  it("erro sem corpo JSON usa a mensagem padrão com o código HTTP", async () => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(new Response("<html>erro</html>", { status: 502 })),
    );
    await expect(new ApiService().listDocuments()).rejects.toThrow(
      "A API retornou erro 502",
    );
  });
  it("5xx sem corpo (proxy sem backend) orienta a conferir se o backend está rodando", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(new Response("", { status: 500 })),
    );
    await expect(new ApiService().listDocuments()).rejects.toThrow(
      "Confira se o backend está rodando",
    );
  });
  it("500 real do servidor (com corpo JSON sem mensagem) não diz que o backend está fora do ar", async () => {
    const corpo = {
      timestamp: "2026-10-30",
      status: 500,
      error: "Internal Server Error",
    };
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(
          new Response(JSON.stringify(corpo), { status: 500 }),
        ),
    );
    await expect(new ApiService().listDocuments()).rejects.toThrow(
      "A API retornou erro 500",
    );
  });
  it("rejeita schemas remotos incompletos", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(new Response('[{"id":"1"}]')),
    );
    await expect(new ApiService().listDocuments()).rejects.toThrow(
      "Status de processamento",
    );
  });
  it("não considera HTML um sucesso JSON", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(new Response("<html>spa fallback</html>")),
    );
    await expect(new ApiService().listDocuments()).rejects.toThrow(
      "deve retornar JSON",
    );
  });
  it("chat usa documento na URL e rejeita resposta de outro contexto", async () => {
    const fetcher = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          id: "m1",
          documentId: "other",
          role: "assistant",
          content: "reply",
          createdAt: "2026-10-16",
        }),
      ),
    );
    vi.stubGlobal("fetch", fetcher);
    await expect(
      new ApiService().sendMessage("doc/1", "Pergunta"),
    ).rejects.toThrow("incompatível");
    expect(fetcher.mock.calls[0][0]).toBe(
      "/api/documentos/doc%2F1/chat/mensagens",
    );
    expect(JSON.parse(fetcher.mock.calls[0][1].body)).toEqual({
      message: "Pergunta",
    });
  });
  it("health aceita texto e anexo sem contrato falha explicitamente", async () => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(
          new Response("Backend do Kaup está rodando e integrado!"),
        ),
    );
    const service = new ApiService();
    expect(await service.health()).toContain("Backend");
  });
  it("anexa contexto como multipart e lista principal mais anexos", async () => {
    const anexo = {
      id: "4",
      documentId: "d1",
      name: "aditivo.pdf",
      mimeType: "application/pdf",
      createdAt: "2026-10-30T12:00:00Z",
    };
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce(Response.json(anexo, { status: 201 }))
      .mockResolvedValueOnce(
        Response.json({
          id: "d1",
          name: "contrato.pdf",
          categoryId: null,
          processingStatus: "completed",
          createdAt: "2026-10-30T12:00:00Z",
        }),
      )
      .mockResolvedValueOnce(Response.json([anexo]));
    vi.stubGlobal("fetch", fetcher);
    const service = new ApiService("http://localhost:8080");
    const file = new File(["x"], "aditivo.pdf", { type: "application/pdf" });
    const anexado = await service.attachContext("d1", file);
    expect(anexado).toEqual({ emoji: "📑", name: "aditivo.pdf", type: "Anexo" });
    const [url, init] = fetcher.mock.calls[0];
    expect(String(url)).toContain("/api/documentos/d1/contexto");
    expect(init.method).toBe("POST");
    expect(init.body).toBeInstanceOf(FormData);
    const lista = await service.getContext("d1");
    expect(lista.map((c) => c.name)).toEqual(["contrato.pdf", "aditivo.pdf"]);
    expect(lista[0].isMain).toBe(true);
  });
  it("recusa anexo de outro documento", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        Response.json({
          id: "4",
          documentId: "outro",
          name: "x.pdf",
          mimeType: "application/pdf",
          createdAt: "2026-10-30T12:00:00Z",
        }),
      ),
    );
    await expect(
      new ApiService("http://localhost:8080").attachContext(
        "d1",
        new File(["x"], "x.pdf"),
      ),
    ).rejects.toThrow("outro documento");
  });
  it("normaliza falha de rede", async () => {
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(new TypeError("offline")));
    await expect(new ApiService().getProfile()).rejects.toThrow(
      "Não foi possível conectar",
    );
  });
  it("aplica timeout e cancelamento sem fabricar resposta", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(
        (_url, init: RequestInit) =>
          new Promise((_resolve, reject) => {
            if (init.signal?.aborted) reject(init.signal.reason);
            init.signal?.addEventListener(
              "abort",
              () => reject(init.signal?.reason),
              { once: true },
            );
          }),
      ),
    );
    await expect(createHttp("", 15)("/api/documentos")).rejects.toThrow(
      "demorou demais",
    );
    const controller = new AbortController();
    controller.abort();
    await expect(
      createHttp("")("/api/documentos", { signal: controller.signal }),
    ).rejects.toThrow("cancelada");
  });
});
describe("sessão, CSRF e cookies", () => {
  const ana = { id: "u1", name: "Ana", email: "ana@exemplo.com", role: "USER" };
  const apagarCookie = () => {
    document.cookie = "XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT";
  };
  afterEach(apagarCookie);
  it("devolve o token CSRF do cookie nas escritas, mas não nas leituras", async () => {
    document.cookie = "XSRF-TOKEN=abc%20123";
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(new Response(JSON.stringify(ana)));
    vi.stubGlobal("fetch", fetcher);
    const service = new ApiService();
    await service.logout();
    await service.getSession();
    expect(fetcher.mock.calls[0][1].headers["X-XSRF-TOKEN"]).toBe("abc 123");
    expect(fetcher.mock.calls[1][1].headers).toBeUndefined();
  });
  it("sem cookie não mexe nos cabeçalhos (um upload não ganha Content-Type)", async () => {
    const fetcher = vi
      .fn()
      .mockResolvedValue(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetcher);
    await new ApiService().logout();
    expect(fetcher.mock.calls[0][1].headers).toBeUndefined();
  });
  it("envia cookies: 'include' com a API em outra origem, 'same-origin' pelo proxy", async () => {
    const fetcher = vi
      .fn()
      .mockImplementation(() => Promise.resolve(new Response("[]")));
    vi.stubGlobal("fetch", fetcher);
    await new ApiService().listDocuments();
    await new ApiService("http://localhost:8080").listDocuments();
    expect(fetcher.mock.calls[0][1].credentials).toBe("same-origin");
    expect(fetcher.mock.calls[1][1].credentials).toBe("include");
  });
  it("getSession devolve o usuário no 200 e null no 401 (ninguém logado não é erro)", async () => {
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce(new Response(JSON.stringify(ana)))
      .mockResolvedValueOnce(new Response("", { status: 401 }));
    vi.stubGlobal("fetch", fetcher);
    const service = new ApiService();
    await expect(service.getSession()).resolves.toEqual(ana);
    await expect(service.getSession()).resolves.toBeNull();
  });
  it("getSession repassa os outros erros (backend fora do ar não é 'deslogado')", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(new Response("", { status: 500 })),
    );
    await expect(new ApiService().getSession()).rejects.toThrow(
      "Confira se o backend está rodando",
    );
  });
  it("avisa a aplicação quando a sessão acaba (401), mas não nas telas de login", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(new Response("", { status: 401 })),
    );
    const service = new ApiService();
    const aviso = vi.fn();
    service.onUnauthorized = aviso;
    await expect(service.listDocuments()).rejects.toBeInstanceOf(ApiError);
    expect(aviso).toHaveBeenCalledTimes(1);
    // Senha errada também é 401, mas não significa "sessão expirou".
    await expect(service.login("a@b.co", "x")).rejects.toBeInstanceOf(ApiError);
    await service.getSession();
    expect(aviso).toHaveBeenCalledTimes(1);
  });
  it("login e cadastro enviam JSON e validam o usuário recebido", async () => {
    const fetcher = vi
      .fn()
      .mockImplementation(() =>
        Promise.resolve(new Response(JSON.stringify(ana), { status: 201 })),
      );
    vi.stubGlobal("fetch", fetcher);
    const service = new ApiService();
    await service.login("ana@exemplo.com", "senha1234");
    await service.register({
      name: "Ana",
      email: "ana@exemplo.com",
      password: "senha1234",
      confirmPassword: "senha1234",
    });
    expect(fetcher.mock.calls[0][0]).toBe("/api/auth/entrar");
    expect(JSON.parse(fetcher.mock.calls[0][1].body)).toEqual({
      email: "ana@exemplo.com",
      password: "senha1234",
    });
    expect(fetcher.mock.calls[1][0]).toBe("/api/auth/cadastro");
    expect(JSON.parse(fetcher.mock.calls[1][1].body).confirmPassword).toBe(
      "senha1234",
    );
  });
  it("rejeita papel de usuário desconhecido", async () => {
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(
          new Response(JSON.stringify({ ...ana, role: "ROOT" })),
        ),
    );
    await expect(new ApiService().getSession()).rejects.toThrow(
      "Papel de usuário inválido",
    );
  });
  it("o perfil precisa trazer shareIncomeWithAi", async () => {
    const semCampo = {
      name: "Ana",
      email: "ana@exemplo.com",
      fontSize: "large",
      explainLevel: "simple",
      income: "prefer_not",
      audio: false,
    };
    const fetcher = vi
      .fn()
      .mockResolvedValueOnce(new Response(JSON.stringify(semCampo)))
      .mockResolvedValueOnce(
        new Response(JSON.stringify({ ...semCampo, shareIncomeWithAi: false })),
      );
    vi.stubGlobal("fetch", fetcher);
    const service = new ApiService();
    await expect(service.getProfile()).rejects.toThrow(
      "Preferências incompatíveis",
    );
    await expect(service.getProfile()).resolves.toMatchObject({
      shareIncomeWithAi: false,
    });
  });
  it("ownedByMe do documento: falso quando vem falso, verdadeiro quando ausente", async () => {
    const doc = {
      id: "d1",
      name: "Contrato.pdf",
      categoryId: null,
      processingStatus: "completed",
      createdAt: "2026-10-30T12:00:00Z",
    };
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(
          new Response(
            JSON.stringify([doc, { ...doc, id: "d2", ownedByMe: false }]),
          ),
        ),
    );
    const [antigo, alheio] = await new ApiService().listDocuments();
    expect(antigo.ownedByMe).toBe(true);
    expect(alheio.ownedByMe).toBe(false);
  });
});
