export class ApiError extends Error {
  constructor(
    message: string,
    readonly status?: number,
  ) {
    super(message);
    this.name = "ApiError";
  }
}
/**
 * O que o corpo de uma resposta de erro revela: a mensagem que o backend escreveu para o
 * usuário (`detail` do RFC 7807 ou `message`) e se o corpo veio vazio.
 */
async function errorInfo(
  response: Response,
): Promise<{ detail: string | null; empty: boolean }> {
  let raw = "";
  try {
    raw = await response.text();
  } catch {
    // Corpo ilegível: trata como ausente.
  }
  if (!raw.trim()) return { detail: null, empty: true };
  try {
    const body: unknown = JSON.parse(raw);
    if (body && typeof body === "object") {
      const { detail, message } = body as Record<string, unknown>;
      for (const text of [detail, message])
        if (typeof text === "string" && text.trim())
          return { detail: text, empty: false };
    }
  } catch {
    // Fora de JSON: quem chama usa a mensagem padrão.
  }
  return { detail: null, empty: false };
}
/** Erro 5xx sem corpo: é o proxy ou o servidor de frontend sem ninguém do outro lado. */
const backendForaDoAr = (status: number) =>
  `Não foi possível falar com o servidor (erro ${status}). Confira se o backend está rodando e tente novamente.`;
function errorMessage(
  path: string,
  status: number,
  { detail, empty }: { detail: string | null; empty: boolean },
) {
  if (detail) return detail;
  if (status === 404 || status === 501)
    return `API indisponível: ${path} ainda não foi implementado ou configurado no backend.`;
  if (empty && status >= 500) return backendForaDoAr(status);
  return `A API retornou erro ${status}. Tente novamente.`;
}
export function createHttp(baseUrl: string, timeoutMs = 20000) {
  return async function request(
    path: string,
    init: RequestInit = {},
    text = false,
  ): Promise<unknown> {
    const controller = new AbortController();
    const abort = () => controller.abort(init.signal?.reason);
    if (init.signal?.aborted) abort();
    else init.signal?.addEventListener("abort", abort, { once: true });
    const timer = setTimeout(
      () =>
        controller.abort(
          new DOMException("Tempo limite excedido", "TimeoutError"),
        ),
      timeoutMs,
    );
    try {
      const response = await fetch(baseUrl.replace(/\/$/, "") + path, {
        ...init,
        signal: controller.signal,
      });
      if (!response.ok)
        throw new ApiError(
          errorMessage(path, response.status, await errorInfo(response)),
          response.status,
        );
      if (response.status === 204) return undefined;
      if (text) return await response.text();
      try {
        return (await response.json()) as unknown;
      } catch {
        throw new ApiError(
          "Resposta inválida: o endpoint deve retornar JSON no contrato acordado.",
        );
      }
    } catch (error) {
      if (error instanceof ApiError) throw error;
      if (controller.signal.aborted)
        throw new ApiError(
          controller.signal.reason?.name === "TimeoutError"
            ? "O servidor demorou demais. Tente novamente."
            : "Operação cancelada.",
        );
      throw new ApiError(
        "Não foi possível conectar ao backend. Verifique a conexão e tente novamente.",
      );
    } finally {
      clearTimeout(timer);
      init.signal?.removeEventListener("abort", abort);
    }
  };
}
