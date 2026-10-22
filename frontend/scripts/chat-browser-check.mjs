import { chromium } from "playwright";
import assert from "node:assert/strict";
import fs from "node:fs/promises";
const browser = await chromium.launch({
  executablePath: process.env.CHROME_PATH ?? "/usr/bin/google-chrome",
  args: ["--no-sandbox"],
});
const result = { date: "2026-10-16", viewports: [], checks: [], errors: [] };
const option = "Menos de R$ 3.000";
await fs.mkdir("validation", { recursive: true });
async function screenshot(page, name) {
  await page.evaluate(() => document.fonts.ready);
  await page.screenshot({ path: `validation/${name}.png` });
}
async function bottom(page) {
  await page.evaluate(() => {
    for (const el of document.querySelectorAll("div")) {
      if (getComputedStyle(el).overflowY === "auto")
        el.scrollTop = el.scrollHeight;
    }
  });
  await page.waitForTimeout(300);
}
async function compare(page, size, state) {
  const sources = await Promise.all(
    ["original", "novo"].map(
      async (prefix) =>
        "data:image/png;base64," +
        (
          await fs.readFile(`validation/${size}-chat-${prefix}-${state}.png`)
        ).toString("base64"),
    ),
  );
  const png = await page.evaluate(async (sources) => {
    const images = await Promise.all(
      sources.map(
        (src) =>
          new Promise((resolve, reject) => {
            const img = new Image();
            img.onload = () => resolve(img);
            img.onerror = reject;
            img.src = src;
          }),
      ),
    );
    const width = images[0].width;
    const canvas = document.createElement("canvas");
    canvas.width = width * 2 + 12;
    canvas.height = images[0].height + 30;
    const ctx = canvas.getContext("2d");
    ctx.fillStyle = "#e2e8f0";
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.fillStyle = "#1e293b";
    ctx.font = "12px sans-serif";
    ctx.fillText("Original", 8, 18);
    ctx.fillText("Novo", width + 20, 18);
    images.forEach((img, i) => ctx.drawImage(img, i * (width + 12), 30));
    return canvas.toDataURL("image/png");
  }, sources);
  await fs.writeFile(
    `validation/${size}-chat-comparacao-${state}.png`,
    Buffer.from(png.split(",")[1], "base64"),
  );
}
// Use the same local Nunito files in both captures; avoid Google Fonts timing.
const localFontCss = (
  await Promise.all(
    [400, 600, 700, 800, 900].map(async (weight) => {
      const bytes = await fs.readFile(
        `node_modules/@fontsource/nunito/files/nunito-latin-${weight}-normal.woff2`,
      );
      return `@font-face{font-family:'Nunito';font-style:normal;font-weight:${weight};src:url(data:font/woff2;base64,${bytes.toString("base64")}) format('woff2');font-display:swap;}`;
    }),
  )
).join("\n");
try {
  for (const viewport of [
    { width: 390, height: 844 },
    { width: 430, height: 932 },
    { width: 1280, height: 800 },
  ]) {
    const context = await browser.newContext({ viewport });
    await context.route("https://fonts.googleapis.com/**", (route) =>
      route.fulfill({ contentType: "text/css", body: localFontCss }),
    );
    const pairs = [];
    const size = `${viewport.width}x${viewport.height}`;
    const styles = [];
    for (const [prefix, url] of [
      ["original", "http://localhost:5175"],
      ["novo", "http://localhost:5174"],
    ]) {
      const page = await context.newPage();
      page.setDefaultTimeout(12000);
      page.on("pageerror", (e) => result.errors.push(e.message));
      pairs.push(page);
      await page.goto(url, { waitUntil: "domcontentloaded" });
      await page.evaluate(() => document.fonts.ready);
      await page
        .getByRole("button", { name: /🚗.*Carros.*documentos/ })
        .click();
      await page
        .getByRole("button", { name: /Carro Gol 2016/ })
        .first()
        .click();
      await page.getByRole("button", { name: option, exact: true }).click();
      await page.waitForTimeout(200);
      assert.equal(
        await page.getByRole("button", { name: /Enviar resposta/ }).count(),
        1,
      );
      if (prefix === "novo")
        assert.equal(
          await page
            .getByRole("button", { name: option, exact: true })
            .getAttribute("aria-pressed"),
          "true",
        );
      styles.push(
        await page
          .getByRole("button", { name: option, exact: true })
          .evaluate((el) => {
            const s = getComputedStyle(el);
            return {
              padding: s.padding,
              border: s.border,
              borderRadius: s.borderRadius,
              background: s.backgroundColor,
              fontFamily: s.fontFamily,
            };
          }),
      );
      await bottom(page);
      await screenshot(page, `${size}-chat-${prefix}-renda`);
      await page.getByRole("button", { name: /Enviar resposta/ }).click();
      if (prefix === "novo")
        await page
          .getByRole("status", { name: "Assistente digitando" })
          .waitFor();
      await page.getByText(/não substitui orientação/).waitFor();
      await page.getByRole("textbox").waitFor();
      assert.equal(await page.getByText(/mais de 40%/).count(), 1);
      assert.equal(await page.getByText(/não substitui orientação/).count(), 1);
      await bottom(page);
      await screenshot(page, `${size}-chat-${prefix}-resposta`);
      await page.getByRole("textbox").fill("Qual a multa?");
      await page
        .getByRole("button", {
          name: prefix === "novo" ? "Enviar mensagem" : "↑",
          exact: true,
        })
        .click();
      await page.getByText(/A multa por cancelamento antecipado/).waitFor();
      if (prefix === "novo") {
        await page.getByRole("button", { name: /Voltar/ }).click();
        await page
          .getByRole("button", { name: /Carro Gol 2016/ })
          .first()
          .click();
        await page.getByText(/A multa por cancelamento antecipado/).waitFor();
        assert.equal(
          await page.getByRole("button", { name: option, exact: true }).count(),
          0,
        );
      }
    }
    assert.deepEqual(styles[0], styles[1]);
    await compare(pairs[1], size, "renda");
    await compare(pairs[1], size, "resposta");
    result.viewports.push({
      size,
      selectedOptionStyles: styles[1],
      screenshots: ["renda", "resposta"],
      status: "passed",
    });
    await context.close();
  }
  const context = await browser.newContext({
    viewport: { width: 390, height: 844 },
  });
  const page = await context.newPage();
  page.on("pageerror", (e) => result.errors.push(e.message));
  await page.goto("http://localhost:5174");
  await page.getByRole("button", { name: /Enviar documento/ }).click();
  await page.getByLabel("Escolher arquivo", { exact: true }).setInputFiles({
    name: "contrato-novo.pdf",
    mimeType: "application/pdf",
    buffer: Buffer.from("%PDF mock"),
  });
  await page.getByRole("button", { name: "Confirmar e enviar" }).click();
  await page.getByText("Simulando análise", { exact: true }).waitFor();
  assert.equal(
    await page
      .locator(".kaup-154")
      .evaluate((el) => getComputedStyle(el).borderTopColor),
    "rgba(0, 0, 0, 0)",
  );
  await screenshot(page, "chat-upload-processamento");
  await page
    .getByText(/Não li o conteúdo do arquivo e não executei OCR/)
    .waitFor();
  await page.getByRole("note").waitFor();
  await screenshot(page, "chat-upload-exemplo");
  await page
    .getByRole("button", { name: "Prefiro não informar", exact: true })
    .click();
  await page.getByRole("button", { name: /Enviar resposta/ }).click();
  await page.getByText(/não substitui orientação/).waitFor();
  await bottom(page);
  await screenshot(page, "chat-upload-renda-opcional");
  await page
    .getByRole("textbox", { name: "Mensagem sobre o documento" })
    .fill("E os juros?");
  await page.getByRole("button", { name: "Enviar mensagem" }).click();
  await page.getByText(/Os juros são o custo extra/).waitFor();
  result.checks.push(
    "upload → processamento demonstrativo → exemplo rotulado → renda opcional → chat livre",
    "seleção exige confirmação; estilos da opção selecionada iguais ao original",
    "digitação, resposta e aviso; histórico preservado ao sair e retornar",
  );
  await context.close();
  assert.deepEqual(result.errors, []);
  result.status = "passed";
} catch (error) {
  result.status = "failed";
  result.failure = error.message;
  throw error;
} finally {
  await fs.writeFile(
    "validation/chat-browser-results.json",
    JSON.stringify(result, null, 2),
  );
  await browser.close();
}
console.log(JSON.stringify(result, null, 2));
