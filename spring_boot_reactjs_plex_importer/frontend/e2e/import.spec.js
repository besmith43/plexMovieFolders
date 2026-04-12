import { test, expect } from '@playwright/test';
import fs from 'node:fs';
import path from 'node:path';
import { execFileSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const currentDir = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(currentDir, '..', '..');
const sourceRoot = path.join(repoRoot, 'test_root_dir');
const destRoot = path.join(repoRoot, 'test_dest_dir');

function sourcePath(...segments) {
  return path.join(sourceRoot, ...segments);
}

function destPath(...segments) {
  return path.join(destRoot, ...segments);
}

function resetFixture() {
  execFileSync('bash', ['-lc', './reset.sh'], {
    cwd: repoRoot,
    stdio: 'inherit',
  });
}

async function selectDirectory(page, name) {
  await page.getByRole('button', { name: new RegExp(`^${escapeRegex(name)}\\s`) }).click();
}

function escapeRegex(value) {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

test.beforeEach(() => {
  resetFixture();
});

test('imports a movie and removes the source directory when no video files remain', async ({ page }) => {
  await page.goto('/');
  await selectDirectory(page, 'dir1');
  await page.getByLabel('Video file').selectOption('file1.mkv');
  await page.getByRole('textbox', { name: 'Title' }).fill('Inception');
  await page.getByRole('spinbutton', { name: 'Year' }).fill('2010');

  await page.getByRole('button', { name: 'Preview destination' }).click();
  await expect(page.getByText(/Inception \(2010\)\/Inception \(2010\)\.mkv$/)).toBeVisible();

  await page.getByRole('button', { name: 'Execute import' }).click();
  await expect(page.locator('.banner.success')).toContainText('Import completed successfully.');
  await expect(page.getByRole('status')).toContainText('Import finished');
  await expect(page.getByRole('button', { name: /dir1/ })).toHaveCount(0);

  expect(fs.existsSync(sourcePath('dir1'))).toBe(false);
  expect(fs.existsSync(destPath('Movies', 'Inception (2010)', 'Inception (2010).mkv'))).toBe(true);
});

test('imports a tv episode into an existing series', async ({ page }) => {
  await page.goto('/');
  await selectDirectory(page, 'dir3');
  await page.getByLabel('Video file').selectOption('file3.mp4');
  await page.getByRole('button', { name: 'TV Show' }).click();
  await page.getByLabel('Series', { exact: true }).selectOption('Eureka');
  await page.getByRole('spinbutton', { name: 'Season' }).fill('2');
  await page.getByRole('spinbutton', { name: 'Episode' }).fill('3');

  await page.getByRole('button', { name: 'Preview destination' }).click();
  await expect(page.getByText(/TV Shows\/Eureka\/Season 02\/Eureka - s02e03\.mp4$/)).toBeVisible();

  await page.getByRole('button', { name: 'Execute import' }).click();
  await expect(page.locator('.banner.success')).toContainText('Import completed successfully.');
  await expect(page.getByRole('button', { name: /dir3/ })).toHaveCount(0);

  expect(fs.existsSync(sourcePath('dir3'))).toBe(false);
  expect(fs.existsSync(destPath('TV Shows', 'Eureka', 'Season 02', 'Eureka - s02e03.mp4'))).toBe(true);
});

test('defaults to skip when the destination already exists', async ({ page }) => {
  const existingDir = destPath('Movies', 'Collision Test (2024)');
  fs.mkdirSync(existingDir, { recursive: true });
  fs.writeFileSync(path.join(existingDir, 'Collision Test (2024).mkv'), 'existing');

  await page.goto('/');
  await selectDirectory(page, 'dir4');
  await page.getByLabel('Video file').selectOption('file4.mkv');
  await page.getByRole('textbox', { name: 'Title' }).fill('Collision Test');
  await page.getByRole('spinbutton', { name: 'Year' }).fill('2024');

  await page.getByRole('button', { name: 'Preview destination' }).click();
  await expect(page.getByText('Destination already exists. Default action is skip.')).toBeVisible();
  await expect(page.getByLabel('If destination exists')).toHaveValue('SKIP');

  await page.getByRole('button', { name: 'Execute import' }).click();
  await expect(page.locator('.banner.success')).toContainText('Destination already exists. File was skipped.');
  await expect(page.getByRole('status')).toContainText('Import skipped');

  expect(fs.existsSync(sourcePath('dir4', 'file4.mkv'))).toBe(true);
  expect(fs.readFileSync(destPath('Movies', 'Collision Test (2024)', 'Collision Test (2024).mkv'), 'utf8')).toBe('existing');
});

test('serves the favicon without browser console errors', async ({ page }) => {
  const consoleErrors = [];
  page.on('console', (message) => {
    if (message.type() === 'error') {
      consoleErrors.push(message.text());
    }
  });

  const response = await page.goto('/');
  expect(response?.ok()).toBe(true);
  const faviconResponse = await page.request.get('/favicon.ico');
  expect(faviconResponse.ok()).toBe(true);
  expect(consoleErrors).toEqual([]);
});
