import { describe, it, expect, vi } from "vitest";
import { render, screen, within, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import App from "../app/App";
import { AppProvider } from "../app/AppProvider";
import { MockService } from "../services/mock/MockService";
function mount(path = "/", service = new MockService()) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AppProvider service={service}>
        <App />
      </AppProvider>
    </MemoryRouter>,
  );
}
describe("fluxos React", () => {
  it("navega home → categoria com contador derivado e volta à lista geral", async () => {
    const u = userEvent.setup();
    mount();
    const category = await screen.findByRole("button", {
      name: /🚗.*Carros.*4 documentos/,
    });
    await u.click(category);
    expect(await screen.findByText("Carro Gol 2016")).toBeInTheDocument();
    await u.click(screen.getByRole("button", { name: /Voltar/ }));
    await u.click(await screen.findByRole("button", { name: "Ver todos" }));
    expect(
      await screen.findByText("25 documentos no total"),
    ).toBeInTheDocument();
  });
  it("aplica query string de filtro e busca e limpa estado vazio", async () => {
    const u = userEvent.setup();
    mount("/documentos?filtro=warning&busca=gol");
    expect(
      await screen.findByText("Financiamento Gol 2016"),
    ).toBeInTheDocument();
    expect(screen.queryByText("Seguro Gol 2016")).not.toBeInTheDocument();
    await u.clear(screen.getByRole("textbox", { name: "Buscar documento" }));
    await u.type(
      screen.getByRole("textbox", { name: "Buscar documento" }),
      "inexistente",
    );
    expect(
      await screen.findByText("Nenhum documento encontrado"),
    ).toBeInTheDocument();
    await u.click(screen.getByRole("button", { name: "Ver todos" }));
    expect(screen.getByText("Carro Gol 2016")).toBeInTheDocument();
  });
  it("mostra 404 para categoria inválida sem assertion sobre find", async () => {
    mount("/categorias/invalida");
    expect(
      await screen.findByRole("heading", { name: "Página não encontrada" }),
    ).toBeInTheDocument();
  });
  it("retém o arquivo na confirmação e permite cancelar com Escape", async () => {
    const u = userEvent.setup();
    mount();
    await u.click(
      await screen.findByRole("button", { name: /Enviar documento/ }),
    );
    const dialog = await screen.findByRole("dialog");
    await u.upload(
      within(dialog).getByLabelText("Escolher arquivo", { selector: "input" }),
      new File(["%PDF"], "contrato.pdf", { type: "application/pdf" }),
    );
    expect(await screen.findByText(/contrato.pdf ·/)).toBeInTheDocument();
    expect(
      screen.getByText(/o arquivo não sai do navegador/),
    ).toBeInTheDocument();
    await u.keyboard("{Escape}");
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });
  it("edita perfil e preserva nome/preferência entre rotas", async () => {
    const u = userEvent.setup();
    mount("/perfil");
    await u.click(
      await screen.findByRole("button", { name: /Mudar meus dados/ }),
    );
    await u.clear(screen.getByLabelText("Seu nome"));
    await u.type(screen.getByLabelText("Seu nome"), "Ana Kaup");
    await u.click(screen.getByRole("button", { name: /Salvar/ }));
    expect(await screen.findByText("Ana Kaup")).toBeInTheDocument();
    await u.click(screen.getByRole("button", { name: /Normal Explicações/ }));
    await waitFor(() =>
      expect(
        screen.getByRole("button", { name: /Normal Explicações/ }),
      ).toHaveTextContent("Escolhido"),
    );
    await u.click(
      screen.getByRole("button", { name: /Ver todos os documentos/ }),
    );
    await screen.findByText("25 documentos no total");
  });
  it("mantém edição e erro visível quando o serviço de perfil falha", async () => {
    const service = new MockService();
    vi.spyOn(service, "updateProfile").mockRejectedValue(
      new Error("Falha HTTP no perfil"),
    );
    const user = userEvent.setup();
    mount("/perfil", service);
    await user.click(
      await screen.findByRole("button", { name: /Mudar meus dados/ }),
    );
    await user.clear(screen.getByLabelText("Seu nome"));
    await user.type(screen.getByLabelText("Seu nome"), "Nome ainda não salvo");
    await user.click(screen.getByRole("button", { name: /Salvar/ }));
    const dialog = screen.getByRole("dialog", { name: "Editar perfil" });
    expect(await within(dialog).findByRole("alert")).toHaveTextContent(
      "Falha HTTP no perfil",
    );
    expect(within(dialog).getByLabelText("Seu nome")).toHaveValue(
      "Nome ainda não salvo",
    );
  });
});
