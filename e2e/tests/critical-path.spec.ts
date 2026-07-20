import { test, expect, Page } from '@playwright/test';
import { Client } from 'pg';

// Critical path per Test Strategy §5 release gate: registration -> tutor verification ->
// gig creation -> booking -> escrow hold -> mutual completion -> review, run once per
// required locale (FR/AR) to also exercise the RTL smoke-check via the real rendered UI.
//
// Matching (pgvector + the real multilingual embedding model) is intentionally NOT asserted
// here: the model is excluded from CI as "real-model" (see Sprint 3 notes) because a cold
// HuggingFace download makes it slow/non-deterministic in a fresh environment. This suite
// still creates the gig (exercising embedding generation), but books the known verified
// tutor directly via the same URL the "book" button on a real match card would produce,
// rather than waiting for that tutor to actually appear in the match list.

const PASSWORD = 'supersecret1';

const DB_CONFIG = {
  host: process.env.E2E_DB_HOST ?? 'localhost',
  port: Number(process.env.E2E_DB_PORT ?? process.env.DB_HOST_PORT ?? 5432),
  database: process.env.DB_NAME ?? 'dars_ma',
  user: process.env.DB_USER ?? 'dars_ma_app',
  password: process.env.DB_PASSWORD ?? 'changeme'
};

// No admin-registration API exists by design (register_asAdmin_isForbidden, backend-side) - the
// project's own manual verification sessions promote a user to ADMIN via a direct DB update
// (see .logs/sessions.md session 8). This mirrors that exact precedent for automated E2E.
async function promoteToAdmin(email: string): Promise<void> {
  const client = new Client(DB_CONFIG);
  await client.connect();
  try {
    await client.query('UPDATE users SET role = $1 WHERE email = $2', ['ADMIN', email]);
  } finally {
    await client.end();
  }
}

function decodeJwtSub(accessToken: string): string {
  const payload = accessToken.split('.')[1];
  const json = Buffer.from(payload.replace(/-/g, '+').replace(/_/g, '/'), 'base64').toString('utf-8');
  return JSON.parse(json).sub as string;
}

async function setLang(page: Page, lang: 'fr' | 'ar'): Promise<void> {
  await page.goto('/');
  await page.evaluate((l) => localStorage.setItem('dars_ma_lang', l), lang);
}

async function register(page: Page, email: string, fullName: string, role: 'STUDENT' | 'TUTOR'): Promise<void> {
  await page.goto('/register');
  await page.locator('[formcontrolname="fullName"]').fill(fullName);
  await page.locator('[formcontrolname="email"]').fill(email);
  await page.locator('[formcontrolname="password"]').fill(PASSWORD);
  if (role === 'TUTOR') {
    await page.locator('mat-radio-button[value="TUTOR"]').click();
  }
  await page.locator('form button[type="submit"]').click();
  await page.waitForURL('**/login');
}

// Returns the logged-in user's id (decoded from the access token), since the app never
// persists it anywhere else in the DOM - the JWT payload is the source of truth (matches
// AuthService.userId(), decoded the same way client-side).
async function login(page: Page, email: string): Promise<string> {
  await page.goto('/login');
  await page.locator('[formcontrolname="email"]').fill(email);
  await page.locator('[formcontrolname="password"]').fill(PASSWORD);
  const [response] = await Promise.all([
    page.waitForResponse((r) => r.url().includes('/api/v1/auth/login') && r.request().method() === 'POST'),
    page.locator('form button[type="submit"]').click()
  ]);
  await page.waitForURL((url) => !url.pathname.includes('/login'));
  const body = await response.json();
  return decodeJwtSub(body.accessToken);
}

function runCriticalPath(lang: 'fr' | 'ar') {
  test(`critical path (${lang}): register, verify, gig, book, escrow, complete, review`, async ({ page }) => {
    const stamp = Date.now();
    const tutorEmail = `e2e-tutor-${lang}-${stamp}@example.com`;
    const studentEmail = `e2e-student-${lang}-${stamp}@example.com`;
    const adminEmail = `e2e-admin-${lang}-${stamp}@example.com`;

    await setLang(page, lang);
    if (lang === 'ar') {
      await expect(page.locator('html')).toHaveAttribute('dir', 'rtl');
    }

    // 1. Registration (tutor, student, and a throwaway account promoted to ADMIN below)
    await register(page, tutorEmail, 'E2E Tutor', 'TUTOR');
    await register(page, studentEmail, 'E2E Student', 'STUDENT');
    await register(page, adminEmail, 'E2E Admin', 'STUDENT');
    await promoteToAdmin(adminEmail);

    // 2. Tutor: complete profile, upload a verification document
    const tutorUserId = await login(page, tutorEmail);
    await page.goto('/profile/tutor/me');
    await page.locator('[formcontrolname="bio"]').fill('Experienced math and physics tutor.');
    await page.locator('[formcontrolname="subjects"]').fill('Math, Physics');
    await page.locator('[formcontrolname="hourlyRateMad"]').fill('120');
    await page.locator('form button[type="submit"]').click();
    await expect(page.locator('.profile-card').first().locator('.message')).toBeVisible();

    await page.setInputFiles('input[type="file"]', {
      name: 'diploma.pdf',
      mimeType: 'application/pdf',
      buffer: Buffer.from('%PDF-1.4 e2e diploma bytes')
    });
    await page.locator('.profile-card').nth(1).locator('button').click();
    await expect(page.locator('.profile-card').nth(1).locator('.message')).toBeVisible();

    // 3. Admin: approve the pending verification document
    await login(page, adminEmail);
    await page.goto('/admin/verification');
    const queueItem = page.locator('.queue-item').filter({ hasText: 'E2E Tutor' });
    await expect(queueItem).toBeVisible();
    await queueItem.locator('button').first().click(); // "approve" is the first action button
    await expect(page.locator('.message')).toBeVisible();

    // 4. Student: post a gig request
    await login(page, studentEmail);
    await page.goto('/gigs/new');
    await page.locator('[formcontrolname="subject"]').fill('Math');
    await page.locator('[formcontrolname="level"]').fill('High School');
    await page.locator('[formcontrolname="description"]').fill('Need help preparing for the baccalaureate exam.');
    const [createGigResponse] = await Promise.all([
      page.waitForResponse((r) => r.url().endsWith('/api/v1/gigs') && r.request().method() === 'POST'),
      page.locator('form button[type="submit"]').click()
    ]);
    const gig = await createGigResponse.json();
    await expect(page.locator('.message.success')).toBeVisible();

    await page.goto(`/gigs/${gig.id}`);
    await expect(page.locator('.gig-summary')).toBeVisible();

    // 5. Book the verified tutor directly (see file header re: matching nondeterminism)
    await page.goto(`/gigs/${gig.id}/book/${tutorUserId}`);
    await page.locator('[formcontrolname="durationHours"]').fill('2');
    const [bookingResponse] = await Promise.all([
      page.waitForResponse((r) => r.url().endsWith('/api/v1/bookings') && r.request().method() === 'POST'),
      page.locator('form button[type="submit"]').click()
    ]);
    const booking = await bookingResponse.json();
    await page.waitForURL(`**/bookings/${booking.id}`);
    await expect(page.locator('.status')).toHaveClass(/status-escrow_held/);

    // 6. Mutual completion: student confirms first, then the tutor
    await Promise.all([
      page.waitForResponse((r) => r.url().includes(`/bookings/${booking.id}/complete`)),
      page.locator('.actions button').first().click()
    ]);

    await login(page, tutorEmail);
    await page.goto(`/bookings/${booking.id}`);
    await Promise.all([
      page.waitForResponse((r) => r.url().includes(`/bookings/${booking.id}/complete`)),
      page.locator('.actions button').first().click()
    ]);
    await expect(page.locator('.status')).toHaveClass(/status-completed/);

    // 7. Review: student rates the completed session (default 5-star rating, just add a comment)
    await login(page, studentEmail);
    await page.goto(`/bookings/${booking.id}`);
    await expect(page.locator('.review-form')).toBeVisible();
    await page.locator('.review-form [formcontrolname="comment"]').fill('Great session, highly recommend!');
    await Promise.all([
      page.waitForResponse((r) => r.url().includes(`/bookings/${booking.id}/reviews`) && r.request().method() === 'POST'),
      page.locator('.review-form button[type="submit"]').click()
    ]);
    await expect(page.locator('.review-item')).toBeVisible();
  });
}

test.describe('critical path', () => {
  runCriticalPath('fr');
  runCriticalPath('ar');
});
