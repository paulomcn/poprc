import assert from 'node:assert/strict';
import { inspectResponsiveLayout, responsiveFailures } from './responsiveChecks.mjs';

// Receives an authenticated browser tab on the indicated page, in an isolated test environment.
export async function verifyProjectModal(tab) {
  const page = tab.playwright;
  await page.getByRole('button', { name: 'Novo projeto', exact: true }).click();
  assert.deepEqual(responsiveFailures(await page.evaluate(inspectResponsiveLayout)), []);
  await page.getByRole('dialog').press('Tab');
  assert.equal(await page.evaluate(() => Boolean(document.activeElement.closest('[role=dialog]'))), true);
  await page.getByRole('button', { name: 'Fechar modal' }).press('Escape');
  assert.equal(await page.getByRole('dialog').count(), 0);
}

export async function verifyOsMaterialLayout(tab) {
  const page = tab.playwright;
  await page.getByRole('button', { name: 'Nova OS', exact: true }).click();
  assert.deepEqual(responsiveFailures(await page.evaluate(inspectResponsiveLayout)), []);
  const width = await page.evaluate(() => document.querySelector('[aria-label="Material 1"]').getBoundingClientRect().width);
  assert.ok(width >= 180, `Material selector too narrow: ${width}px`);
  await page.getByRole('button', { name: 'Fechar modal' }).click();
}

// Writes the current checklist. Never run against an operational OS.
export async function verifyChecklistContext(tab) {
  const page = tab.playwright;
  const title = await page.getByRole('heading').first().innerText();
  await page.getByRole('button', { name: 'Salvar checklist', exact: true }).click();
  await page.getByText('Atividades realizadas registradas na OS.', { exact: true }).innerText();
  assert.equal(await page.getByRole('heading').first().innerText(), title);
}
