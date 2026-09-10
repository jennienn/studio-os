import { expect, test } from "@playwright/test";

test("landing preview remains responsive and demo is disabled by default", async ({ page }) => {
  const errors: string[] = [];
  page.on("pageerror", error => errors.push(error.message));
  await page.goto("/");
  await expect(page.getByRole("heading", { level: 1 })).toContainText("고객에게 집중하세요.");
  await page.getByRole("button", { name: "뷰티 / 예약샵" }).click();
  await expect(page.getByText("젤 네일", { exact: true })).toBeVisible();
  await expect(page.getByText("개인 2 · 그룹 1", { exact: true })).toHaveCount(0);
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(true);
  await page.getByRole("link", { name: /데모 둘러보기/ }).click();
  await expect(page.getByRole("heading", { name: "서비스를 준비하고 있어요." })).toBeVisible();
  expect(await page.evaluate(() => localStorage.getItem("studio-os-demo-v2"))).toBeNull();
  expect(errors).toEqual([]);
});
