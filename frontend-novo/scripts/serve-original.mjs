// Read-only comparison server: serves the original, pre-existing dist/.
import http from "node:http";
import fs from "node:fs/promises";
import path from "node:path";
const root = path.resolve("../frontend/src/dist");
const mime = {
  ".js": "text/javascript",
  ".css": "text/css",
  ".html": "text/html",
  ".woff2": "font/woff2",
};
http
  .createServer(async (req, res) => {
    try {
      const url = new URL(req.url, "http://localhost");
      const filename = path.join(
        root,
        url.pathname === "/" ? "index.html" : decodeURIComponent(url.pathname),
      );
      if (!filename.startsWith(root + path.sep))
        throw new Error("invalid path");
      res.setHeader(
        "Content-Type",
        mime[path.extname(filename)] ?? "application/octet-stream",
      );
      res.end(await fs.readFile(filename));
    } catch {
      res.statusCode = 404;
      res.end("Not found");
    }
  })
  .listen(5175, "127.0.0.1", () =>
    console.log("Original (read-only dist): http://127.0.0.1:5175"),
  );
