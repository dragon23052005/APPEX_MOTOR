-- Explicit operator-only setup. NOT a Flyway migration and never run at startup.
-- First register your own account through the application.
-- Back up the database and verify the intended account before editing the email below.
-- Connect privately using a database account authorized to update users.
-- Only replace the empty value with the exact email of that verified account.
-- Leaving it empty makes this script a no-op. No password or new account is created.
USE apex_motors;
SET @apex_admin_email = '';

START TRANSACTION;
UPDATE users
SET role_id = (SELECT id FROM roles WHERE name = 'ADMIN')
WHERE email = LOWER(TRIM(@apex_admin_email))
  AND @apex_admin_email <> ''
  AND enabled = 1;
SELECT ROW_COUNT() AS promoted_accounts;
COMMIT;

SELECT u.email, r.name AS role
FROM users u JOIN roles r ON r.id = u.role_id
WHERE u.email = LOWER(TRIM(@apex_admin_email)) AND @apex_admin_email <> '';
-- Sign out and sign in again to load the ADMIN authority.
-- Admin UI remains reserved for Phase 3.3.
