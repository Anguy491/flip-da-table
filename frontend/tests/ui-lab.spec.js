import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

const screens = [
  { name: 'components', desktop: { width: 1024, height: 768 }, mobile: { width: 390, height: 844 } },
  { name: 'login', desktop: { width: 1440, height: 900 }, mobile: { width: 390, height: 844 } },
  { name: 'register', desktop: { width: 1440, height: 900 }, mobile: { width: 390, height: 844 } },
  { name: 'dashboard', desktop: { width: 1440, height: 900 }, mobile: { width: 390, height: 844 } },
  { name: 'lobby', desktop: { width: 1440, height: 900 }, mobile: { width: 390, height: 844 } },
  { name: 'uno', desktop: { width: 1440, height: 900 }, mobile: { width: 667, height: 375 } },
  { name: 'dvc', desktop: { width: 1440, height: 900 }, mobile: { width: 667, height: 375 } },
  { name: 'summary', desktop: { width: 1440, height: 900 }, mobile: { width: 390, height: 844 } },
];

for (const screen of screens) {
  test(`${screen.name} UI Lab is accessible and stable @visual`, async ({ page }, testInfo) => {
    await page.setViewportSize(screen.desktop);
    await page.goto(`/__ui-lab?screen=${screen.name}`);
    await expect(page.locator('main')).toBeVisible();
    await expect(page.locator('body')).toHaveCSS('overflow-x', 'hidden');

    const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa']).analyze();
    const severe = results.violations.filter((violation) => ['serious', 'critical'].includes(violation.impact));
    expect(severe).toEqual([]);

    if (testInfo.project.name === 'chromium') {
      await expect(page).toHaveScreenshot(`${screen.name}-${screen.desktop.width}x${screen.desktop.height}.png`, { fullPage: true });
    }

    await page.setViewportSize(screen.mobile);
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
    expect(overflow).toBeLessThanOrEqual(1);
    if (testInfo.project.name === 'chromium') {
      await expect(page).toHaveScreenshot(`${screen.name}-${screen.mobile.width}x${screen.mobile.height}.png`, { fullPage: true });
    }
  });
}

test('ordinary mobile layout has no page overflow', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/__ui-lab?screen=components');
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  expect(overflow).toBeLessThanOrEqual(1);
});

for (const game of ['uno', 'dvc']) {
  test(`${game} phone landscape keeps controls reachable`, async ({ page }) => {
    await page.setViewportSize({ width: 667, height: 375 });
    await page.goto(`/__ui-lab?screen=${game}`);
    await expect(page.getByRole('button', { name: /leave table/i })).toBeVisible();
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
    expect(overflow).toBeLessThanOrEqual(1);
  });
}

test('dialog traps focus and closes with Escape', async ({ page }) => {
  await page.goto('/__ui-lab?screen=components');
  await page.getByRole('button', { name: 'Open dialog' }).click();
  await expect(page.getByRole('dialog', { name: 'Shared dialog' })).toBeVisible();
  await expect(page.getByRole('textbox', { name: 'Invite code' })).toBeFocused();
  await page.keyboard.press('Escape');
  await expect(page.getByRole('dialog')).toHaveCount(0);
});

test('confirmed DVC rack has no reorder affordance', async ({ page }) => {
  await page.goto('/__ui-lab?screen=dvc&state=settled');
  await expect(page.getByText(/rack is locked/i)).toBeVisible();
  await expect(page.locator('.dvc-card-hit[draggable="true"]')).toHaveCount(0);
  await expect(page.locator('.dvc-card-hit').first()).toBeDisabled();
});

test('DVC game log shows guess outcomes without covering the rack', async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto('/__ui-lab?screen=dvc');
  await expect(page.getByRole('log', { name: /game log/i })).toBeVisible();
  await expect(page.getByText(/guessed CipherFox's #3 WHITE tile as 6 — WRONG/i)).toBeVisible();
  await expect(page.getByText(/guessed PixelPilot's #2 WHITE tile as 3 — CORRECT/i)).toBeVisible();
  const handBox = await page.locator('.dvc-rack__hand').boundingBox();
  const logBox = await page.getByRole('log', { name: /game log/i }).boundingBox();
  expect(handBox).not.toBeNull();
  expect(logBox).not.toBeNull();
  expect(logBox.y).toBeGreaterThanOrEqual(handBox.y + handBox.height);

  await page.setViewportSize({ width: 667, height: 375 });
  await page.reload();
  const mobileRackBox = await page.locator('.dvc-rack').boundingBox();
  const mobileHandBox = await page.locator('.dvc-rack__hand').boundingBox();
  const mobileLogBox = await page.getByRole('log', { name: /game log/i }).boundingBox();
  expect(mobileRackBox).not.toBeNull();
  expect(mobileHandBox).not.toBeNull();
  expect(mobileLogBox).not.toBeNull();
  expect(mobileLogBox.height).toBeGreaterThan(0);
  expect(mobileLogBox.y).toBeGreaterThanOrEqual(mobileHandBox.y + mobileHandBox.height);
  expect(mobileLogBox.y + mobileLogBox.height).toBeLessThanOrEqual(mobileRackBox.y + mobileRackBox.height + 1);
});
