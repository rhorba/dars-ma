import { defineConfig } from '@playwright/test';

// Points at whatever stack is already running (docker-compose or docker-compose.prod.yml) -
// this suite doesn't start its own servers, it exercises a live deployment end-to-end.
const baseURL = process.env.E2E_BASE_URL ?? 'http://localhost:4200';

export default defineConfig({
  testDir: './tests',
  timeout: 90_000,
  expect: { timeout: 10_000 },
  fullyParallel: false, // shared DB state across the fr/ar critical-path runs; keep sequential
  retries: 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: process.env.E2E_RECORD_VIDEO === 'true' ? 'on' : 'retain-on-failure'
  }
});
