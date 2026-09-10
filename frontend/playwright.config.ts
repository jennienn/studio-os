import { defineConfig, devices } from "@playwright/test";
export default defineConfig({
  testDir: "./e2e",
  fullyParallel: true,
  timeout: 60000,
  use: { baseURL: "http://127.0.0.1:31741", channel: "chromium" },
  projects: [
    { name: "desktop", use: { ...devices["Desktop Chrome"] } },
    { name: "mobile", use: { ...devices["iPhone 13"], defaultBrowserType: "chromium" } },
  ],
  webServer: [{
    command: "cd ../backend && ./gradlew browserTestServer --no-daemon",
    url: "http://127.0.0.1:8080/actuator/health",
    reuseExistingServer: false,
    timeout: 180000,
  }, {
    command: "npm run start -- --hostname 127.0.0.1 --port 31741",
    url: "http://127.0.0.1:31741",
    reuseExistingServer: false,
  }],
});
