import { describe, it, expect, vi, afterEach } from "vitest";
import { act, cleanup, screen, waitFor } from "@testing-library/react";
import { ApiService } from "../services/api/ApiService";
import { documentFromDTO } from "../services/api/dto";
import { statusOf } from "../styles/status";
import type { KaupDocument } from "../types/domain";
import { ana, documento, montar, servico } from "./apiHelpers";

afterEach(() => {
  cleanup();
  vi.useRealTimers();
  vi.unstubAllGlobals();
});

const lendo: KaupDocument = {
  ...documento,
  id: "novo",
  name: "Aluguel.pdf",
  description: "Lendo o contrato…",
  status: "pending",
  processingStatus: "pending",
};
const dto = {
  id: "novo",
  name: "Aluguel.pdf",
  categoryId: null,
  createdAt: "2026-10-30T12:00:00Z",
  description: null,
  visualStatus: "pending",
};

describe("envio assíncrono (202)", () => {
  it("manda a Idempotency-Key no envio, e só ela nos cabeçalhos", async () => {
    const fetcher = vi
      .fn()
      .mockResolvedValue(
        new Response(JSON.stringify({ ...dto, processingStatus: "pending" }), {
          status: 202,
        }),
      );
    vi.stubGlobal("fetch", fetcher);
    const file = new File(["a"], "Aluguel.pdf", { type: "application/pdf" });

    await new ApiService().upload(file, null, undefined, "chave-1");

    const [, init] = fetcher.mock.calls[0];
    expect(Object.fromEntries(new Headers(init.headers))).toEqual({
      "idempotency-key": "chave-1",
    });
  });

  it("documento pendente vira 'Lendo o contrato…' e o falho mostra o motivo", () => {
    const pendente = documentFromDTO({ ...dto, processingStatus: "pending" });
    expect(pendente.description).toBe("Lendo o contrato…");

    const falho = documentFromDTO({
      ...dto,
      processingStatus: "failed",
      failureReason: "O PDF está protegido por senha.",
    });
    expect(falho.description).toBe("O PDF está protegido por senha.");
    expect(falho.failureReason).toBe("O PDF está protegido por senha.");
  });

  it("o selo é 'Lendo…' enquanto lê, 'Não deu para ler' se falhou e o do resumo depois", () => {
    expect(
      statusOf({ status: "pending", processingStatus: "pending" }).label,
    ).toBe("Lendo…");
    expect(
      statusOf({ status: "pending", processingStatus: "processing" }).label,
    ).toBe("Lendo…");
    expect(
      statusOf({ status: "pending", processingStatus: "failed" }).label,
    ).toBe("Não deu para ler");
    expect(
      statusOf({ status: "warning", processingStatus: "completed" }).label,
    ).toBe("Atenção");
  });

  it("a lista se atualiza sozinha enquanto há documento sendo lido e para ao terminar", async () => {
    vi.useFakeTimers({ toFake: ["setInterval", "clearInterval"] });
    const service = servico(ana, [lendo]);
    // 1ª consulta (ao entrar): pendente. Depois: pronto.
    const pronto: KaupDocument = {
      ...lendo,
      description: "Contrato de aluguel.",
      status: "ok",
      processingStatus: "completed",
    };
    vi.mocked(service.listDocuments)
      .mockResolvedValueOnce([lendo])
      .mockResolvedValue([pronto]);

    montar(service, "/documentos");
    expect(await screen.findByTitle("Lendo…")).toBeInTheDocument();
    expect(service.listDocuments).toHaveBeenCalledTimes(1);

    await act(async () => {
      vi.advanceTimersByTime(3000);
    });
    await waitFor(() =>
      expect(screen.queryByTitle("Lendo…")).not.toBeInTheDocument(),
    );
    expect(service.listDocuments).toHaveBeenCalledTimes(2);

    // Tudo pronto: não consulta mais.
    await act(async () => {
      vi.advanceTimersByTime(9000);
    });
    expect(service.listDocuments).toHaveBeenCalledTimes(2);
  });

  it("sem documento sendo lido não consulta a lista em segundo plano", async () => {
    vi.useFakeTimers({ toFake: ["setInterval", "clearInterval"] });
    const service = servico(ana, [documento]);

    montar(service, "/documentos");
    await screen.findByText("Contrato.pdf");
    await act(async () => {
      vi.advanceTimersByTime(10000);
    });

    expect(service.listDocuments).toHaveBeenCalledTimes(1);
  });

  it("documento que falhou aparece com o selo de erro na lista", async () => {
    const falho: KaupDocument = {
      ...lendo,
      description: "O PDF está protegido por senha.",
      processingStatus: "failed",
      failureReason: "O PDF está protegido por senha.",
    };
    montar(servico(ana, [falho]), "/documentos");

    expect(await screen.findByTitle("Não deu para ler")).toBeInTheDocument();
  });
});
