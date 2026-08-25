import { test, expect } from '@playwright/test';
import AxeBuilder from '@axe-core/playwright';

const screens = [
  { name: 'components', desktop: { width: 1024, height: 768 }, mobile: { width: 390, height: 844 } },
  { name: 'login', desktop: { width: 1440, height: 900 }, mobile: { width: 390, height: 844 } },
  { name: 'register', desktop: { width: 1440, height: 900 }, mobile: { width: 390, height: 844 } },
  { name: 'forgot', desktop: { width: 1440, height: 900 }, mobile: { width: 390, height: 844 } },
  { name: 'reset', desktop: { width: 1440, height: 900 }, mobile: { width: 390, height: 844 } },
  { name: 'privacy', desktop: { width: 1440, height: 900 }, mobile: { width: 390, height: 844 } },
  { name: 'dashboard', desktop: { width: 1440, height: 900 }, mobile: { width: 390, height: 844 } },
  { name: 'lobby', desktop: { width: 1440, height: 900 }, mobile: { width: 390, height: 844 } },
  { name: 'uno', desktop: { width: 1440, height: 900 }, mobile: { width: 667, height: 375 } },
  { name: 'dvc', desktop: { width: 1440, height: 900 }, mobile: { width: 667, height: 375 } },
  { name: 'vegas', desktop: { width: 1440, height: 900 }, mobile: { width: 667, height: 375 } },
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

for (const game of ['uno', 'dvc', 'vegas']) {
  test(`${game} phone landscape keeps controls reachable`, async ({ page }) => {
    await page.setViewportSize({ width: 667, height: 375 });
    await page.goto(`/__ui-lab?screen=${game}`);
    await expect(page.getByRole('button', { name: /leave table/i })).toBeVisible();
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
    expect(overflow).toBeLessThanOrEqual(1);
  });
}

test('Las Vegas tablet keeps a two-column casino grid @visual', async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 1024, height: 768 });
  await page.goto('/__ui-lab?screen=vegas');
  const casinos = page.locator('.vegas-casino');
  await expect(casinos).toHaveCount(6);
  const first = await casinos.nth(0).boundingBox();
  const second = await casinos.nth(1).boundingBox();
  const third = await casinos.nth(2).boundingBox();
  expect(first).not.toBeNull();
  expect(second).not.toBeNull();
  expect(third).not.toBeNull();
  expect(Math.abs(first.y - second.y)).toBeLessThanOrEqual(1);
  expect(third.y).toBeGreaterThan(first.y + first.height - 1);
  if (testInfo.project.name === 'chromium') {
    await expect(page).toHaveScreenshot('vegas-1024x768.png', { fullPage: true });
  }
});

test('Las Vegas keeps keyboard focus, reduced motion, and 200% zoom usable', async ({ page }) => {
  await page.emulateMedia({ reducedMotion: 'reduce' });
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto('/__ui-lab?screen=vegas');
  const reveal = page.getByRole('button', { name: 'Reveal total assets' });
  await reveal.focus();
  await expect(reveal).toBeFocused();
  const transitionDuration = await reveal.evaluate((element) => getComputedStyle(element).transitionDuration);
  expect(['0s', '0.001s']).toContain(transitionDuration.split(',')[0]);

  await page.evaluate(() => { document.body.style.zoom = '2'; });
  await expect(page.getByRole('button', { name: 'Leave table' })).toBeVisible();
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  expect(overflow).toBeLessThanOrEqual(1);
});

test('Las Vegas visibly identifies a bot turn and locks human actions', async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto('/__ui-lab?screen=vegas&state=bot');
  await expect(page.getByText(/Bot 1 \(CPU\) is taking their turn/i)).toBeVisible();
  await expect(page.getByText('CPU', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Place all 1s' })).toBeDisabled();
  const textState = JSON.parse(await page.evaluate(() => window.render_game_to_text()));
  expect(textState.currentPlayerId).toBe('BOT1');
  expect(textState.players.find((player) => player.playerId === 'BOT1').bot).toBe(true);
});

test('one human and two Las Vegas bots show separate roll and placement broadcasts', async ({ page }) => {
  await page.goto('/__ui-lab?screen=vegas&state=bot-sequence');
  await page.waitForFunction(() => typeof window.render_game_to_text === 'function'
    && typeof window.advance_las_vegas_bot_fixture === 'function');
  const tableState = () => page.evaluate(() => JSON.parse(window.render_game_to_text()));
  const advance = () => page.evaluate(() => window.advance_las_vegas_bot_fixture());

  await expect.poll(async () => (await tableState()).players.map((player) => player.playerId)).toEqual(['P1', 'BOT1', 'BOT2']);
  await expect.poll(async () => `${(await tableState()).currentPlayerId}:${(await tableState()).mode}`).toBe('BOT1:WAITING_FOR_ROLL');

  await advance();
  await expect.poll(async () => {
    const state = await tableState();
    return `${state.currentPlayerId}:${state.mode}:${state.currentRoll.length}`;
  }).toBe('BOT1:WAITING_FOR_CHOICE:3');

  await advance();
  await expect.poll(async () => `${(await tableState()).currentPlayerId}:${(await tableState()).mode}`).toBe('BOT2:WAITING_FOR_ROLL');
  await expect.poll(async () => (await tableState()).casinos[0].placements[0].playerId).toBe('BOT1');

  await advance();
  await expect.poll(async () => {
    const state = await tableState();
    return `${state.currentPlayerId}:${state.mode}:${state.currentRoll.length}`;
  }).toBe('BOT2:WAITING_FOR_CHOICE:2');

  await advance();
  await expect.poll(async () => `${(await tableState()).currentPlayerId}:${(await tableState()).mode}`).toBe('P1:WAITING_FOR_ROLL');
});

test('Las Vegas phone portrait has no horizontal overflow', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/__ui-lab?screen=vegas&state=bot');
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  expect(overflow).toBeLessThanOrEqual(1);
});

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
