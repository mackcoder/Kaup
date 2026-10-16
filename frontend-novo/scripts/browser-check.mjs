import { composeScreenshots } from "./compose-screenshots.mjs";
import { chromium } from "playwright";
import fs from "node:fs/promises";
import assert from "node:assert/strict";
const browser = await chromium.launch({
  executablePath: process.env.CHROME_PATH ?? "/usr/bin/google-chrome",
  headless: true,
  args: ["--no-sandbox"],
});
const results = { date: "2026-10-16", viewports: [], flows: [], errors: [] };
await fs.mkdir("validation", { recursive: true });
const oldUrl = "http://127.0.0.1:5175",
  newUrl = "http://127.0.0.1:5174";
async function ready(page) {
  await page.evaluate(() => document.fonts.ready);
  await page.waitForTimeout(250);
}
async function capture(page, name) {
  await ready(page);
  await page.screenshot({ path: `validation/${name}.png`, fullPage: true });
}
try {
  for (const viewport of [
    { width: 390, height: 844 },
    { width: 430, height: 932 },
    { width: 1280, height: 800 },
  ]) {
    const context = await browser.newContext({ viewport });
    const old = await context.newPage(),
      next = await context.newPage();
    for (const page of [old, next])
      page.on("pageerror", (e) => results.errors.push(e.message));
    await Promise.all([old.goto(oldUrl), next.goto(newUrl)]);
    await Promise.all([ready(old), ready(next)]);
    const size = `${viewport.width}x${viewport.height}`;
    for (const [page, prefix] of [
      [old, "original"],
      [next, "novo"],
    ]) {
      await capture(page, `${size}-${prefix}-home`);
      await page
        .getByRole("button", { name: "Ver todos", exact: true })
        .click();
      await page.waitForTimeout(350);
      await capture(page, `${size}-${prefix}-documentos`);
      await page.getByRole("button", { name: /Voltar/ }).click();
      await page.waitForTimeout(350);
      await page
        .getByRole("button", { name: /🚗.*Carros.*documentos/ })
        .click();
      await page.waitForTimeout(350);
      await capture(page, `${size}-${prefix}-categoria`);
      await page
        .getByRole("button", { name: /Carro Gol 2016/ })
        .first()
        .click();
      await page.waitForTimeout(1900);
      // Compare at the start of the conversation after the seed messages.
      await page.getByText("Qual é a sua renda mensal aproximada?").waitFor();
      await page.evaluate(() => {
        for (const el of document.querySelectorAll("div")) {
          if (getComputedStyle(el).overflowY === "auto") el.scrollTop = 0;
        }
      });
      await page.waitForTimeout(500);
      await capture(page, `${size}-${prefix}-chat`);
      await page.getByRole("button", { name: /Contexto/ }).click();
      await capture(page, `${size}-${prefix}-contexto`);
      await page.mouse.click(5, viewport.height / 2);
      await page.getByRole("button", { name: /Voltar/ }).click();
      await page.waitForTimeout(200);
      await page.getByRole("button", { name: /Voltar/ }).click();
      await page.waitForTimeout(200);
      await page
        .getByRole("button", {
          name: prefix === "novo" ? "Meu perfil" : "👤",
          exact: true,
        })
        .click();
      await page.waitForTimeout(250);
      await capture(page, `${size}-${prefix}-perfil`);
    }
    const geometry = await next.evaluate(() => ({
      width: document.documentElement.scrollWidth,
      viewport: innerWidth,
      font: getComputedStyle(document.body).fontFamily,
    }));
    assert.ok(geometry.width <= viewport.width, `Horizontal overflow ${size}`);
    results.viewports.push({ size, geometry });
    await context.close();
  }
  const ctx = await browser.newContext({
    viewport: { width: 390, height: 844 },
  });
  const page = await ctx.newPage();
  page.on("pageerror", (e) => results.errors.push(e.message));
  await page.goto(newUrl);
  await ready(page);
  await page.getByRole("button", { name: /Enviar documento/ }).click();
  await page.getByRole("dialog").waitFor();
  await page.getByLabel("Escolher arquivo", { exact: true }).setInputFiles({
    name: "teste-contrato.pdf",
    mimeType: "application/pdf",
    buffer: Buffer.from("%PDF-1.4 demo"),
  });
  await page.getByRole("button", { name: "Confirmar e enviar" }).click();
  await page.waitForURL("**/chat");
  await page
    .getByText(/Não li o conteúdo do arquivo/)
    .waitFor({ timeout: 8000 });
  await page
    .getByRole("button", { name: "Prefiro não informar", exact: true })
    .click();
  await page.getByRole("button", { name: /Enviar resposta/ }).click();
  await page.getByText(/não substitui orientação/).waitFor();
  assert.equal(
    await page
      .getByRole("note")
      .filter({ hasText: "Não foram extraídos" })
      .count(),
    1,
  );
  await page
    .getByRole("textbox", { name: "Mensagem sobre o documento" })
    .fill("Qual a multa?");
  await page.getByRole("button", { name: "Enviar mensagem" }).click();
  await page.getByText(/Exemplo fictício: A multa por cancelamento/).waitFor();
  await capture(page, "fluxo-upload-chat");
  results.flows.push(
    "upload PDF → confirmação → exemplo fictício rotulado → renda opcional → chat ilustrativo",
  );
  await page.getByRole("button", { name: /Contexto/ }).click();
  await page
    .getByRole("button", { name: /Adicionar arquivo ao contexto/ })
    .click();
  await page.getByLabel("Escolher arquivo", { exact: true }).setInputFiles({
    name: "apoio.png",
    mimeType: "image/png",
    buffer: Buffer.from(
      "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVQIHWP4z8DwHwAFgAI/ScLbtAAAAABJRU5ErkJggg==",
      "base64",
    ),
  });
  await page.getByAltText("Prévia do documento selecionado").waitFor();
  await page.getByRole("button", { name: "Confirmar e enviar" }).click();
  await page.getByRole("button", { name: /Contexto/ }).click();
  await page.getByText("apoio.png", { exact: true }).waitFor();
  results.flows.push(
    "anexo de imagem → preview → contexto do documento atualizado",
  );
  await page.keyboard.press("Escape");
  await page.getByRole("button", { name: /Voltar/ }).click();
  assert.equal(
    await page.getByRole("button", { name: /Outros.*7 documentos/ }).count(),
    1,
  );
  await page.getByRole("button", { name: "Ver todos", exact: true }).click();
  await page.getByText("26 documentos no total").waitFor();
  await page
    .getByRole("textbox", { name: "Buscar documento" })
    .fill("teste-contrato");
  await page.reload();
  await page.getByText("Nenhum documento encontrado").waitFor();
  results.flows.push(
    "contadores 25→26; busca na URL; reload limpa apenas dados de sessão",
  );
  await page.goto(newUrl + "/perfil");
  await page.getByRole("button", { name: /Mudar meus dados/ }).click();
  await page.getByLabel("Seu nome").fill("Ana Kaup");
  await page.getByRole("button", { name: /Salvar/ }).click();
  await page.getByText("Ana Kaup", { exact: true }).waitFor();
  await page.getByRole("button", { name: /A Média/ }).click();
  await page.waitForTimeout(250);
  await page.getByRole("button", { name: /Apagar minha conta/ }).click();
  await page.getByRole("button", { name: /Sim, apagar/ }).click();
  await page.getByText("🗑️ Conta apagada (simulação)").waitFor();
  await page.getByRole("button", { name: /Fechar/ }).click();
  await page.getByRole("button", { name: /Sair da conta/ }).click();
  await page.getByRole("button", { name: "Sim, sair", exact: true }).click();
  await page.getByText("Até logo!").waitFor();
  results.flows.push(
    "edição de perfil, fonte, exclusão e logout demonstrativos",
  );
  await ctx.close();
  assert.deepEqual(results.errors, []);
  await composeScreenshots(browser);
  results.status = "passed";
} finally {
  await fs.writeFile(
    "validation/browser-results.json",
    JSON.stringify(results, null, 2),
  );
  await browser.close();
}
console.log(JSON.stringify(results, null, 2));
