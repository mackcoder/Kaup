import { describe, it, expect, vi, afterEach } from "vitest";
import {
  act,
  cleanup,
  fireEvent,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
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
    const fetcher = vi.fn().mockResolvedValue(
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

describe("envio e tela do documento (modo API)", () => {
  async function enviarArquivo(nome: string) {
    fireEvent.click(
      await screen.findByRole("button", { name: /Enviar documento/ }),
    );
    const dialog = await screen.findByRole("dialog");
    const input = within(dialog).getByLabelText("Escolher arquivo", {
      selector: "input",
    });
    fireEvent.change(input, {
      target: {
        files: [new File(["%PDF-1.4"], nome, { type: "application/pdf" })],
      },
    });
    fireEvent.click(
      await screen.findByRole("button", {
        name: /Confirmar e enviar|Tentar novamente/,
      }),
    );
  }

  it("repete a mesma Idempotency-Key ao tentar de novo e troca a chave ao escolher outro arquivo", async () => {
    const service = servico(ana, []);
    const upload = vi
      .spyOn(service, "upload")
      .mockRejectedValueOnce(new Error("rede caiu"))
      .mockResolvedValue({ ...lendo, id: "novo" });
    vi.spyOn(service, "getDocument").mockResolvedValue({ ...lendo });
    vi.spyOn(service, "getAnalysis").mockResolvedValue({
      documentId: "novo",
      processingStatus: "pending",
      summary: null,
      keyPoints: [],
      ocrQualityWarning: null,
    });
    montar(service, "/");

    await enviarArquivo("Aluguel.pdf");
    await screen.findByRole("alert");
    fireEvent.click(screen.getByRole("button", { name: "Tentar novamente" }));
    await waitFor(() => expect(upload).toHaveBeenCalledTimes(2));

    const [primeira, segunda] = upload.mock.calls.map((c) => c[3]);
    expect(primeira).toMatch(/^[0-9a-f-]{36}$/);
    expect(segunda).toBe(primeira); // mesmo envio: não duplica no servidor
  });

  it("documento ainda sendo lido mostra o aviso e mantém o chat desligado", async () => {
    const service = servico(ana, [lendo]);
    vi.spyOn(service, "getDocument").mockResolvedValue({ ...lendo });
    vi.spyOn(service, "getAnalysis").mockResolvedValue({
      documentId: "novo",
      processingStatus: "processing",
      summary: null,
      keyPoints: [],
      ocrQualityWarning: null,
    });
    const mensagens = vi.spyOn(service, "getMessages");

    montar(service, "/documentos/novo/chat");

    expect(
      await screen.findByText(/Estamos lendo seu contrato/),
    ).toBeInTheDocument();
    expect(mensagens).not.toHaveBeenCalled();
    expect(screen.queryByLabelText(/mensagem/i)).not.toBeInTheDocument();
  });

  it("documento que falhou mostra o motivo na tela, sem abrir o chat", async () => {
    const falho = { ...lendo, processingStatus: "failed" as const };
    const service = servico(ana, [falho]);
    vi.spyOn(service, "getDocument").mockResolvedValue(falho);
    vi.spyOn(service, "getAnalysis").mockResolvedValue({
      documentId: "novo",
      processingStatus: "failed",
      summary: null,
      keyPoints: [],
      ocrQualityWarning: "O PDF está protegido por senha.",
    });
    const mensagens = vi.spyOn(service, "getMessages");

    montar(service, "/documentos/novo/chat");

    expect(
      await screen.findByText("O PDF está protegido por senha."),
    ).toBeInTheDocument();
    expect(mensagens).not.toHaveBeenCalled();
  });
});
