import fs from "node:fs/promises";
export async function composeScreenshots(browser) {
  const page = await browser.newPage();
  const dimensions = [];
  for (const size of ["390x844", "430x932", "1280x800"]) {
    for (const screen of [
      "home",
      "categoria",
      "documentos",
      "chat",
      "perfil",
      "contexto",
    ]) {
      const paths = ["original", "novo"].map(
        (mode) => `validation/${size}-${mode}-${screen}.png`,
      );
      const sources = await Promise.all(
        paths.map(
          async (path) =>
            "data:image/png;base64," +
            (await fs.readFile(path)).toString("base64"),
        ),
      );
      const result = await page.evaluate(
        async ({ sources, desktop }) => {
          const pictures = await Promise.all(
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
          const [a, b] = pictures;
          const width = desktop ? 470 : a.width;
          const height = Math.max(a.height, b.height) + 30;
          const canvas = document.createElement("canvas");
          canvas.width = width * 2 + 12;
          canvas.height = height;
          const ctx = canvas.getContext("2d");
          ctx.fillStyle = "#e2e8f0";
          ctx.fillRect(0, 0, canvas.width, height);
          ctx.fillStyle = "#1e293b";
          ctx.font = "12px sans-serif";
          ctx.fillText("Original", 8, 18);
          ctx.fillText("Novo", width + 20, 18);
          pictures.forEach((picture, i) => {
            const x = desktop ? (picture.width - 430) / 2 - 20 : 0;
            ctx.drawImage(
              picture,
              x,
              0,
              width,
              picture.height,
              i * (width + 12),
              30,
              width,
              picture.height,
            );
          });
          return {
            png: canvas.toDataURL("image/png"),
            originalHeight: a.height,
            newHeight: b.height,
          };
        },
        { sources, desktop: size === "1280x800" },
      );
      await fs.writeFile(
        `validation/${size}-comparacao-${screen}.png`,
        Buffer.from(result.png.split(",")[1], "base64"),
      );
      dimensions.push({
        viewport: size,
        page: screen,
        originalHeight: result.originalHeight,
        newHeight: result.newHeight,
      });
    }
  }
  await page.close();
  await fs.writeFile(
    "validation/visual-dimensions.json",
    JSON.stringify(dimensions, null, 2),
  );
}
