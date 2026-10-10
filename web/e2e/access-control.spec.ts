import { expect, test } from '@playwright/test';

/**
 * EN: Anonymous visitors must not access any operational or platform screens.
 * ES: Los visitantes anónimos no deben acceder a pantallas privadas.
 *
 * These are browser routing checks, NOT backend authorization tests.
 */
const privateRoutes = [
    '/app',
    '/app/mine',
    '/app/reports',
    '/platform',
    '/select-tenant',
];

for (const route of privateRoutes) {
    test(`anonymous visitor is redirected from ${route}`, async ({ page }) => {
        await page.goto(route);
        await expect(page).toHaveURL(/\/login(?:[?#].*)?$/);
        await expect(page.locator('input[type="password"]')).toBeVisible();
    });
}

test('login form does not expose a prefilled password', async ({ page }) => {
    await page.goto('/login');
    await expect(page.locator('input[type="password"]')).toHaveValue('');
});
