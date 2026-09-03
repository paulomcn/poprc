import { readFile, stat } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import path from "node:path";

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const html = await readFile(path.join(root, "dist", "index.html"), "utf8");
const entry = html.match(/<script[^>]+src="\/assets\/([^"]+\.js)"/)?.[1];

if (!entry) {
  throw new Error("Não foi possível localizar o JavaScript inicial em dist/index.html.");
}

const bytes = (await stat(path.join(root, "dist", "assets", entry))).size;
const budgetBytes = 300 * 1024;

if (bytes > budgetBytes) {
  throw new Error(
    `Bundle inicial acima do limite: ${(bytes / 1024).toFixed(1)} kB `
      + `(máximo ${budgetBytes / 1024} kB). Revise imports estáticos de páginas ou bibliotecas pesadas.`,
  );
}

console.log(`Bundle inicial aprovado: ${(bytes / 1024).toFixed(1)} kB de ${budgetBytes / 1024} kB.`);
