import { cpSync, existsSync, mkdirSync, rmSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDir = dirname(fileURLToPath(import.meta.url));
const frontendRoot = resolve(scriptDir, '..');
const buildDir = resolve(frontendRoot, 'build');
const backendStaticDir = resolve(frontendRoot, '../backend/src/main/resources/static');

if (!existsSync(buildDir)) {
  console.error('Build directory not found. Run "npm run build" first.');
  process.exit(1);
}

if (existsSync(backendStaticDir)) {
  rmSync(backendStaticDir, { recursive: true, force: true });
}
mkdirSync(backendStaticDir, { recursive: true });
cpSync(buildDir, backendStaticDir, { recursive: true });
console.log(`Copied ${buildDir} -> ${backendStaticDir}`);
