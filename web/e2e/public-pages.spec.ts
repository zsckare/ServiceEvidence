import { expect, test } from '@playwright/test';

/** EN: Public landing renders and offers a way to log in.
 * ES: La página pública se muestra y permite ir al inicio de sesión.
 */
test('landing page renders and links to login', async ({ page }) => {
    await page.goto('/');
    await expect(page.locator('body')).toContainText('ServiceEvidence');
    await expect(page.locator('a[href="/login"]').first()).toBeVisible();
});

/** EN: Login is accessible directly, without an API response.
 * ES: El login es accesible sin depender de respuestas del backend.
 */
test('login shows email and password inputs', async ({ page }) => {
    await page.goto('/login');
    await expect(page.getByRole('heading', { name: 'Bienvenido de nuevo' })).toBeVisible();
    await expect(page.locator('input[type="email"]')).toBeVisible();
    await expect(page.locator('input[type="password"]')).toBeVisible();
});

test('anonymous users cannot open tenant dashboard', async ({ page }) => {
    await page.goto('/app');
    await expect(page).toHaveURL(/\/login(?:\?.*)?$/);
});
