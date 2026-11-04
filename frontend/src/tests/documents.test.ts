import { describe, it, expect, vi } from "vitest";
import { MockService } from "../services/mock/MockService";
import { presentDocument } from "../styles/presenters";
import {
  countDocuments,
  filterDocuments,
} from "../features/documents/selectors";
import { MAX_FILE_SIZE, validateFile } from "../features/documents/upload";
import { createService } from "../services";
describe("documentos e sessão mock", () => {
  it("usa uma fonte única para categorias, contadores e busca", async () => {
    const docs = (await new MockService().listDocuments()).map(presentDocument);
    expect(countDocuments(docs)).toEqual({
      all: 25,
      ok: 15,
      warning: 6,
      pending: 4,
    });
    expect(filterDocuments(docs, "warning", "gol")).toHaveLength(1);
    expect(filterDocuments(docs, "ok", "  CASA  ")).toHaveLength(3);
    expect(filterDocuments(docs, "pending", "sem resultado")).toEqual([]);
    expect(new Set(docs.map((d) => d.id)).size).toBe(25);
  });
  it("adiciona o File escolhido à categoria e identifica as finanças como exemplo, sem extraí-las do upload", async () => {
    const service = new MockService();
    const file = new File(["%PDF-1.4"], "meu-contrato.pdf", {
      type: "application/pdf",
    });
    const doc = await service.upload(file, "Casa");
    expect(doc.name).toBe(file.name);
    expect(doc.categoryId).toBe("Casa");
    expect(countDocuments(await service.listDocuments()).all).toBe(26);
    const analysis = await service.getAnalysis(doc.id);
    expect(analysis.facts?.[0].value).toBe("R$ 1.250,00");
    expect(analysis.exampleNotice).toContain("Não foram extraídos");
    expect(analysis.exampleNotice).toContain(file.name);
    expect(analysis.summary).toContain("Nenhum texto foi extraído");
    expect((await service.getMessages(doc.id))[0].text).toContain(
      "Não li o conteúdo",
    );
  });
  it("cancela upload sem criar documento", async () => {
    const service = new MockService();
    const c = new AbortController();
    const promise = service.upload(
      new File(["a"], "a.pdf", { type: "application/pdf" }),
      null,
      c.signal,
    );
    c.abort();
    await expect(promise).rejects.toThrow();
    expect(await service.listDocuments()).toHaveLength(25);
  });
  it("mantém histórico e contexto separados por documentId", async () => {
    vi.useFakeTimers();
    try {
      const s = new MockService();
      const sending = s.sendMessage("carros-1", "Qual o prazo?");
      await vi.advanceTimersByTimeAsync(1700);
      const reply = await sending;
      expect(reply.reply.documentId).toBe("carros-1");
      expect(
        (await s.getMessages("carros-1")).some(
          (m) => m.text === "Qual o prazo?",
        ),
      ).toBe(true);
      expect(
        (await s.getMessages("casa-1")).some((m) => m.role === "user"),
      ).toBe(false);
      const f = new File(["a"], "apoio.pdf", { type: "application/pdf" });
      await s.attachContext("carros-1", f);
      expect(
        (await s.getContext("carros-1")).some((c) => c.name === f.name),
      ).toBe(true);
      expect(
        (await s.getContext("casa-1")).some((c) => c.name === f.name),
      ).toBe(false);
    } finally {
      vi.useRealTimers();
    }
  });
  it("atualiza preferências na sessão e exclui documentos mock", async () => {
    const s = new MockService();
    const p = await s.getProfile();
    await s.updateProfile({ name: "Ana", income: "3to5" });
    expect(await s.getProfile()).toEqual({ ...p, name: "Ana", income: "3to5" }); // o resto não muda
    expect((await s.getProfile()).name).toBe("Ana");
    await s.removeDocument("carros-1");
    await expect(s.getDocument("carros-1")).rejects.toThrow("não encontrado");
    expect(await s.listDocuments()).toHaveLength(24);
  });
  it("não aceita modo de dados desconhecido", () =>
    expect(() => createService("typo")).toThrow("mock ou api"));
});
describe("validação conservadora de arquivo", () => {
  it.each([
    ["a.pdf", "application/pdf"],
    ["a.png", "image/png"],
    ["a.jpeg", "image/jpeg"],
  ])("aceita %s", (name, type) =>
    expect(validateFile(new File(["a"], name, { type }))).toBeNull(),
  );
  it("rejeita arquivo vazio, grande, extensão/MIME conflitantes e formatos não suportados", () => {
    expect(
      validateFile(new File([], "a.pdf", { type: "application/pdf" })),
    ).toContain("vazio");
    expect(
      validateFile(
        new File([new Uint8Array(MAX_FILE_SIZE + 1)], "a.pdf", {
          type: "application/pdf",
        }),
      ),
    ).toContain("10 MiB");
    expect(
      validateFile(new File(["a"], "a.pdf", { type: "image/png" })),
    ).toContain("PDF, PNG ou JPEG");
    expect(
      validateFile(
        new File(["a"], "a.docx", {
          type: "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        }),
      ),
    ).toContain("ainda não");
  });
});
