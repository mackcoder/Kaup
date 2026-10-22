// HTTP fixtures are only test infrastructure. No fake API is shipped in the app.
import { chromium } from "playwright";
import fs from "node:fs/promises";
import assert from "node:assert/strict";
const b = await chromium.launch({
  executablePath: process.env.CHROME_PATH ?? "/usr/bin/google-chrome",
  args: ["--no-sandbox"],
});
const context = await b.newContext({ viewport: { width: 390, height: 844 } });
const page = await context.newPage();
page.setDefaultTimeout(10000);
const result = {
  mode: "api",
  backend: "interceptado exclusivamente no teste",
  checks: [],
  errors: [],
};
page.on("pageerror", (e) => result.errors.push(e.message));
const dto = {
  id: "http-document",
  name: "arquivo-http.pdf",
  categoryId: "Casa",
  processingStatus: "pending",
  createdAt: "2026-10-16T12:00:00Z",
};
const profile = {
  name: "Usuário HTTP",
  email: "http@example.test",
  fontSize: "large",
  explainLevel: "simple",
  income: "prefer_not",
  audio: false,
};
let ready = false,
  uploaded = false,
  chatFailure = true,
  polls = 0;
async function json(route, data, status = 200) {
  await route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(data),
  });
}
await page.route("http://localhost:5176/api/**", async (route) => {
  const req = route.request();
  const url = new URL(req.url());
  if (!ready) {
    await route.fulfill({ status: 404, body: "Not implemented" });
    return;
  }
  if (url.pathname === "/api/perfil") {
    await json(route, req.method() === "PATCH" ? req.postDataJSON() : profile);
    return;
  }
  if (url.pathname === "/api/documentos") {
    if (req.method() === "POST") {
      const contentType = req.headers()["content-type"];
      assert.ok(contentType.startsWith("multipart/form-data; boundary="));
      assert.ok(req.postDataBuffer().includes(Buffer.from("arquivo-http.pdf")));
      uploaded = true;
      await json(route, dto, 202);
      return;
    }
    await json(route, uploaded ? [dto] : []);
    return;
  }
  if (url.pathname.endsWith("/analise")) {
    polls++;
    await json(route, {
      documentId: dto.id,
      processingStatus: polls < 2 ? "processing" : "completed",
      summary: "Resumo retornado pelo servidor de teste.",
      keyPoints: [
        {
          id: "point-1",
          title: "Cláusula de teste",
          description: "Detalhe HTTP",
          severity: "info",
        },
      ],
    });
    return;
  }
  if (url.pathname.endsWith("/chat/mensagens")) {
    if (req.method() === "POST") {
      assert.equal(req.postDataJSON().message, "Pergunta com conexão instável");
      if (chatFailure) {
        chatFailure = false;
        await route.fulfill({ status: 503 });
        return;
      }
      await json(route, {
        id: "reply-http",
        documentId: dto.id,
        role: "assistant",
        content: "Resposta HTTP contextualizada de teste.",
        createdAt: "2026-10-16T12:00:00Z",
      });
      return;
    }
    await json(route, []);
    return;
  }
  if (url.pathname === "/api/documentos/" + dto.id) {
    await json(route, {
      ...dto,
      processingStatus: polls >= 2 ? "completed" : "pending",
    });
    return;
  }
  await route.fulfill({ status: 404 });
});
try {
  await page.goto("http://localhost:5176");
  await page
    .getByRole("alert")
    .filter({ hasText: "ainda não foi implementado" })
    .waitFor();
  assert.equal(
    await page.getByText("Carro Gol 2016", { exact: true }).count(),
    0,
  );
  assert.equal(
    await page.getByText("João da Silva", { exact: true }).count(),
    0,
  );
  result.checks.push("404 explícito; nenhum fallback para fixture");
  ready = true;
  await page
    .getByRole("button", { name: "Tentar novamente", exact: true })
    .click();
  await page.getByRole("alert").waitFor({ state: "hidden" });
  await page.getByRole("button", { name: /Enviar documento/ }).click();
  await page.getByLabel("Escolher arquivo", { exact: true }).setInputFiles({
    name: "arquivo-http.pdf",
    mimeType: "application/pdf",
    buffer: Buffer.from("%PDF test HTTP"),
  });
  await page.getByRole("button", { name: "Confirmar e enviar" }).click();
  await page.waitForURL("**/chat");
  await page.getByText("Resumo retornado pelo servidor de teste.").waitFor();
  assert.ok(polls >= 2);
  assert.equal(await page.getByText("R$ 1.250,00", { exact: true }).count(), 0);
  result.checks.push(
    "File via FormData; 202 pending → polling processing → completed; resumo HTTP",
  );
  const input = page.getByRole("textbox", {
    name: "Mensagem sobre o documento",
  });
  await input.fill("Pergunta com conexão instável");
  await page.getByRole("button", { name: "Enviar mensagem" }).click();
  await page.getByRole("alert").filter({ hasText: "503" }).waitFor();
  assert.equal(await input.inputValue(), "Pergunta com conexão instável");
  await page.getByRole("button", { name: "Tentar enviar novamente" }).click();
  await page.getByText("Resposta HTTP contextualizada de teste.").waitFor();
  result.checks.push(
    "503 no chat mantém texto; retry retorna resposta HTTP por documentId",
  );
  await page.screenshot({
    path: "validation/api-http-chat.png",
    fullPage: true,
  });
  await page.goto("http://localhost:5176/perfil");
  await page.getByText("Usuário HTTP", { exact: true }).waitFor();
  await page.getByRole("button", { name: /Mudar meus dados/ }).click();
  await page.getByLabel("Seu nome").fill("Nome atualizado HTTP");
  await page.getByRole("button", { name: /Salvar/ }).click();
  await page.getByText("Nome atualizado HTTP", { exact: true }).waitFor();
  result.checks.push("GET/PATCH de perfil usa contrato HTTP");
  assert.deepEqual(result.errors, []);
  result.status = "passed";
} catch (error) {
  result.status = "failed";
  result.failure = error.message;
  result.body = await page.locator("body").innerText();
  await page.screenshot({ path: "validation/api-failure.png", fullPage: true });
  throw error;
} finally {
  await fs.writeFile(
    "validation/api-browser-results.json",
    JSON.stringify(result, null, 2),
  );
  await b.close();
}
console.log(JSON.stringify(result, null, 2));
