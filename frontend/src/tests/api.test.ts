import { afterEach, describe, it, expect, vi } from "vitest";
import { ApiService } from "../services/api/ApiService";
import { createHttp } from "../services/api/http";
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
    await expect(service.attachContext()).rejects.toThrow(
      "Nenhum arquivo foi enviado",
    );
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
