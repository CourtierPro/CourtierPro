import { test, expect } from '@playwright/test';

test('broker creates a new transaction', async ({ page }) => {
  // --- Login (Auth0 Hosted Page)
  await page.goto('https://dev-y7mhv7ttykx4kz4f.us.auth0.com/login');

  await page.getByRole('textbox', { name: 'Email' }).fill('broker.test@example.com');
  await page.getByRole('textbox', { name: 'Password' }).fill('!Broker1234!');
  await page.getByRole('button', { name: 'Log In' }).click();

  // --- Wait for redirect back into the app
  await page.waitForURL('http://localhost:8081/dashboard/broker**', { timeout: 10000 });

  // --- Navigate to Transactions
  await page.getByRole('button', { name: 'Transactions' }).click();

  // --- Create New Transaction
  await page.getByRole('button', { name: 'New Transaction' }).click();

  // Buyer-side option ("Client is purchasing a ...")
  await page.getByText('Client is purchasing a').click();

  // Select Client
  await page.getByRole('textbox', { name: 'Client required' }).click();
  await page.getByRole('option', { name: /Michael Chen/i }).click();

  // Property address
  await page.getByRole('textbox', { name: /Property Address required/i })
    .fill('33 riverside saint-lambert');

  // Initial stage dropdown
  await page.getByRole('combobox', { name: /Initial Stage required/i }).click();
  await page.getByRole('option', { name: /Prequalify Financially/i }).click();

  // Create transaction
  await page.getByRole('button', { name: 'Create Transaction' }).click();

  // Back to Transactions page
  await page.getByRole('button', { name: 'Transactions' }).click();

  // Final assertion (ensures test actually passed)
  await expect(page.getByText(/33 riverside saint-lambert/i)).toBeVisible();
});
