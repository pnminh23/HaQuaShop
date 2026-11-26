// Sinh src/environments/environment.ts từ frontend/.env (hoặc biến môi trường của shell).
// Chạy tự động qua prestart / prebuild.
import { existsSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const fromFile = {};
const envPath = join(root, '.env');
if (existsSync(envPath)) {
  for (const line of readFileSync(envPath, 'utf8').split('\n')) {
    const m = line.match(/^\s*([A-Z0-9_]+)\s*=\s*(.*?)\s*$/);
    if (m && !line.trim().startsWith('#')) fromFile[m[1]] = m[2].replace(/^["']|["']$/g, '');
  }
}
const get = (key, fallback) => process.env[key] ?? fromFile[key] ?? fallback;

const content = `// FILE SINH TỰ ĐỘNG bởi scripts/set-env.mjs từ .env – sửa .env chứ không sửa file này.
export const environment = {
  apiUrl: '${get('API_URL', '/api')}',
  assetsUrl: '${get('ASSETS_URL', '')}',
};
`;
writeFileSync(join(root, 'src/environments/environment.ts'), content);
console.log('environment.ts <-', get('API_URL', '/api'));
