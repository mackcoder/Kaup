import { StrictMode } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  act,
  fireEvent,
  render,
  screen,
  within,
  cleanup,
} from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import App from "../app/App";
import { AppProvider } from "../app/AppProvider";
import { MockService } from "../services/mock/MockService";
import { ApiService } from "../services/api/ApiService";
import type { KaupService } from "../services/contracts/KaupService";
import { stageFromHistory } from "../features/chat/session";
const choice = "Menos de R$ 3.000";
const question = "Qual é a sua renda mensal aproximada?";
const user = () => ({
  click: async (target: HTMLElement) => {
    fireEvent.click(target);
    await advance();
  },
  type: async (target: HTMLElement, value: string) => {
    fireEvent.change(target, { target: { value } });
    await advance();
  },
  upload: async (target: HTMLElement, file: File) => {
    fireEvent.change(target, { target: { files: [file] } });
    await advance();
  },
});
async function advance(ms = 0) {
  await act(async () => {
    await vi.advanceTimersByTimeAsync(ms);
  });
}
function mount(service: KaupService, path = "/documentos/carros-1/chat") {
  return render(
    <StrictMode>
      <MemoryRouter initialEntries={[path]}>
        <AppProvider service={service}>
          <App />
        </AppProvider>
      </MemoryRouter>
    </StrictMode>,
  );
}
async function waitingIncome(service = new MockService()) {
  mount(service);
  await advance();
  await advance(1400);
  return service;
}
async function confirmIncome(value = choice) {
  const u = user();
  await u.click(screen.getByRole("button", { name: value }));
  await u.click(screen.getByRole("button", { name: /Enviar resposta/ }));
}
const composer = () =>
  screen.getByRole("textbox", { name: "Mensagem sobre o documento" });
const history = () =>
  screen.getByRole("log", { name: "Conversa sobre o documento" });
beforeEach(() => vi.useFakeTimers());
afterEach(() => {
  cleanup();
  vi.clearAllTimers();
  vi.useRealTimers();
  vi.unstubAllGlobals();
});
describe("jornada demonstrativa completa", () => {
  it("apresenta introdução, resumo, pergunta e alternativas gradualmente, sem liberar texto durante a introdução", async () => {
    mount(new MockService());
    await advance();
    expect(composer()).toBeDisabled();
    expect(screen.queryByText("Parcela mensal")).not.toBeInTheDocument();
    await advance(600);
    expect(screen.getByText("Parcela mensal")).toBeInTheDocument();
    expect(screen.queryByText(question)).not.toBeInTheDocument();
    await advance(400);
    expect(screen.getByText(question)).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: choice }),
    ).not.toBeInTheDocument();
    await advance(400);
    expect(screen.getByRole("button", { name: choice })).toBeEnabled();
    expect(
      screen.getByRole("button", { name: "Prefiro não informar" }),
    ).toBeInTheDocument();
  });
  it("seleciona sem enviar, confirma uma vez, bloqueia alternativas, mostra digitação, resposta e aviso único, depois responde perguntas livres", async () => {
    const service = await waitingIncome();
    const send = vi.spyOn(service, "sendMessage");
    const u = user();
    const option = screen.getByRole("button", { name: choice });
    await u.click(option);
    expect(option).toHaveAttribute("aria-pressed", "true");
    expect(send).not.toHaveBeenCalled();
    const confirm = screen.getByRole("button", { name: /Enviar resposta/ });
    fireEvent.click(confirm);
    fireEvent.click(confirm);
    expect(send).toHaveBeenCalledTimes(1);
    expect(option).toBeDisabled();
    expect(confirm).toBeDisabled();
    expect(
      screen.getByRole("status", { name: "Assistente digitando" }),
    ).toBeInTheDocument();
    expect(screen.getByText(question)).toBeInTheDocument();
    await advance(1800);
    expect(
      screen.getByText(/Exemplo fictício:.*mais de 40%/),
    ).toBeInTheDocument();
    expect(
      screen.queryByText(/não substitui orientação/),
    ).not.toBeInTheDocument();
    await advance(800);
    expect(composer()).toBeEnabled();
    expect(screen.getAllByText(/não substitui orientação/)).toHaveLength(1);
    expect(
      screen.queryByRole("button", { name: choice }),
    ).not.toBeInTheDocument();
    await u.type(composer(), "Qual é a multa?");
    await u.click(screen.getByRole("button", { name: "Enviar mensagem" }));
    expect(
      screen.getByRole("status", { name: "Assistente digitando" }),
    ).toBeInTheDocument();
    await advance(1600);
    expect(
      screen.getByText(/Exemplo fictício: A multa por cancelamento/),
    ).toBeInTheDocument();
    expect(screen.getAllByText(/não substitui orientação/)).toHaveLength(1);
    const stored = await service.getMessages("carros-1");
    expect(
      stored.filter((m) => m.role === "user" && m.intent === "income"),
    ).toHaveLength(1);
    expect(stored.filter((m) => m.kind === "disclaimer")).toHaveLength(1);
  });
  it.each(["Prefiro não informar", "Outra opção"])(
    "permite “%s” e libera a conversa sem exigir renda",
    async (value) => {
      await waitingIncome();
      await confirmIncome(value);
      await advance(2600);
      expect(composer()).toBeEnabled();
      expect(
        screen.getByText(/Exemplo fictício: Tudo bem!/),
      ).toBeInTheDocument();
      const u = user();
      await u.type(composer(), "Qual o prazo?");
      await u.click(screen.getByRole("button", { name: "Enviar mensagem" }));
      await advance(1600);
      expect(screen.getByText(/O prazo é de 48 meses/)).toBeInTheDocument();
    },
  );
  it("envia PDF pela confirmação e executa processamento, exemplo rotulado, pergunta, renda e chat livre", async () => {
    const service = new MockService();
    const selectedFile = new File(["%PDF-1.4 teste"], "contrato-novo.pdf", {
      type: "application/pdf",
    });
    const upload = vi.spyOn(service, "upload");
    mount(service, "/");
    await advance();
    const u = user();
    await u.click(screen.getByRole("button", { name: /Enviar documento/ }));
    await advance(150);
    await u.upload(
      within(screen.getByRole("dialog")).getByLabelText("Escolher arquivo", {
        selector: "input",
      }),
      selectedFile,
    );
    expect(screen.getByText(/contrato-novo.pdf ·/)).toBeInTheDocument();
    await u.click(screen.getByRole("button", { name: "Confirmar e enviar" }));
    await advance(500);
    await advance();
    expect(upload.mock.calls[0][0]).toBe(selectedFile);
    expect(screen.getByText("Simulando análise")).toBeInTheDocument();
    expect(screen.queryByText(question)).not.toBeInTheDocument();
    await advance(3200);
    await advance(1700);
    expect(
      screen.getByText(/Não li o conteúdo do arquivo e não executei OCR/),
    ).toBeInTheDocument();
    expect(screen.getByRole("note")).toHaveTextContent(
      "Valores de um contrato fictício de exemplo",
    );
    expect(screen.getByRole("note")).toHaveTextContent(
      "Não foram extraídos de “contrato-novo.pdf”",
    );
    expect(screen.getByText("R$ 1.250,00")).toBeInTheDocument();
    expect(screen.getByText(question)).toBeInTheDocument();
    expect(
      screen.queryByText(/^Analisei seu contrato\./),
    ).not.toBeInTheDocument();
    await confirmIncome();
    await advance(2600);
    expect(
      screen.getByText(/Exemplo fictício:.*mais de 40%/),
    ).toBeInTheDocument();
    await u.type(composer(), "E os juros?");
    await u.click(screen.getByRole("button", { name: "Enviar mensagem" }));
    await advance(1600);
    expect(screen.getByText(/Os juros são o custo extra/)).toBeInTheDocument();
  });
  it("preserva histórico e etapa concluída ao voltar; outro documento inicia sua própria jornada", async () => {
    const service = await waitingIncome();
    const u = user();
    await confirmIncome();
    await advance(2600);
    await u.click(screen.getByRole("button", { name: /Voltar/ }));
    await advance();
    await u.click(screen.getByRole("button", { name: /Carro Gol 2016/ }));
    await advance(150);
    await advance();
    expect(composer()).toBeEnabled();
    expect(
      within(history()).getAllByText(/Exemplo fictício:.*mais de 40%/),
    ).toHaveLength(1);
    expect(screen.getAllByText(/não substitui orientação/)).toHaveLength(1);
    expect(
      screen.queryByRole("button", { name: choice }),
    ).not.toBeInTheDocument();
    await u.click(screen.getByRole("button", { name: /Voltar/ }));
    await advance();
    await u.click(screen.getByRole("button", { name: /Aluguel Ap. Centro/ }));
    await advance(150);
    await advance(1400);
    expect(screen.getByRole("button", { name: choice })).toBeEnabled();
    expect(composer()).toBeDisabled();
    expect(
      (await service.getMessages("casa-1")).some((m) => m.role === "user"),
    ).toBe(false);
  });
  it("conclui um envio já confirmado mesmo ao sair antes da resposta e não repete ao retornar", async () => {
    await waitingIncome();
    const u = user();
    await confirmIncome();
    await u.click(screen.getByRole("button", { name: /Voltar/ }));
    await advance(2600);
    await u.click(screen.getByRole("button", { name: /Carro Gol 2016/ }));
    await advance(150);
    await advance();
    expect(composer()).toBeEnabled();
    expect(screen.getAllByText(/não substitui orientação/)).toHaveLength(1);
  });
  it("retoma introdução interrompida sem duplicar pergunta ou pular renda", async () => {
    mount(new MockService());
    await advance();
    await advance(600);
    const u = user();
    await u.click(screen.getByRole("button", { name: /Voltar/ }));
    await advance();
    await u.click(screen.getByRole("button", { name: /Carro Gol 2016/ }));
    await advance(150);
    await advance();
    expect(screen.getAllByText(question)).toHaveLength(1);
    expect(screen.getByRole("button", { name: choice })).toBeEnabled();
    expect(composer()).toBeDisabled();
  });
  it("permite retry de falha de renda sem duplicar usuário, resposta ou aviso", async () => {
    const service = await waitingIncome();
    const spy = vi
      .spyOn(service, "sendMessage")
      .mockRejectedValueOnce(new Error("Falha simulada de envio"));
    await confirmIncome();
    await advance();
    expect(screen.getByRole("alert")).toHaveTextContent("Falha simulada");
    expect(screen.getByText(question)).toBeInTheDocument();
    expect(screen.getByRole("button", { name: choice })).toBeDisabled();
    expect(composer()).toBeDisabled();
    const u = user();
    await u.click(
      screen.getByRole("button", { name: "Tentar enviar novamente" }),
    );
    await advance(2600);
    expect(spy).toHaveBeenCalledTimes(2);
    expect(spy.mock.calls[0][3]?.turnId).toBe(spy.mock.calls[1][3]?.turnId);
    expect(screen.getAllByText(choice, { exact: true })).toHaveLength(1);
    expect(screen.getAllByText(/Exemplo fictício:.*mais de 40%/)).toHaveLength(
      1,
    );
    expect(screen.getAllByText(/não substitui orientação/)).toHaveLength(1);
  });
  it("repete com o mesmo ID quando a resposta se perde após o mock já ter gravado o envio", async () => {
    const service = await waitingIncome();
    const original = service.sendMessage.bind(service);
    vi.spyOn(service, "sendMessage").mockImplementationOnce(async (...args) => {
      await original(...args);
      throw new Error("Resposta perdida");
    });
    await confirmIncome();
    await advance(1800);
    expect(screen.getByRole("alert")).toHaveTextContent("Resposta perdida");
    await user().click(
      screen.getByRole("button", { name: "Tentar enviar novamente" }),
    );
    await advance(800);
    const stored = await service.getMessages("carros-1");
    expect(
      stored.filter((m) => m.role === "user" && m.intent === "income"),
    ).toHaveLength(1);
    expect(
      stored.filter(
        (m) => m.role === "ai" && m.intent === "income" && m.kind === "text",
      ),
    ).toHaveLength(1);
    expect(stored.filter((m) => m.kind === "disclaimer")).toHaveLength(1);
    expect(screen.getAllByText(/não substitui orientação/)).toHaveLength(1);
  });
  it("não usa texto, símbolos de dinheiro ou qualquer mensagem do usuário para concluir a etapa de renda", () => {
    expect(
      stageFromHistory("mock", [
        {
          id: "q",
          documentId: "1",
          role: "ai",
          kind: "options",
          intent: "income",
          options: [choice],
        },
        {
          id: "u",
          documentId: "1",
          role: "user",
          kind: "text",
          intent: "free-text",
          text: "R$ 5.000",
        },
      ]),
    ).toBe("waiting-income");
    expect(stageFromHistory("api", [])).toBe("free-chat");
  });
});
describe("respostas por faixa no contrato de exemplo", () => {
  it.each([
    ["Menos de R$ 3.000", "mais de 40%"],
    ["Entre R$ 3.000 e R$ 5.000", "entre 25% e 42%"],
    ["Entre R$ 5.000 e R$ 7.000", "entre 18% e 25%"],
    ["Entre R$ 7.000 e R$ 15.000", "entre 8% e 18%"],
    ["Entre R$ 15.000 e R$ 20.000", "menos de 8%"],
    ["Mais de R$ 20.000", "menos de 7%"],
    ["Outra opção", "Independente da renda"],
    ["Prefiro não informar", "não precisa informar sua renda"],
  ])("responde “%s” e produz um único aviso", async (text, expected) => {
    const service = new MockService();
    const options = { turnId: crypto.randomUUID(), intent: "income" as const };
    const sending = service.sendMessage("carros-1", text, undefined, options);
    await vi.advanceTimersByTimeAsync(1800);
    const turn = await sending;
    expect(turn.reply.text).toContain(expected);
    expect(turn.reply.intent).toBe("income");
    expect(turn.followUps).toHaveLength(1);
    const repeated = await service.sendMessage(
      "carros-1",
      text,
      undefined,
      options,
    );
    expect(repeated.reply.id).toBe(turn.reply.id);
    const stored = await service.getMessages("carros-1");
    expect(stored.filter((m) => m.turnId === options.turnId)).toHaveLength(3);
  });
});
describe("chat API sem roteiro fictício", () => {
  function remoteService() {
    const service = new ApiService();
    vi.spyOn(service, "getDocument").mockResolvedValue({
      id: "real",
      name: "Documento do servidor",
      description: "",
      emoji: "📄",
      categoryId: null,
      status: "pending",
      processingStatus: "completed",
      createdAt: "2026-10-16T12:00:00Z",
    });
    vi.spyOn(service, "getAnalysis").mockResolvedValue({
      documentId: "real",
      processingStatus: "completed",
      summary: "Resumo retornado pelo servidor",
      keyPoints: [],
    });
    vi.spyOn(service, "getContext").mockResolvedValue([]);
    vi.spyOn(service, "getMessages").mockResolvedValue([
      {
        id: "remote-question",
        documentId: "real",
        role: "ai",
        kind: "text",
        text: "Qual cláusula deseja entender?",
      },
    ]);
    vi.spyOn(service, "listDocuments").mockResolvedValue([]);
    vi.spyOn(service, "getProfile").mockResolvedValue({
      name: "HTTP",
      email: "http@example.test",
      fontSize: "large",
      explainLevel: "simple",
      income: "prefer_not",
      audio: false,
    });
    return service;
  }
  it("mantém o card de resumo no topo mesmo quando o servidor já tem histórico", async () => {
    mount(remoteService(), "/documentos/real/chat");
    await advance();
    const card = screen.getByText("ANÁLISE INICIAL");
    const mensagem = screen.getByText("Qual cláusula deseja entender?");
    // O card vem antes da mensagem do servidor na tela.
    expect(
      card.compareDocumentPosition(mensagem) & Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy();
  });
  it("preserva os parágrafos da resposta da IA (linha em branco antes de 'Minha opinião:')", async () => {
    const service = remoteService();
    vi.spyOn(service, "getMessages").mockResolvedValue([
      {
        id: "1",
        documentId: "real",
        role: "ai",
        kind: "text",
        text: "O contrato diz isso.\n\nMinha opinião: parece comum.",
      },
    ]);
    mount(service, "/documentos/real/chat");
    await advance();
    expect(screen.getByText(/Minha opinião: parece comum/)).toHaveStyle({
      whiteSpace: "pre-wrap",
    });
  });
  it("limita o campo de pergunta ao tamanho que o backend aceita", async () => {
    mount(remoteService(), "/documentos/real/chat");
    await advance();
    expect(composer()).toHaveAttribute("maxlength", "2000");
  });
  it("preserva a pergunta do servidor e falha/retry HTTP sem opções, respostas ou aviso locais", async () => {
    const service = remoteService();
    const fetcher = vi
      .fn()
      .mockResolvedValue(new Response("", { status: 503 }));
    vi.stubGlobal("fetch", fetcher);
    mount(service, "/documentos/real/chat");
    await advance();
    expect(
      screen.getByText("Qual cláusula deseja entender?"),
    ).toBeInTheDocument();
    expect(screen.queryByText(question)).not.toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: choice }),
    ).not.toBeInTheDocument();
    expect(composer()).toBeEnabled();
    const u = user();
    await u.type(composer(), "Minha parcela é R$ 2.000");
    await u.click(screen.getByRole("button", { name: "Enviar mensagem" }));
    await advance();
    expect(screen.getByRole("alert")).toHaveTextContent("503");
    expect(composer()).toHaveValue("Minha parcela é R$ 2.000");
    expect(screen.queryByText(/Exemplo fictício:/)).not.toBeInTheDocument();
    expect(
      screen.queryByText(/não substitui orientação/),
    ).not.toBeInTheDocument();
    fetcher.mockResolvedValue(
      new Response(
        JSON.stringify({
          id: "real-reply",
          documentId: "real",
          role: "assistant",
          content: "Resposta recebida do backend.",
          createdAt: "2026-10-16",
        }),
      ),
    );
    await u.click(
      screen.getByRole("button", { name: "Tentar enviar novamente" }),
    );
    await advance();
    expect(
      screen.getByText("Resposta recebida do backend."),
    ).toBeInTheDocument();
    expect(
      screen.getAllByText("Minha parcela é R$ 2.000", { exact: true }),
    ).toHaveLength(1);
    expect(screen.queryByText(question)).not.toBeInTheDocument();
    expect(
      screen.queryByText(/não substitui orientação/),
    ).not.toBeInTheDocument();
    expect(composer()).toHaveValue("");
  });
});
