import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

function parseProperties(content) {
  const result = {};
  for (const line of content.split('\n')) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) continue;
    const idx = trimmed.indexOf('=');
    if (idx === -1) continue;
    result[trimmed.slice(0, idx).trim()] = trimmed.slice(idx + 1).trim();
  }
  return result;
}

const i18nDir = path.resolve(__dirname, '../../src/main/resources/i18n');
const outDir = path.resolve(__dirname, '../src/i18n/bundled');
fs.mkdirSync(outDir, { recursive: true });

for (const lang of ['en', 'ru']) {
  const content = fs.readFileSync(path.join(i18nDir, `messages_${lang}.properties`), 'utf8');
  fs.writeFileSync(
    path.join(outDir, `${lang}.json`),
    JSON.stringify(parseProperties(content), null, 2),
  );
}

const publicUrl = process.env.VITE_PUBLIC_URL || 'http://localhost:8080';
const sitemap = `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
  <url>
    <loc>${publicUrl}/</loc>
  </url>
</urlset>
`;
fs.writeFileSync(path.resolve(__dirname, '../public/sitemap.xml'), sitemap);
