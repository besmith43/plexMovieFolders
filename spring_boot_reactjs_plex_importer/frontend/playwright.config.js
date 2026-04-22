import { defineConfig } from '@playwright/test';
import path from 'node:path';

const repoRoot = path.resolve(import.meta.dirname, '..');

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  retries: 0,
  timeout: 30_000,
  expect: {
    timeout: 10_000,
  },
  reporter: 'list',
  use: {
    baseURL: 'http://127.0.0.1:8080',
    headless: true,
    trace: 'retain-on-failure',
  },
  webServer: {
    command: 'bash -lc "export SOURCE=test_root_dir DEST=test_dest_dir SERVER_PORT=8080; ./gradlew --no-daemon bootRun"',
    cwd: repoRoot,
    port: 8080,
    reuseExistingServer: false,
    timeout: 120_000,
  },
});
