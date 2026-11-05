# APEX MOTORS — Phase 3.2 Authentication & Authorization

Implementation and verification date: 5 November 2026 (Asia/Saigon).

Status: authentication implementation and final H2/MySQL/HTTP verification are complete. The final build passed all 134 Java tests with no failures, errors or skips. Browser visual verification remains incomplete. After reviewing the implementation and test results, the user authorized committing and publishing the `phase-3-authentication` branch. Merging into `main` remains separate.

## Repository audit and preservation

- Repository: `C:\Users\long5\Downloads\APPEX_MOTOR_PHASE2`, remote `https://github.com/dragon23052005/APPEX_MOTOR.git`.
- Started from `phase-3-database-foundation` at `8180e6d1738045177f0a11094d526cec868a4752`. Fetched remote references; `origin/main` was `359c159`, containing the Phase 3.1 merge.
- Created the requested `phase-3-authentication` branch from `8180e6d` without resetting, stashing, merging or overwriting local work.
- Preserved the unrelated two-line change in `tools/prepare_ferrari_gallery.py` and the untracked `tmp/` directory.
- No database, table or existing Docker volume was reset or deleted. Automated tests use isolated H2 schemas or fresh Testcontainers instances with random credentials and localhost port bindings. The user's `mysql8036` container on port 3306 was not used for test data. In the subsequent local-startup follow-up, the previously absent `apex_motors` database was created and initialized by the unchanged managed Flyway migrations, as described below.
- All original Ferrari images, gallery manifest, image URLs, showroom repository behavior, gallery CSS/JS and controllers were preserved. Shared navigation gains an account link only with the `mysql` profile.
- Existing private `.env` and `application-local.properties` are kept out of Git. The previously published Phase 3.1 credential exposure is historical; this phase neither republishes a credential nor rewrites Git history. Password rotation for the previously exposed credential remains the operator's responsibility.

## Implemented features

- Spring Boot-managed Spring Security 6.5.6, form authentication and session-based login backed by the existing users/roles repositories.
- Public registration with full name, email, password, confirmation, server-side validation and duplicate-email handling. Email is normalized, and database uniqueness handles concurrent duplicate submissions.
- Registration always grants CUSTOMER. Client-provided roles, IDs, enabled flags and hashes are ignored by explicit form field allowlists.
- BCrypt cost 12. Passwords require 12–64 characters with uppercase, lowercase and a number, and no more than 72 UTF-8 bytes. Password values are never rendered back into forms or exposed in profile DTOs/JSON.
- Login success/failure pages, fixed generic authentication errors, session fixation protection and safe return to a previously requested private GET path. Absolute saved URL hosts and user-supplied redirect parameters never become redirect destinations; saved query strings are deliberately discarded.
- POST-only logout with CSRF, session invalidation and cookie clearing.
- CUSTOMER/ADMIN profile view, full-name/phone updates and current-password verification for password changes. Account identity always comes from the authenticated principal, never a submitted account ID/email.
- Transactional account writes, row locks for profile/password changes, and expiry of all registered sessions after a password change commits. The current session is invalidated immediately; other sessions are rejected on their next request.
- ADMIN-only reserved routes and branded access-denied/CSRF error pages. No Phase 3.3 administrator UI or business operations were added.
- Login, registration and profile templates reuse the existing navigation, typography, studio photographs and scripts, with a separate dark/metallic account stylesheet. Forms include labels, autocomplete hints, error summaries, focus indicators, keyboard-operable password visibility buttons, responsive layouts and reduced-motion styles. Fields and text are escaped through Thymeleaf.
- A deliberate operator-only ADMIN promotion script with no account creation or default password. It is never invoked by Flyway or startup.

## Architecture and routes

`com.apexmotors.auth` contains:

- `SecurityConfig`: profile-scoped filter chain, BCrypt encoder, session registry/listener and reserved-route authorization.
- `DatabaseUserDetailsService`: reads the current enabled flag, hash and role in a read transaction; no generated in-memory login account is enabled.
- `AccountService`: transactional registration/profile/password logic and an after-commit password-change event.
- `AccountSessionInvalidator`: expires all sessions registered for a changed account after commit.
- `LocalLoginSuccessHandler`: validates saved paths and sends only local redirects.
- `RegistrationForm`, `ProfileForm`, `PasswordChangeForm`, `StrongPassword` and its validator: explicit input contracts, separate from persistence entities.
- `AuthenticationController`, `ProfileController`, `AccountNavigationAdvice` and `AccountsUnavailableController`: page rendering and profile compatibility.

| Route | Behavior |
|---|---|
| `GET /login` | Custom login page; authenticated users go to their profile |
| `POST /login` | Spring Security authentication, `email` and `password` parameters |
| `GET /register`, `POST /register` | Customer-only registration with validation; success redirects to login |
| `POST /logout` | CSRF required; signs out the current session |
| `GET /profile` | CUSTOMER or ADMIN; own profile only |
| `POST /profile` | Own full name and optional phone only |
| `POST /profile/password` | Current password required; changes the hash and invalidates sessions |
| `/admin`, `/admin/**` | ADMIN only; currently no controllers, so an authorized request returns 404 |
| `/appointments`, `/appointments/**`, `/favorites`, `/favorites/**` | Reserved for CUSTOMER/ADMIN; features not implemented |
| `/access-denied` | Safe 403 page, including CSRF failure explanations |
| Existing showroom, gallery and static resources | Public |

The public contact form remains a demo. Under `mysql`, its existing Thymeleaf POST form automatically includes CSRF. Default/dev modes preserve the prior database-free behavior.

## Database and profile compatibility

No new database tables, columns or Flyway versions were needed. The managed V1 and V2 contents are unchanged. `UserEntity` adds only an explicitly JSON-ignored hash accessor used internally for authentication. `UserJpaRepository` adds existence and locked-account lookups. Hibernate still uses `ddl-auto=validate`; Flyway remains the schema authority, with clean disabled and automatic baseline disabled.

The `mysql` profile activates all account/database/security beans. Without it, database and security auto-configuration are excluded, no generated development security account is created, and the showroom remains accessible without MySQL. Account/reserved routes return a friendly 503 page, and account navigation is hidden.

Cookies use HttpOnly, SameSite=Lax, cookie-only tracking and a 30-minute idle timeout. `SESSION_COOKIE_SECURE=true` must be set for HTTPS deployment; the default false supports loopback HTTP development. No session token is stored in browser local storage or URL parameters. Relative redirects are enforced by both the login handler and `server.tomcat.use-relative-redirects=true`, so Tomcat does not turn a local destination into an absolute URL based on an untrusted Host header.

## Verification

| Check | Result |
|---|---|
| Phase 3.1 baseline before edits | PASS: 40 Java tests and executable JAR packaging |
| Initial Phase 3.2 unit/H2/showroom verification | PASS: 85 Java tests before the additional real HTTP scenario |
| Initial real MySQL account contract | PASS: all 36 account scenarios on MySQL 8.0.36 before the additional real HTTP scenario |
| Final combined H2/MySQL/HTTP verification | PASS: `verify -Pmysql-it -Dapex.mysql.test-image=mysql:8.0.36`; 86 regular tests plus 48 MySQL integration tests (134 total), no failures/errors/skips, executable JAR packaged; BUILD SUCCESS |
| Ferrari gallery JavaScript | PASS: 11 tests; no changes to the gallery implementation |
| Live visual/browser check | INCOMPLETE: in-app browser initialization failed with a Statsig timeout; Windows browser-control approval timed out. No successful desktop/mobile screenshot or keyboard visual audit is claimed |
| Production MySQL 8.4 target | NOT EXECUTED in this phase: the working available image is MySQL 8.0.36; results must not be represented as MySQL 8.4 verification |

The final suite includes real HTTP requests against an embedded Tomcat server with cookie storage and actual rendered CSRF tokens, on both H2 and MySQL. It checks registration, session ID rotation, HttpOnly/SameSite cookies, profile rendering/update, real CSRF and access-denied pages, current-password changes, expiry of a second session, rejection of the old password, login with the new password, and logout. This is HTTP verification, not a visual browser audit.

Other account checks cover normalized email, duplicate and concurrent duplicate registration, weak/invalid/oversized passwords, unknown/disabled accounts, generic login failures, CSRF for every mutation, anonymous restrictions, CUSTOMER vs ADMIN authorization, mass-assignment attempts, access to only the signed-in profile, escaped stored text, wrong current passwords, invalid confirmation and local-only saved redirects.

Real MySQL runs exposed two previously unverified test assumptions in the Phase 3.1 suite. JDBC URL parameters were appended with `&` even when no `?` existed; the containers now build these parameters via `withUrlParam`. MySQL CHECK failures use code 3819/SQL state HY000, which Spring wraps differently from H2's 23513. The contract now checks the exact CHECK error code and constraint name for either engine, and also verifies that the invalid writes left the original row values intact. Constraints were not removed or weakened.

Evidence is under ignored `target/`: `phase32-baseline.log`, `phase32-verify.log`, `phase32-mysql-verify.log`, `phase32-mysql-final.log`, `phase32-final-verify.log`, `surefire-reports/` and `failsafe-reports/`. Earlier failed runs are retained as diagnostics, not counted as passing tests.

### Local startup follow-up

The user's screenshot of `/login` showed the deliberate database-free preview page. Inspection confirmed that IntelliJ had launched `ApexMotorsApplication` without an active `mysql` profile, and the running MySQL 8.0.36 instance had no `apex_motors` database. Created that missing database with UTF8MB4 and started the packaged application using the existing private credentials and `mysql` profile. Flyway applied V1/V2 successfully; seven application tables and both successful history records were verified. No user account was created and no existing data was deleted.

After verifying the account application on temporary port 8081, replaced the identified workspace preview process on port 8080 with the account application, bound to loopback. Confirmed HTTP 200 for `/login`, `/register`, account CSS/JS and the homepage; the login and registration responses contain their actual forms, and login includes email/password/CSRF fields. Stopped the temporary 8081 process. The local application remains available at `http://localhost:8080/login`. Logs are `target/phase32-local-mysql.log` and `target/phase32-local-8080.log`. README now explains how to select **APEX MOTORS MySQL** in IntelliJ so future runs enable accounts too. This startup check does not replace the outstanding visual browser audit.

## Manual setup

1. Install/select JDK 21. Start MySQL and configure private credentials as described in README. Do not write credentials into tracked files.
2. Use an empty `apex_motors` schema or one already managed by V1/V2. If it was created manually without Flyway history, preserve its data, compare the schema/seed state and plan intentional Flyway adoption; do not enable automatic baselining or drop tables.
3. Run `.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=mysql'`. The default port is 8080. Use `dev,mysql` for template reloads.
4. Visit `/register`, create your own customer account, sign in and check `/profile`. Sign-out is a form button in the profile area.
5. To create the first ADMIN authority, register the intended owner's account normally. A trusted operator may run a private copy of `tools/promote_admin.sql`, after verifying the database and replacing its empty email variable with the exact intended account. Verify the result, then sign out/sign in. The empty script does nothing; no default admin credentials are supplied.
6. Before publishing online, provide HTTPS, set `SESSION_COOKIE_SECURE=true`, configure trusted proxy handling and add deployment-level abuse controls.

```powershell
.\mvnw.cmd verify
.\mvnw.cmd verify -Pmysql-it '-Dapex.mysql.test-image=mysql:8.0.36'
# Default target, when a working MySQL 8.4 image is available:
.\mvnw.cmd verify -Pmysql-it
node --test tools/vehicle-gallery.test.cjs
```

## Outstanding issues and Phase 3.3 readiness

- A visual pass at desktop/tablet/mobile sizes and keyboard/reduced-motion verification remains to be completed when browser control is available. Automated HTML rendering and accessibility attributes were checked, but that is not equivalent to visual review.
- MySQL 8.4 must be validated separately before claiming compatibility with the configured Compose target.
- Login/registration rate limiting, email ownership verification, password recovery, operational security monitoring and production deployment configuration are not implemented. No internet-facing production-readiness claim is made.
- The session registry is local to one JVM. Multiple application instances require shared sessions/invalidation. Operator role changes require reauthentication; a future admin interface must define immediate revocation rules.
- Automated tests did not use the persistent development database. The later local-startup follow-up initialized the previously absent `apex_motors` schema and left the account application running on port 8080. The temporary H2 preview from the earlier attempt and temporary MySQL application on port 8081 were stopped.
- No authentication bypass, hardcoded ADMIN password, public promotion endpoint, payment flow, booking UI or administrator dashboard was added.
- Phase 3.3 can build on the reserved ADMIN route rules and existing repositories. Vehicle rendering still uses the preserved in-memory catalog and Ferrari JSON; switching that rendering to the database is separate work.
- The user authorized commit/push after receiving the implementation and test results. This work has not been merged into `main`.

Implementation references: [Spring Security form login](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/passwords/form.html) and [session management](https://docs.spring.io/spring-security/reference/6.5/servlet/authentication/session-management.html).
