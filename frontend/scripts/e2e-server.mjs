import { readFile, writeFile } from "node:fs/promises";
import { spawn } from "node:child_process";

const nextEnvPath = new URL("../next-env.d.ts", import.meta.url);
const originalNextEnv = await readFile(nextEnvPath, "utf8");
const environment = { ...process.env, NEXT_DIST_DIR: ".next-e2e" };

function run(command, args) {
  return new Promise((resolve) => {
    const child = spawn(command, args, { env: environment, stdio: "inherit" });
    child.once("exit", (code, signal) => resolve({ code, signal }));
  });
}

const build = await run("npm", ["run", "build"]);
await writeFile(nextEnvPath, originalNextEnv);
if (build.code !== 0) process.exit(build.code ?? 1);

const server = spawn("npm", ["run", "start", "--", "--hostname", "127.0.0.1", "--port", "31741"], {
  env: environment,
  stdio: "inherit",
});
for (const signal of ["SIGINT", "SIGTERM"]) {
  process.on(signal, () => server.kill(signal));
}
server.once("exit", (code) => process.exit(code ?? 0));
