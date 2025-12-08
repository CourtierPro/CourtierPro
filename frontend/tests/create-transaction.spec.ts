import { test } from '@playwright/test';

test('broker creates a new transaction', async ({ page }) => {
  // Visit dashboard directly (Auth0 bypassed)
  await page.goto('http://localhost:8081/dashboard/broker');

  // Navigate to Transactions
  await page.getByRole('button', { name: 'Transactions' }).click();

  // Create New Transaction
  await page.getByRole('button', { name: 'New Transaction' }).click();

  // Select buyer-side flow
  await page.getByText('Client is purchasing a').click();

  // Select Client
  await page.getByRole('textbox', { name: 'Client required' }).click();
  await page.getByRole('option', { name: /Michael Chen/i }).click();

  // Property address
  await page.getByRole('textbox', { name: /Property Address required/i })
    .fill('33 riverside saint-lambert');

  // Initial Stage
  await page.getByRole('combobox', { name: /Initial Stage required/i }).click();
  await page.getByRole('option', { name: /Prequalify Financially/i }).click();

  // Create transaction
  await page.getByRole('button', { name: 'Create Transaction' }).click();

// Back to Transactions page
await page.getByRole('button', { name: /^Transactions$/ }).click();

// Wait for the row to appear
await page.waitForSelector('text=33 riverside saint-lambert', { timeout: 15000 });


});
