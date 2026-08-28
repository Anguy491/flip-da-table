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
  { name: 'conquer', desktop: { width: 1440, height: 900 }, mobile: { width: 667, height: 375 } },
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

for (const game of ['uno', 'dvc', 'vegas', 'conquer']) {
  test(`${game} phone landscape keeps controls reachable`, async ({ page }) => {
    await page.setViewportSize({ width: 667, height: 375 });
    await page.goto(`/__ui-lab?screen=${game}`);
    await expect(page.getByRole('button', { name: /leave table|exit/i })).toBeVisible();
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
    expect(overflow).toBeLessThanOrEqual(1);
  });
}

for (const state of ['loading', 'error', 'waiting-to-roll', 'unlocked-target', 'partial-siege', 'double-crown', 'clan-locked', 'bot-turn', 'reconnecting', 'finished']) {
  test(`Conquer Westeros ${state} fixture remains usable`, async ({ page }) => {
    await page.setViewportSize({ width: 1024, height: 768 });
    await page.goto(`/__ui-lab?screen=conquer&state=${state}`);
    await expect(page.locator('main')).toBeVisible();
    if (!['loading', 'error'].includes(state)) await expect(page.locator('.cw-map-token')).toHaveCount(14);
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
    expect(overflow).toBeLessThanOrEqual(1);
  });
}

test('Conquer Westeros Bot turn is visible and locks human controls', async ({ page }) => {
  await page.setViewportSize({ width: 1024, height: 768 });
  await page.goto('/__ui-lab?screen=conquer&state=bot-turn');
  await page.getByRole('button', { name: 'Open current operation tips' }).click();
  await expect(page.getByText('Bot 1 (CPU) is evaluating the public war table.')).toBeVisible();
  await page.getByRole('button', { name: 'Close dialog' }).click();
  await page.getByRole('button', { name: 'Roll Dice' }).click();
  const siegeDialog = page.getByRole('dialog', { name: 'Siege console' });
  await expect(siegeDialog.getByRole('button', { name: 'Roll remaining dice' })).toBeDisabled();
  await expect(siegeDialog.getByRole('button', { name: 'Complete line' })).toBeDisabled();
  await expect(siegeDialog.getByRole('button', { name: 'Lose selected die' })).toBeDisabled();
  const state = await page.evaluate(() => JSON.parse(window.render_game_to_text()));
  expect(state.players.find((player) => player.playerId === 'BOT1').bot).toBe(true);
});

test('Conquer Westeros supports keyboard focus, reduced motion, 200% zoom, and phone portrait', async ({ page }) => {
  await page.emulateMedia({ reducedMotion: 'reduce' });
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto('/__ui-lab?screen=conquer&state=unlocked-target');
  const firstTarget = page.locator('.cw-map-token').first();
  await firstTarget.focus();
  await expect(firstTarget).toBeFocused();
  const transitionDuration = await firstTarget.evaluate((element) => getComputedStyle(element).transitionDuration);
  expect(['0s', '0.001s']).toContain(transitionDuration.split(',')[0]);
  await page.evaluate(() => { document.body.style.zoom = '2'; });
  let overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  expect(overflow).toBeLessThanOrEqual(1);
  await page.evaluate(() => { document.body.style.removeProperty('zoom'); });
  await page.setViewportSize({ width: 390, height: 844 });
  await page.reload();
  overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  expect(overflow).toBeLessThanOrEqual(1);
  const mapViewport = page.locator('.cw-map-viewport');
  const mapOverflow = await mapViewport.evaluate((element) => ({
    x: element.scrollWidth - element.clientWidth,
    y: element.scrollHeight - element.clientHeight,
  }));
  expect(mapOverflow.x).toBeGreaterThan(0);
  expect(mapOverflow.y).toBeGreaterThan(0);
});

test('Conquer Westeros inspects a map token before confirming the siege target', async ({ page }) => {
  await page.setViewportSize({ width: 1024, height: 768 });
  await page.goto('/__ui-lab?screen=conquer&state=unlocked-target');
  const token = page.getByRole('button', { name: /Open Highgarden details/i });
  await token.click();
  const dialog = page.getByRole('dialog', { name: 'Highgarden' });
  await expect(dialog).toBeVisible();
  await expect(dialog).toHaveCSS('opacity', '1');
  await expect(dialog).toContainText('Military ≥ 5');
  const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa']).analyze();
  const severe = results.violations.filter((violation) => ['serious', 'critical'].includes(violation.impact));
  expect(severe).toEqual([]);
  await expect(page.getByRole('button', { name: 'Set as target' })).toBeFocused();
  await page.getByRole('button', { name: 'Set as target' }).click();
  await expect(dialog).toBeHidden();
  await expect(token).toHaveAttribute('aria-pressed', 'true');
  await page.getByRole('button', { name: 'Roll Dice' }).click();
  await expect(page.getByRole('dialog', { name: 'Siege console' }).getByRole('heading', { name: 'Siege: Highgarden' })).toBeVisible();
});

test('Conquer Westeros map docks open player, throne, tips, log, and siege details', async ({ page }) => {
  await page.setViewportSize({ width: 1024, height: 768 });
  await page.goto('/__ui-lab?screen=conquer&state=unlocked-target');

  await expect(page.locator('.cw-map-shell')).toBeVisible();
  await expect(page.locator('.cw-seat-rail, .cw-side-stack, .cw-console')).toHaveCount(0);

  await page.getByRole('button', { name: /Open player details for PixelPilot/i }).click();
  await expect(page.getByRole('dialog', { name: 'PixelPilot' })).toContainText('Total score');
  await page.getByRole('button', { name: 'Close dialog' }).click();

  await page.getByRole('button', { name: 'Open Iron Throne details' }).click();
  await expect(page.getByRole('dialog', { name: 'Iron Throne' })).toContainText('CipherFox');
  await page.getByRole('button', { name: 'Close dialog' }).click();

  await page.getByRole('button', { name: 'Open current operation tips' }).click();
  await expect(page.getByRole('dialog', { name: 'Operation tips' })).toContainText('Not selected');
  await page.getByRole('button', { name: 'Close dialog' }).click();

  await page.getByRole('button', { name: 'Open campaign log' }).click();
  await expect(page.getByRole('dialog', { name: 'Campaign log' })).toContainText('PixelPilot rolled 7 dice');
  await page.getByRole('button', { name: 'Close dialog' }).click();

  await page.getByRole('button', { name: 'Roll Dice' }).click();
  await expect(page.getByRole('dialog', { name: 'Siege console' }).locator('.cw-die')).toHaveCount(7);
});

test('Conquer Westeros siege dialog keeps all actions reachable in phone landscape @visual', async ({ page }, testInfo) => {
  await page.setViewportSize({ width: 667, height: 375 });
  await page.goto('/__ui-lab?screen=conquer&state=unlocked-target');
  await page.getByRole('button', { name: 'Roll Dice' }).click();

  const dialog = page.getByRole('dialog', { name: 'Siege console' });
  await expect(dialog).toBeVisible();
  await expect(dialog.locator('.cw-die')).toHaveCount(7);
  if (testInfo.project.name === 'chromium') {
    await expect(page).toHaveScreenshot('conquer-siege-667x375.png');
  }
  await dialog.getByRole('button', { name: 'Lose selected die' }).scrollIntoViewIfNeeded();
  await expect(dialog.getByRole('button', { name: 'Lose selected die' })).toBeVisible();
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  expect(overflow).toBeLessThanOrEqual(1);
  const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa']).analyze();
  const severe = results.violations.filter((violation) => ['serious', 'critical'].includes(violation.impact));
  expect(severe).toEqual([]);
});

test('Dance of the Dragons maps all fourteen strongholds without fallback', async ({ page }) => {
  await page.setViewportSize({ width: 1024, height: 768 });
  await page.goto('/__ui-lab?screen=conquer&campaign=dance');
  await expect(page.locator('.cw-map-token')).toHaveCount(14);
  await expect(page.getByRole('group', { name: 'Unmapped strongholds' })).toHaveCount(0);
  await expect(page.locator('[data-stronghold-id="T01"]')).toHaveAttribute('aria-label', /Open The Eyrie details/i);
  await expect(page.locator('[data-stronghold-id="T14"]')).toHaveAttribute('aria-label', /Open High Tide details/i);
  await expect(page.locator('[data-stronghold-id="T01"]').locator('..')).toHaveAttribute('data-map-position', '70,48');
});

test('War of the Usurper maps all fourteen strongholds without fallback', async ({ page }) => {
  await page.setViewportSize({ width: 1024, height: 768 });
  await page.goto('/__ui-lab?screen=conquer&campaign=usurper');
  await expect(page.locator('.cw-map-token')).toHaveCount(14);
  await expect(page.getByRole('group', { name: 'Unmapped strongholds' })).toHaveCount(0);
  await expect(page.locator('[data-stronghold-id="T01"]')).toHaveAttribute('aria-label', /Open Stoney Sept details/i);
  await expect(page.locator('[data-stronghold-id="T10"]')).toHaveAttribute('aria-label', /Open King's Landing details/i);
  await expect(page.locator('[data-stronghold-id="T14"]')).toHaveAttribute('aria-label', /Open Storm's End details/i);
  await expect(page.locator('[data-stronghold-id="T01"]').locator('..')).toHaveAttribute('data-map-position', '44,59');
  await expect(page.locator('[data-stronghold-id="T13"]').locator('..')).toHaveAttribute('data-map-position', '56,90');
  const overlaps = await page.locator('.cw-map-token-wrap').evaluateAll((tokens) => {
    const boxes = tokens.map((token) => ({
      id: token.querySelector('[data-stronghold-id]')?.getAttribute('data-stronghold-id'),
      rect: token.getBoundingClientRect(),
    }));
    return boxes.flatMap((left, leftIndex) => boxes.slice(leftIndex + 1).flatMap((right) => {
      const intersects = left.rect.left < right.rect.right
        && left.rect.right > right.rect.left
        && left.rect.top < right.rect.bottom
        && left.rect.bottom > right.rect.top;
      return intersects ? [`${left.id}-${right.id}`] : [];
    }));
  });
  expect(overlaps).toEqual([]);
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  expect(overflow).toBeLessThanOrEqual(1);
  const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa']).analyze();
  const severe = results.violations.filter((violation) => ['serious', 'critical'].includes(violation.impact));
  expect(severe).toEqual([]);
});

test("Aegon's Conquest maps all fourteen strongholds without fallback at game breakpoints", async ({ page }) => {
  for (const viewport of [
    { width: 1440, height: 900 },
    { width: 1024, height: 768 },
    { width: 667, height: 375 },
  ]) {
    await page.setViewportSize(viewport);
    await page.goto('/__ui-lab?screen=conquer&campaign=conquest');
    await expect(page.locator('.cw-map-token')).toHaveCount(14);
    await expect(page.getByRole('group', { name: 'Unmapped strongholds' })).toHaveCount(0);
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
    expect(overflow).toBeLessThanOrEqual(1);
  }

  await page.setViewportSize({ width: 1024, height: 768 });
  await page.goto('/__ui-lab?screen=conquer&campaign=conquest');
  await expect(page.locator('[data-stronghold-id="T04"]')).toHaveAttribute('aria-label', /Open Gulltown details/i);
  await expect(page.locator('[data-stronghold-id="T05"]')).toHaveAttribute('aria-label', /Open Field of Fire details/i);
  await expect(page.locator('[data-stronghold-id="T10"]')).toHaveAttribute('aria-label', /Open Aegonfort details.*Iron Throne stronghold/i);
  await expect(page.locator('[data-stronghold-id="T14"]')).toHaveAttribute('aria-label', /Open Storm's End details/i);
  await expect(page.locator('[data-stronghold-id="T01"]').locator('..')).toHaveAttribute('data-map-position', '67,56');
  await expect(page.locator('[data-stronghold-id="T04"]').locator('..')).toHaveAttribute('data-map-position', '82,48');
  await expect(page.locator('[data-stronghold-id="T05"]').locator('..')).toHaveAttribute('data-map-position', '39,64');
  await expect(page.locator('[data-stronghold-id="T08"]').locator('..')).toHaveAttribute('data-map-position', '49,70');
  await expect(page.locator('[data-stronghold-id="T10"]').locator('..')).toHaveAttribute('data-map-position', '57,63');
  const overlaps = await page.locator('.cw-map-token-wrap').evaluateAll((tokens) => {
    const boxes = tokens.map((token) => ({
      id: token.querySelector('[data-stronghold-id]')?.getAttribute('data-stronghold-id'),
      rect: token.getBoundingClientRect(),
    }));
    return boxes.flatMap((left, leftIndex) => boxes.slice(leftIndex + 1).flatMap((right) => {
      const intersects = left.rect.left < right.rect.right
        && left.rect.right > right.rect.left
        && left.rect.top < right.rect.bottom
        && left.rect.bottom > right.rect.top;
      return intersects ? [`${left.id}-${right.id}`] : [];
    }));
  });
  expect(overlaps).toEqual([]);
  const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa']).analyze();
  const severe = results.violations.filter((violation) => ['serious', 'critical'].includes(violation.impact));
  expect(severe).toEqual([]);
});

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

test('Las Vegas keeps eight 3D dice and their actions in a persistent centered modal', async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto('/__ui-lab?screen=vegas&state=roll');
  await page.getByRole('button', { name: 'Roll 8 dice' }).click();

  const dialog = page.getByRole('dialog');
  await expect(dialog).toBeVisible();
  await expect(page.locator('.vegas-roll-die-3d')).toHaveCount(8);
  await expect(page.locator('.vegas-roll-cube__face')).toHaveCount(48);
  await expect(page.locator('.vegas-current-roll')).toHaveCount(0);
  await expect(page.locator('.vegas-console .vegas-die')).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Hide dialog' })).toHaveCount(0);
  await page.keyboard.press('Escape');
  await expect(dialog).toBeVisible();

  await expect.poll(async () => page.locator('.vegas-roll-cube').first().evaluate(
    (element) => getComputedStyle(element).transform,
  )).toMatch(/^matrix3d\(/);

  await page.waitForTimeout(200);
  const dialogBox = await dialog.boundingBox();
  expect(dialogBox).not.toBeNull();
  expect(Math.abs(dialogBox.x + dialogBox.width / 2 - 720)).toBeLessThanOrEqual(2);
  expect(Math.abs(dialogBox.y + dialogBox.height / 2 - 450)).toBeLessThanOrEqual(2);

  const results = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa']).analyze();
  const severe = results.violations.filter((violation) => ['serious', 'critical'].includes(violation.impact));
  expect(severe).toEqual([]);

  await expect(page.getByRole('dialog', { name: 'Roll complete // choose a casino' })).toBeVisible({ timeout: 15000 });
  await expect(page.locator('.vegas-roll-dialog [data-result-face]')).toHaveCount(8);
  await expect.poll(async () => page.locator('.vegas-roll-dialog [data-result-face]').evaluateAll(
    (elements) => elements.map((element) => Number(element.dataset.resultFace)),
  )).toEqual([6, 2, 5, 1, 4, 3, 6, 5]);
  const dieGap = await page.locator('.vegas-roll-dice').evaluate(
    (element) => Number.parseFloat(getComputedStyle(element).columnGap),
  );
  expect(dieGap).toBeGreaterThanOrEqual(12);
  const bigDieBorder = await page.locator('.vegas-roll-die-3d--big .vegas-roll-cube__face').first().evaluate(
    (element) => {
      const styles = getComputedStyle(element);
      return {
        style: styles.borderTopStyle,
        width: styles.borderTopWidth,
        shadow: styles.boxShadow,
      };
    },
  );
  expect(bigDieBorder).toEqual(expect.objectContaining({ style: 'double', width: '6px' }));
  expect(bigDieBorder.shadow).not.toBe('none');
  await expect(page.getByRole('button', { name: 'Place all 6s' })).toBeEnabled();
  await expect(page.getByRole('button', { name: 'Place all 1s' })).toBeFocused();
  await expect(page.locator('.vegas-console').getByRole('button', { name: /Place all/i })).toHaveCount(0);

  await page.getByRole('button', { name: 'Hide dialog' }).click();
  await expect(dialog).toHaveCount(0);
  const showRoll = page.getByRole('button', { name: 'Show roll & actions' });
  await expect(showRoll).toBeFocused();
  await showRoll.click();
  await expect(page.getByRole('dialog', { name: 'Roll complete // choose a casino' })).toBeVisible();
  await expect(page.locator('.vegas-roll-dialog [data-result-face]')).toHaveCount(8);

  await page.keyboard.press('Escape');
  await expect(dialog).toHaveCount(0);
  await page.getByRole('button', { name: 'Show roll & actions' }).click();
  await page.getByRole('button', { name: 'Place all 6s' }).click();
  await expect(dialog).toHaveCount(0);
  await expect(page.getByRole('heading', { name: 'Table locked' })).toBeFocused();
  const textState = JSON.parse(await page.evaluate(() => window.render_game_to_text()));
  expect(textState.rollDialogVisible).toBe(false);
  expect(textState.currentPlayerId).toBe('P2');
});

test('Las Vegas roll reveal fits phone landscape and respects reduced motion', async ({ page }) => {
  await page.emulateMedia({ reducedMotion: 'reduce' });
  await page.setViewportSize({ width: 667, height: 375 });
  await page.goto('/__ui-lab?screen=vegas&state=roll');
  await page.getByRole('button', { name: 'Roll 8 dice' }).click();

  const dialog = page.getByRole('dialog');
  await expect(dialog).toBeVisible();
  const dialogBox = await dialog.boundingBox();
  expect(dialogBox).not.toBeNull();
  expect(dialogBox.x).toBeGreaterThanOrEqual(0);
  expect(dialogBox.y).toBeGreaterThanOrEqual(0);
  expect(dialogBox.x + dialogBox.width).toBeLessThanOrEqual(667);
  expect(dialogBox.y + dialogBox.height).toBeLessThanOrEqual(375);
  await expect(page.locator('.vegas-roll-die-3d')).toHaveCount(8);
  await expect(page.locator('.vegas-roll-cube__face')).toHaveCount(48);
  const animationDuration = await page.locator('.vegas-roll-die-wrap').first().evaluate((element) => getComputedStyle(element).animationDuration);
  expect(['0s', '0.001s']).toContain(animationDuration.split(',')[0]);

  await expect(page.getByRole('dialog', { name: 'Roll complete // choose a casino' })).toBeVisible({ timeout: 1500 });
  await expect(page.getByRole('button', { name: 'Spend 1 chip to skip' })).toBeVisible();
  await page.getByRole('button', { name: 'Hide dialog' }).click();
  await expect(page.getByRole('button', { name: 'Show roll & actions' })).toBeFocused();

  await page.setViewportSize({ width: 390, height: 844 });
  await page.getByRole('button', { name: 'Show roll & actions' }).click();
  const portraitDialogBox = await page.getByRole('dialog').boundingBox();
  expect(portraitDialogBox).not.toBeNull();
  expect(portraitDialogBox.x).toBeGreaterThanOrEqual(0);
  expect(portraitDialogBox.x + portraitDialogBox.width).toBeLessThanOrEqual(390);
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
  expect(overflow).toBeLessThanOrEqual(1);
});

test('Las Vegas visibly identifies a bot turn and locks human actions', async ({ page }) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await page.goto('/__ui-lab?screen=vegas&state=bot');
  await expect(page.getByText(/Bot 1 \(CPU\) is taking their turn/i)).toBeVisible();
  await expect(page.getByText('CPU', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: 'Place all 1s' })).toHaveCount(0);
  await expect(page.getByRole('button', { name: 'Show roll & actions' })).toHaveCount(0);
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
