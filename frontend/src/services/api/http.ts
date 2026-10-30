export class ApiError extends Error {
  constructor(
    message: string,
    readonly status?: number,
  ) {
    super(message);
    this.name = "ApiError";
  }
}
/** Mensagem que o backend escreveu para o usuário: `detail` (RFC 7807) ou `message`. */
async function errorDetail(response: Response): Promise<string | null> {
  try {
    const body: unknown = await response.json();
    if (body && typeof body === "object") {
      const { detail, message } = body as Record<string, unknown>;
      for (const text of [detail, message])
        if (typeof text === "string" && text.trim()) return text;
    }
  } catch {
    // Sem corpo ou fora de JSON: quem chama usa a mensagem padrão.
  }
  return null;
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
          (await errorDetail(response)) ??
            (response.status === 404 || response.status === 501
              ? `API indisponível: ${path} ainda não foi implementado ou configurado no backend.`
              : `A API retornou erro ${response.status}. Tente novamente.`),
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
