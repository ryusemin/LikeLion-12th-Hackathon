const { defineConfig, devices } = require('@playwright/test');

module.exports = defineConfig({
  testDir: './e2e',
  workers: 1,
  forbidOnly: !!process.env.CI,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: 'http://127.0.0.1:3001',
    serviceWorkers: 'block',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: {
    command: 'npm start',
    url: 'http://127.0.0.1:3001',
    reuseExistingServer: false,
    timeout: 120000,
    env: {
      BROWSER: 'none', HOST: '127.0.0.1', PORT: '3001',
      REACT_APP_API_BASE_URL: 'http://127.0.0.1:8080',
      REACT_APP_WS_URL: 'http://127.0.0.1:8080/ws/notification',
      REACT_APP_APP_BASE_URL: 'http://127.0.0.1:3001',
    },
  },
});
