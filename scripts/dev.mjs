// `npm run dev`: chạy cùng lúc backend (profile dev) và frontend; Ctrl+C để tắt cả hai.
// Đổi cổng khi cần: BACKEND_PORT=8089 FRONTEND_PORT=4201 npm run dev
import { spawn, spawnSync } from 'node:child_process';
import { existsSync, readdirSync, readFileSync } from 'node:fs';
import { connect } from 'node:net';
import { homedir } from 'node:os';
import { delimiter, dirname, join } from 'node:path';
import { createInterface } from 'node:readline';
import { fileURLToPath } from 'node:url';

const root = join(dirname(fileURLToPath(import.meta.url)), '..');
const backendDir = join(root, 'backend');
const frontendDir = join(root, 'frontend');
const windows = process.platform === 'win32';

function fail(message) {
  console.error(`\n[dev] ${message}\n`);
  process.exit(1);
}

function envFileValue(file, key) {
  if (!existsSync(file)) return undefined;
  const line = readFileSync(file, 'utf8').split('\n').find((l) => l.startsWith(`${key}=`));
  return line?.slice(key.length + 1).trim() || undefined;
}

/** Backend cần JDK 21 trở lên; máy có thể đang để mặc định bản cũ hơn. */
function findJavaHome() {
  const sdkman = join(homedir(), '.sdkman', 'candidates', 'java');
  const installed = existsSync(sdkman)
    ? readdirSync(sdkman).filter((v) => v.startsWith('21.')).map((v) => join(sdkman, v))
    : [];
  // '' nghĩa là dùng lệnh `java` trong PATH
  for (const home of [process.env.JAVA_HOME, '', ...installed]) {
    if (home === undefined) continue;
    const java = home ? join(home, 'bin', 'java') : 'java';
    const out = spawnSync(java, ['-version'], { encoding: 'utf8' }).stderr ?? '';
    if (Number(out.match(/version "(\d+)/)?.[1]) >= 21) return home;
  }
  fail('Không tìm thấy JDK 21. Cài JDK 21 (vd. `sdk install java 21-tem`) rồi chạy lại.');
}

function portInUse(port) {
  return new Promise((resolve) => {
    const socket = connect({ port, host: 'localhost' });
    socket.once('connect', () => { socket.destroy(); resolve(true); });
    socket.once('error', () => resolve(false));
  });
}

const backendPort = Number(
  process.env.BACKEND_PORT ?? envFileValue(join(backendDir, '.env'), 'SERVER_PORT') ?? 8080,
);
const frontendPort = Number(process.env.FRONTEND_PORT ?? 4200);

if (!existsSync(join(frontendDir, 'node_modules'))) {
  fail('Frontend chưa cài thư viện. Chạy một lần: cd frontend && npm install');
}
for (const [name, port] of [['backend', backendPort], ['frontend', frontendPort]]) {
  if (await portInUse(port)) {
    fail(`Cổng ${port} (${name}) đang được dùng. Tắt tiến trình đang chạy ở cổng đó (vd. nút Run trong IDE) rồi chạy lại.`);
  }
}

const javaHome = findJavaHome();
const children = [];
let stopping = false;

function signalAll(signal) {
  for (const child of children) {
    if (child.exitCode !== null || child.signalCode !== null) continue;
    try {
      if (windows) spawnSync('taskkill', ['/pid', String(child.pid), '/T', '/F']);
      else process.kill(-child.pid, signal); // cả nhóm: mvnw → java, npm → ng
    } catch {
      // tiến trình đã thoát
    }
  }
}

function stop(code) {
  if (stopping) return;
  stopping = true;
  process.exitCode = code;
  signalAll('SIGTERM');
  setTimeout(() => signalAll('SIGKILL'), 10_000).unref();
}

function run(label, color, command, args, cwd, env) {
  const child = spawn(command, args, {
    cwd,
    env: { ...process.env, ...env },
    stdio: ['ignore', 'pipe', 'pipe'],
    detached: !windows,
    shell: windows,
  });
  children.push(child);
  const prefix = `\x1b[${color}m[${label}]\x1b[0m `;
  for (const stream of [child.stdout, child.stderr]) {
    createInterface({ input: stream }).on('line', (line) => console.log(prefix + line));
  }
  child.on('error', (err) => { console.error(prefix + err.message); stop(1); });
  child.on('exit', (code) => {
    if (!stopping) console.log(`${prefix}đã dừng (mã ${code}), tắt phần còn lại…`);
    stop(code ?? 1);
  });
}

for (const signal of ['SIGINT', 'SIGTERM', 'SIGHUP']) process.on(signal, () => stop(0));

console.log(`[dev] Backend http://localhost:${backendPort} (profile dev), frontend http://localhost:${frontendPort}`);

run('BE', 36, windows ? 'mvnw.cmd' : './mvnw', ['spring-boot:run'], backendDir, {
  ...(javaHome && { JAVA_HOME: javaHome, PATH: join(javaHome, 'bin') + delimiter + process.env.PATH }),
  SPRING_PROFILES_ACTIVE: 'dev',
  SERVER_PORT: String(backendPort),
  // Ảnh đã lưu nằm ở backend/uploads; đường dẫn đầy đủ để không phụ thuộc thư mục chạy
  HAQUA_UPLOAD_DIR: process.env.HAQUA_UPLOAD_DIR ?? join(backendDir, 'uploads'),
});

// Gọi `ng serve` trực tiếp để đổi được cổng, nên phải tự chạy bước sinh environment.ts (prestart)
spawnSync(process.execPath, [join('scripts', 'set-env.mjs')], { cwd: frontendDir, stdio: 'inherit' });
run('FE', 35, 'npm', ['exec', 'ng', '--', 'serve', '--port', String(frontendPort)], frontendDir, {
  BACKEND_URL: `http://localhost:${backendPort}`,
});
