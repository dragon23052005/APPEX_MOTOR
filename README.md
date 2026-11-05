# APEX MOTORS — Phase 3.2 Authentication & Authorization

APEX MOTORS is a server-rendered luxury automotive showroom built with Java 21, Spring Boot, Spring MVC, Thymeleaf, Bootstrap and GSAP. Phase 2 preserves the Phase 1 routes and backend architecture while replacing the previous real-time 3D experiment with a cinematic presentation and a reusable image-sequence 360° viewer.

Phase 3.2 adds database-backed customer registration, sign-in, profile editing and password changes to the Phase 3.1 persistence foundation. Account features are enabled only with the `mysql` profile. The default and `dev` profiles still run the complete showroom without a database. Booking workflows and the administrator interface remain reserved for later phases.

## Phase 3.1 architecture

The existing `VehicleRepository` / `InMemoryVehicleRepository` and `JsonVehicleGalleryRepository` continue serving every page in **all profiles**, including `mysql`. No route, template, animation, Ferrari manifest, original PNG or image URL is changed by this phase. The database is prepared for a later, separately reviewed application adapter.

`com.apexmotors.persistence` contains independent JPA entities, seven Spring Data repositories, profile configuration and an internal startup verifier. The `mysql` profile enables DataSource, Flyway, entity scanning and repository scanning. A conditional `!mysql` configuration document excludes database auto-configuration only when MySQL is not requested; there are no global exclusions on the application class. `dev,mysql` is supported.

Flyway is the schema authority. Hibernate uses `ddl-auto=validate`, Open Session in View is disabled, and SQL initialization is disabled. Relationships are lazy and unidirectional, with no cascading JPA deletes. Repositories provide transaction boundaries for individual operations; future multi-repository workflows must use service-level `@Transactional`. Controllers continue using the existing view models, not JPA entities. Password hashes and entity relationships are excluded from JSON serialization.

| Table | Purpose and relationships |
|---|---|
| `roles` | Unique `ADMIN` / `CUSTOMER` roles |
| `users` | Unique email, encoded password hash, required role, UTC audit timestamps |
| `brands` | Unique slug; referenced by vehicles |
| `vehicles` | Unique slug, brand, decimal price/performance, optional unknown specifications, UTC audit timestamps |
| `vehicle_images` | URL metadata, category/order/primary flag; unique vehicle+URL and vehicle+order |
| `appointments` | Required user and vehicle; UTC appointment time and audit timestamps; status only, no booking workflow |
| `favorites` | Composite primary key `(user_id, vehicle_id)` and UTC creation time |

The automatic Flyway migrations are in `src/main/resources/db/migration/managed/`. Their original contents/checksums are preserved. `V1__database_foundation.sql` creates tables, foreign keys, checks and indexes. `V2__seed_showroom_catalog.sql` inserts two roles, eight brands, the eight current showroom vehicles and eight hero URL records. Prices are the existing demonstration EUR prices, not new quotations. Availability is `UNSPECIFIED` because the source catalog does not track inventory status. Brand descriptions remain null because none exist in the source. The Ferrari 24-image manifest remains authoritative and is not duplicated in MySQL. Existing presentation-only fields (powertrain, highlights, body style, etc.) remain intact in the in-memory catalog.

Versioned seed migrations run once under Flyway's history/locking mechanism; restarting does not overwrite local data. No user or administrator account is seeded. Phase 3.2 uses BCrypt to create password hashes and the existing entity validation still rejects plaintext hashes. All stored `DATETIME(6)` values represent UTC; future forms must convert users' local times at their boundary.

## Phase 3.2 accounts

`com.apexmotors.auth` contains Spring Security configuration, a database-backed `UserDetailsService`, transactional account services, form DTOs, controllers and session invalidation. Spring Boot manages the Spring Security 6 dependency versions. Authentication uses an HTTP session, not JWTs or browser storage. Existing `users` and `roles` tables are reused; no new migration or authentication table is required, and managed V1/V2 remain unchanged.

| Route | Access and behavior with `mysql` active |
|---|---|
| `GET /login`, `POST /login` | Public sign-in; Spring Security processes the POST using email/password |
| `GET /register`, `POST /register` | Public registration; new accounts always receive CUSTOMER |
| `POST /logout` | CSRF-protected sign-out; invalidates the current session |
| `GET /profile` | CUSTOMER or ADMIN; shows only the signed-in account's public profile fields |
| `POST /profile` | Updates the signed-in user's full name and optional phone only |
| `POST /profile/password` | Requires the current password, validates the new password and signs out all registered sessions after commit |
| `/admin`, `/admin/**` | ADMIN only; no administrator pages or operations exist yet, so authorized requests currently return 404 |
| `/appointments/**`, `/favorites/**` | Reserved for CUSTOMER/ADMIN; workflows are not implemented |
| `/access-denied` | A branded 403 page, also used for invalid/expired CSRF submissions |
| Existing showroom routes and assets | Public, including Ferrari gallery content |

Without `mysql`, the account navigation link is hidden and account/reserved routes return a friendly 503 page. Database and Spring Security auto-configuration are excluded only in this database-free mode. Existing public enquiry submissions remain demonstrations; with `mysql` active, they also require CSRF tokens supplied by Thymeleaf.

### Run and use accounts locally

1. Use JDK 21. Start MySQL and configure `DB_HOST`, `MYSQL_PORT`, `MYSQL_USER` and `MYSQL_PASSWORD` in your private `.env`, or use the ignored `application-local.properties` for an existing installation. Do not copy passwords into tracked source.
2. Ensure `apex_motors` is empty or already managed by the unchanged Flyway V1/V2 migrations. If you previously ran the manual SQL client scripts, follow the existing-schema instructions below; do not enable automatic baselining or delete data to work around missing history.
3. Run `.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=mysql'` (or `dev,mysql`). Open `/register`, create a customer account, then sign in at `/login`. No default account exists.
4. Edit your name/phone at `/profile`. Email and role are displayed but cannot be changed through the public forms. Changing your password requires the old password and signs out the current browser and other sessions on their next request.

The Compose target remains MySQL 8.4. If an existing MySQL instance already occupies port 3306, use it with the appropriate private configuration, or choose a different `MYSQL_PORT` for Compose. Do not stop or replace an unrelated database container to start this project.

If `/login` displays “The showroom is yours to explore” and “Account services are not available”, the application is running without the `mysql` profile. Starting MySQL alone does not enable accounts. Stop the current application and select **APEX MOTORS MySQL** in IntelliJ's Run dropdown; this shared configuration activates `dev,mysql`. For a direct `ApexMotorsApplication` run, set program arguments to `--spring.profiles.active=mysql`. After restarting, open `http://localhost:8080/login`; the startup log must list `mysql` as an active profile. Keep the working directory at the project root so private database configuration is loaded.

### Security decisions

- Passwords require 12–64 characters, uppercase/lowercase letters and a number, and at most 72 UTF-8 bytes. They are hashed with BCrypt cost 12. Oversized login input is rejected without truncation or an error page. Passwords are never repopulated in HTML.
- Email is trimmed and lowercased. Duplicate detection has both a user-friendly precheck and the database unique constraint for concurrent requests. Login errors use the same message for an unknown account, wrong password and disabled account.
- Form binding explicitly allows only the documented fields. Client-supplied account IDs, roles, enabled flags, email changes and hashes cannot update another account or elevate privileges. Service writes are transactional and profile/password changes lock the account row.
- Spring Security provides session fixation protection, CSRF and POST logout. Sessions use HttpOnly, SameSite=Lax cookies, cookie-only tracking and a 30-minute idle timeout. For an HTTPS deployment, set `SESSION_COOKIE_SECURE=true`; configure TLS and trusted proxy handling at deployment time.
- Login restores a saved private GET path. Only a local allowlisted path is used; query strings, `next`/`redirect` parameters and saved URL hostnames are not used as destinations. `server.tomcat.use-relative-redirects=true` also keeps the servlet container from expanding these paths using the request's Host header.
- Changing a password expires all of that account's sessions after the database transaction commits. The session registry is in memory and applies to a single application instance; distributed deployment needs shared sessions/invalidation. Role changes made by an operator require sign-out/sign-in to refresh authorities.
- The existing public showroom remains public. New protected route families in future phases must be explicitly added to the authorization rules; do not assume new routes are automatically private.
- No remember-me cookie, email verification, password recovery or login/registration rate limiting is included. Add abuse controls and account recovery before an internet-facing production release. This phase does not claim production deployment readiness.

### Explicit initial ADMIN setup

Register the intended owner's account normally first. A trusted database operator can then use a private copy of `tools/promote_admin.sql`: connect to the intended database, set its initially empty `@apex_admin_email` to the verified account's email, and execute it deliberately. Confirm the reported account and role, then sign out and sign in again. The empty script is a no-op; it creates no account or password, and neither Flyway nor application startup invokes it. Do not expose this procedure through a public endpoint or commit a filled-in local copy. `/admin/**` remains reserved for Phase 3.3.

### Account verification

```powershell
# Unit, H2 account/security contracts, profile isolation, showroom regressions and packaging
.\mvnw.cmd verify
# All integration suites on the default MySQL 8.4 image; requires Docker
.\mvnw.cmd verify -Pmysql-it
# Same integration suites using a locally available MySQL release
.\mvnw.cmd verify -Pmysql-it '-Dapex.mysql.test-image=mysql:8.0.36'
node --test tools/vehicle-gallery.test.cjs
```

The image override changes only the isolated test containers, not application/Compose configuration. Results from MySQL 8.0.36 do not establish MySQL 8.4 compatibility. Tests never use the development database or its Docker volumes. `MySqlAuthenticationIT` runs the same 37 authentication scenarios as H2, including a real HTTP flow with cookies, rendered CSRF tokens and two concurrent sessions. Failsafe reports MySQL setup errors as failures instead of silently skipping them. See [PHASE3_AUTHENTICATION_REPORT.md](PHASE3_AUTHENTICATION_REPORT.md) for executed checks and remaining limitations.

## Local MySQL with Docker

Default showroom development does **not** require these steps. For database development, install/start Docker Desktop with Linux containers and use a free local port.

1. Copy `.env.example` to `.env` if it does not already exist. Replace **both** password placeholders with different random secrets. An existing `.env` must be preserved. This implementation may already have generated a local `.env`; its values are intentionally not printed in reports.
2. Keep values unquoted and simple. Compose reads `.env` automatically; the `mysql` Spring profile imports it as a properties file relative to the project working directory. Long random hexadecimal passwords work in both parsers. For arbitrary passwords containing escapes, quotes or interpolation characters, supply environment variables instead.
3. Validate and start:

```powershell
docker compose config --quiet
docker compose up -d --wait mysql
docker compose ps
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=mysql'
```

Compose pins the MySQL 8.4 release line, creates `apex_motors`, uses a project-scoped persistent `mysql_data` volume, restarts unless stopped, and checks an authenticated `SELECT 1`. MySQL inherits `utf8mb4` / `utf8mb4_0900_ai_ci`, InnoDB and UTC. The host port binds **only to `127.0.0.1`**, never all interfaces. The app uses the non-root account scoped to `apex_motors`; local Flyway migrations require DDL privileges in that database. Production separation of migration/runtime privileges is a later deployment concern.

| Variable | Meaning |
|---|---|
| `MYSQL_PORT` | Host port, default `3306`; choose another free port if necessary |
| `MYSQL_USER` | Non-root application/database migration user; example `apex_app` |
| `MYSQL_PASSWORD` | Application user's local secret; required |
| `MYSQL_ROOT_PASSWORD` | Container initialization secret; not used by Spring |
| `DB_HOST` | JDBC host, default `127.0.0.1` |
| `MYSQL_SSL_MODE` | Connector/J TLS mode, default `PREFERRED` for local development |

`.env`, `.env.*` (except the example), local application configuration files, `secrets/`, compressed SQL exports and dumps are ignored. Do not run `docker compose config` without `--quiet` in shared logs: resolved environment values can contain secrets.

For an existing local MySQL installation with different credentials, put `spring.datasource.username` and `spring.datasource.password` in the ignored root file `application-local.properties`. The `mysql` profile imports this optional file after `.env`, preserving workstation overrides without storing passwords in tracked source. Keep this file private; omit it when you want the Compose credentials from `.env`. The previous published commit contained literal datasource credentials; they have been removed from the current tracked configuration, but remain in Git history. Rotate that password before relying on it again.

Connect from a client with host `127.0.0.1`, the configured port, database `apex_motors`, and the application user's credentials from `.env`. In MySQL Workbench/IntelliJ Database, enter the password privately in the credential field. From the container, request an interactive password prompt:

```powershell
docker compose exec mysql sh -c 'mysql -u "$MYSQL_USER" -p apex_motors'
```

Stop while keeping the data:

```powershell
docker compose stop mysql
```

Do not remove volumes to fix a startup issue. Container initialization variables apply only to an empty data directory; changing `.env` does not rotate credentials in an existing database. Back up and deliberately manage existing users/credentials instead. Never point this development configuration at an unknown or production database.

Changing the configured database name to `apex_motors` does not rename a database inside an existing volume. If a database has already been initialized under another name, preserve its data and prepare a reviewed migration/import into `apex_motors`, with the required application-user privileges, before switching the running application. Do not delete the volume to apply the name change.

## Chạy SQL trực tiếp trong Workbench / IntelliJ Database

1. Khởi động MySQL 8.0.16 trở lên rồi kết nối tới server bằng tài khoản có quyền tạo database và bảng. Script thủ công đã được chạy thử trên MySQL 8.0; cấu hình Docker của dự án vẫn dùng 8.4. Khi `apex_motors` chưa tồn tại, để trống ô database/schema trong cấu hình kết nối.
2. Mở `src/main/resources/db/migration/V1__database_foundation.sql` (file ngay trong thư mục `migration`, không phải bản trong `managed`). Chạy **toàn bộ file** trên cùng một kết nối, với chế độ dừng khi gặp lỗi. Trong Workbench, dùng **Execute All or Selection** khi không chọn riêng đoạn nào; trong IntelliJ dùng **Run SQL Script** với data source MySQL.
3. Script tạo `apex_motors` nếu chưa có, chọn database, đặt múi giờ UTC và tạo 7 bảng InnoDB/UTF8MB4 cùng khóa và index. Kết quả cuối liệt kê các bảng. Refresh **Schemas** trong Workbench hoặc **Synchronize** trong IntelliJ để thấy chúng.
4. Nếu cần dữ liệu mẫu, chọn `apex_motors` rồi chạy `src/main/resources/db/migration/V2__seed_showroom_catalog.sql` **một lần**, trên cùng kết nối sau khi V1 thành công. V1 chỉ tạo cấu trúc; V2 thêm 2 vai trò, 8 hãng, 8 xe và 8 ảnh hero, không tạo tài khoản người dùng.

V1 dành cho database mới/rỗng. Nếu báo **Table already exists**, dừng và kiểm tra dữ liệu hiện có; script không xóa hoặc sửa bảng để chạy lại. Chạy thiếu dòng `USE apex_motors` có thể gây lỗi **No database selected**. Mỗi kết nối nhập thời gian trực tiếp về sau cần đặt `SET SESSION time_zone = '+00:00'` và dùng giá trị UTC.

Chọn một cách khởi tạo cho mỗi database: **chạy SQL thủ công** hoặc **Spring Boot/Flyway tự chạy**. Bản thủ công không tạo lịch sử Flyway. Vì vậy, nếu tạo bảng thủ công rồi khởi động profile `mysql` trên cùng database, Flyway sẽ từ chối schema chưa có lịch sử. Muốn dùng ngay profile `mysql`, hãy để Flyway tạo bảng trên một database rỗng. Việc tiếp nhận database đã tạo thủ công cần đối chiếu schema/dữ liệu và thiết lập baseline có chủ đích theo hướng dẫn bên dưới; ứng dụng không tự thực hiện việc này. Profile mặc định vẫn chạy showroom bình thường.

## Profiles, migrations and IntelliJ

```powershell
# Existing showroom: no MySQL required
.\mvnw.cmd spring-boot:run
# Existing IntelliJ development mode: also no MySQL required
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev'
# Persistence plus development template reloads
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev,mysql'
# Packaged application (run from the project directory to read .env)
java -jar target/apex-motors-0.0.1-SNAPSHOT.jar --spring.profiles.active=mysql
```

In IntelliJ, keep Project SDK and Maven Runner on Java 21 and reload Maven. The existing **APEX MOTORS** shared configuration stays database-free. Use the added **APEX MOTORS MySQL** configuration after starting Compose, or duplicate the existing Maven configuration and set `-Dspring-boot.run.profiles=dev,mysql`. Keep the working directory at `$PROJECT_DIR$` so `.env` resolves. No credentials are stored in shared run configurations. Alternatively run `ApexMotorsApplication` with active profile `mysql` and the same working directory/environment.

On a **fresh** database, application startup applies V1 then V2 and validates all entity mappings. A private startup check verifies `SELECT 1`, current Flyway migrations/history and all seven tables. Its success log contains no credentials; it is not an HTTP endpoint.

For an **existing nonempty database without Flyway history**, startup intentionally refuses to continue (`baseline-on-migrate=false`). Do not enable automatic baselining, run `clean`, change Hibernate to `update`, or alter unknown tables. First take a verified backup and inventory the schema/data. Prefer a separate fresh database for evaluation. An intentional adoption requires an owner-reviewed schema comparison and migration/import plan; only establish a baseline version after proving exactly which migrations are already represented, including seed state. This project does not perform that adoption automatically.

After the managed V1/V2 have run, never edit their checksums on an established database. Add a new versioned migration under `db/migration/managed/` for future changes. Flyway scans only that directory; the top-level client scripts are excluded. When changing the schema, keep the manual setup script consistent with the resulting managed schema. Flyway clean is disabled. If a checksum mismatch appears, restore the original migration and investigate; do not blindly run repair. MySQL DDL can commit implicitly: a failed migration needs inspection and a reviewed recovery plan, not automatic table deletion or baselining.

Troubleshooting:

- **Connection refused / timeout:** verify Docker Desktop is running, Compose reports healthy, the `.env` port is free and `DB_HOST` is correct.
- **Access denied:** verify `.env` matches the credentials used when the persistent volume was first initialized. Do not delete the volume.
- **Missing `MYSQL_USER` / `MYSQL_PASSWORD`:** check the working directory and environment. Spring imports `.env` only for `mysql`; environment variables can override the file.
- **Port already allocated:** change `MYSQL_PORT` in `.env`, then recreate only the Compose service with `docker compose up -d --wait mysql`; preserve the volume.
- **Nonempty schema / checksum / validation error:** stop and inspect migration history and schema. Do not use destructive recovery shortcuts.
- **Docker unavailable:** the default profile and H2 tests still work. Real MySQL acceptance remains unverified until `mysql-it` passes on a Docker-capable host.
- **Java native memory allocation failure:** free memory used by other development workloads and run Maven with `$env:MAVEN_OPTS='-Xmx256m'` and `.\mvnw.cmd verify '-DargLine=-Xmx512m'`. Add `-Pmysql-it` only when Docker is running and sufficient memory is available for MySQL too.
- **Testcontainers reports Docker API 400 / client too old:** the test-only `docker-java.properties` selects API 1.44 for Spring Boot's managed Docker client. Use a Docker engine supporting API 1.44 or newer. This follows the client's documented [API version setting](https://github.com/docker-java/docker-java/blob/main/docs/getting_started.md); application dependency versions remain managed by Spring Boot.

## Phase 3.1 tests

```powershell
# Compile, all existing regressions, entity validation, H2 migration/repository checks, package
.\mvnw.cmd verify
# Additionally use a fresh real MySQL 8.4 Testcontainers instance (Docker required)
.\mvnw.cmd verify -Pmysql-it
# Existing gallery browser-state tests
node --test tools/vehicle-gallery.test.cjs
```

H2 is test-scoped, runs the same managed migrations in MySQL compatibility mode, and still uses `ddl-auto=validate`. It checks isolated application behavior; it is **not** proof of MySQL compatibility. The opt-in Maven `mysql-it` profile runs `MySqlDatabaseIT` and `MySqlManualSetupIT` through Failsafe and fails when Docker is unavailable. They use random test credentials, fresh ephemeral databases, and loopback-only MySQL port binding; they do not connect to `.env`'s database or reuse/delete development volumes. Tests cover exact catalog seeds, decimal values, lazy associations, UTC audit fields, unique/foreign/check constraints, restricted/cascading deletion, migration re-entry, password serialization, profile isolation and Ferrari rendering. The manual setup check starts with no selected database, executes the actual client scripts, verifies all seven tables and seeds, and verifies that rerunning V1 fails without overwriting data.

To check only the manual script on MySQL 8.0 (the locally verified client path), run:

```powershell
.\mvnw.cmd -Pmysql-it '-Dit.test=MySqlManualSetupIT' '-Dapex.mysql.manual-test-image=mysql:8.0' test-compile failsafe:integration-test failsafe:verify
```

This check does not replace the full MySQL 8.4 application integration suite.

Implementation and verification status: [PHASE3_DATABASE_REPORT.md](PHASE3_DATABASE_REPORT.md).

## Start on another computer

Phase 3.2 is developed on `phase-3-authentication`, starting from commit `8180e6d`. It includes the existing showroom and keeps MySQL optional. Publication of this branch is authorized; merging into `main` remains a separate action. Verification details are in `PHASE3_AUTHENTICATION_REPORT.md`.

Clone **the `phase-3-authentication` branch** (these changes are not automatically merged into `main`):

```shell
git clone --branch phase-3-authentication --single-branch https://github.com/dragon23052005/APPEX_MOTOR.git APEX-MOTORS
```

Alternatively, select that branch on GitHub before choosing **Code → Download ZIP**, then extract the complete ZIP.

Open the downloaded **`pom.xml` as a project** in IntelliJ, let Maven finish syncing, and run the shared **APEX MOTORS** configuration. The checkout can have any folder name and live anywhere; do not copy `.idea` from another computer.

**Required on each computer:** a JDK 21 installation and internet access for the first Maven/dependency download. If IntelliJ cannot find a JDK, choose **Download JDK → 21** in Project SDK once. JDK locations are machine-specific and are deliberately not committed. No separate Maven installation, database, secrets, Node.js, Python or manual image-path configuration is needed to run the application. Browser CDN dependencies still require internet access while using the site.

The repository includes the official Maven Wrapper, pinned to Maven **3.9.16**, plus a relative-path IntelliJ run configuration. Maven reports a clear error if it is launched with the wrong Java version. All required vehicle images are committed and packaged as classpath resources.

## Features

- Fullscreen cinematic homepage with restrained GSAP entrance, reveal and parallax motion
- Editorial vehicle collection with server-side search, manufacturer filters and sorting
- Dynamic vehicle details, specifications, highlights and private enquiry links
- Reusable image-sequence 360° viewer with drag, touch, keyboard, autoplay, zoom, reset and fullscreen controls
- Progressive frame loading, nearby-frame preloading, in-memory frame cache and image error fallback
- Static studio presentation when a complete image sequence is unavailable
- Dedicated generated studio hero for every named vehicle; no cross-model image reuse
- Ferrari SF90 Stradale editorial gallery: 24 supplied photographs, English captions, category filters, zoom and fullscreen lightbox (not a 360° rotation)
- Responsive navigation, mobile layouts and `prefers-reduced-motion` support
- Validated contact form with demonstration submission handling
- Custom 404 page
- Replaceable repository abstraction backed by an in-memory Java implementation

## Current 360° asset status

The repository does not currently contain a complete, licensed multi-angle sequence for any vehicle. Therefore every vehicle intentionally has `viewerEnabled=false` and displays its studio image. The 360° controls are not rendered for unavailable sequences.

The viewer code and Java metadata are complete and ready for real assets; the project does not copy one image into fake frames or simulate missing angles.

## Technology

- Java 21 LTS
- Spring Boot 3.5.7
- Spring MVC and Thymeleaf
- Maven 3.9.16 through the included Maven Wrapper
- Bootstrap 5.3.8 via CDN
- GSAP 3.13 and ScrollTrigger via CDN
- HTML5, CSS3 and modular vanilla JavaScript

The application itself runs only on the Java/Spring Boot runtime. Bootstrap, GSAP and Google Fonts require browser internet access because they are loaded from CDNs.

## Architecture

```text
src/main/java/com/apexmotors/
|-- config/          Static media caching configuration
|-- controller/      Spring MVC route handlers
|-- dto/             Contact form and collection query DTOs
|-- model/           Vehicle and optional image-sequence metadata
|-- repository/      Replaceable abstraction + in-memory implementation
`-- service/         Search, sorting, lookup and demo submission logic

src/main/resources/
|-- templates/       Thymeleaf pages and reusable fragments
`-- static/
    |-- assets/cars/  Per-vehicle generated hero images and future 360° sequences
    |-- css/          Cinematic responsive design system
    |-- images/       Shared homepage and editorial studio photographs
    `-- js/           Site motion and reusable image-sequence viewer
```

`VehicleRepository` remains storage-agnostic, so a later Spring Data JPA adapter can replace the in-memory repository without changing controllers or `VehicleService`.

## Image-sequence metadata

`Vehicle` owns a required `VehicleImageSequence` value with:

- `viewerEnabled`
- `frameCount`
- `framePathPattern`
- `posterImage`

Disabled vehicles use `VehicleImageSequence.unavailable(posterImage)`. An enabled sequence must contain at least two frames and its pattern must contain `{frame}`. At runtime the token is replaced with a one-based, three-digit number.

Example asset layout:

```text
src/main/resources/static/assets/cars/vehicle-slug/
|-- hero.webp
|-- gallery/
|   `-- detail-01.webp
`-- 360/
    |-- frame-001.webp
    |-- frame-002.webp
    `-- ... frame-036.webp
```

Example metadata after all files have been added and checked:

```java
new VehicleImageSequence(
    true,
    36,
    "/assets/cars/vehicle-slug/360/frame-{frame}.webp",
    "/assets/cars/vehicle-slug/hero.webp"
)
```

All frames must depict the same vehicle, framing and lighting. Confirm redistribution rights before committing an image sequence.

## Viewer behaviour

The viewer initially fetches only the current frame and a small group around it. It then preloads nearby frames and a limited forward group during browser idle time. It pauses automatic rotation when offscreen or when the tab is hidden, clears its cache on page exit, and retains the poster image if a required frame fails.

Keyboard controls when a viewer is enabled:

| Key | Action |
|---|---|
| Left / Right | Rotate one frame |
| `+` / `-` | Zoom in / out |
| Space | Play / pause |
| Home | Reset angle and zoom |
| `F` | Enter / exit fullscreen |

## Run from the command line

Prerequisites: JDK 21 and internet access for the first dependency download. Run commands from the directory containing `pom.xml`. On Windows, set `JAVA_HOME` to your own JDK 21 installation if it is not already configured. IntelliJ can manage its Project SDK independently of the terminal.

```powershell
java -version
.\mvnw.cmd -version
.\mvnw.cmd spring-boot:run
```

On macOS/Linux use `./mvnw` instead of `.\mvnw.cmd`. If a ZIP extraction tool discards executable permissions, run `sh mvnw spring-boot:run` or restore the executable bit with `chmod +x mvnw`.

Open <http://localhost:8080>.

Build and run the packaged application:

```powershell
.\mvnw.cmd clean verify
java -jar target/apex-motors-0.0.1-SNAPSHOT.jar
```

Run tests only:

```powershell
.\mvnw.cmd test
```

## Run in IntelliJ IDEA with Java 21

1. Select **File → Open**, choose the downloaded repository's **`pom.xml`**, and choose **Open as Project**. Do not open `src` as a plain Java project.
2. Wait for Maven sync. IntelliJ detects the committed wrapper on a fresh Maven import; no absolute Maven path is shared.
3. If prompted for a Project SDK, select an installed **JDK 21**, or choose **Download JDK → 21**. This is a one-time prerequisite on each computer, not a change to application configuration.
4. Select **APEX MOTORS** from the Run dropdown and click **Run**. The shared configuration in `.run/APEX MOTORS.run.xml` runs Maven's `spring-boot:run` with the `dev` profile and uses `$PROJECT_DIR$`, not the author's local path.
5. Open <http://localhost:8080>.

If the shared Run configuration has not appeared after import, open the **Maven** tool window and run **Plugins → spring-boot → spring-boot:run**, or run `.\mvnw.cmd spring-boot:run` in the IntelliJ terminal. Running `ApexMotorsApplication.main` is also supported after Maven sync.

If IntelliJ reports missing Spring or Jakarta packages, use **Reload All Maven Projects** and check the Maven sync output. For an existing IDE project with overridden Maven settings, select **Use Maven wrapper** and **Project JDK (21)** under **Settings → Build, Execution, Deployment → Build Tools → Maven** and its **Runner** section. Corporate proxies or offline mode must be handled in that computer's Maven settings; no credentials are stored in this repository.

The shared Run configuration already enables the `dev` profile to disable template caching. Packaged production runs keep template caching enabled. If port 8080 is occupied, stop your previous APEX run; alternatively use `.\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--server.port=8081'` and open port 8081. No source-file edit is needed.

See the official [Maven Wrapper documentation](https://maven.apache.org/tools/wrapper/) and [IntelliJ Maven import instructions](https://www.jetbrains.com/help/idea/maven-support.html) for wrapper/import behavior.

### Portability verification

On 15 October 2026, the staged project was exported to a new Windows folder with spaces in its name, without `.idea`, prior `target` output or unrelated local files. The committed wrapper ran `clean verify` successfully: **26 Java tests** passed and the executable JAR was built. All **11 optional JavaScript tests** passed. The same Maven goal and `dev` profile as the shared IntelliJ configuration started successfully; homepage, collection, about, contact and Ferrari routes returned HTTP 200, as did all 16 versioned hero variants and all 24 Ferrari gallery delivery images. The existing machine's JDK 21 and Maven dependency cache were used. This verifies a clean project copy on Windows, not a physical second computer or a macOS/Linux IntelliJ installation.

## Routes

| Route | Purpose |
|---|---|
| `/` | Cinematic homepage and featured vehicles |
| `/collection` | Searchable, filterable and sortable showroom |
| `/cars/{slug}` | Dynamic vehicle details with sequence viewer or honest static fallback |
| `/about` | Brand philosophy |
| `/contact` | Server-validated demonstration enquiry |
| Any unknown route | Custom 404 page |

## Phase boundary and limitations

- No complete 360° frame set is bundled yet, so the static fallback is the truthful active experience.
- Current vehicle specifications, prices and inventory are demonstration data, not sales quotations.
- Current per-vehicle and shared studio photographs are WebP assets generated for this project, with 960-pixel responsive variants for mobile and card layouts. Future AVIF media can be added through the documented `assets/cars` structure.
- Vehicle hero URLs (including both `srcset` variants and detail-page posters) use [Spring MVC content versioning](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-config/static-resources.html). Replacing a hero image and restarting the application produces a new URL, bypassing older cached responses while retaining the 30-day image cache. Original unversioned asset URLs remain available. If an already-open page shows broken images, reload it with Ctrl+F5; in IntelliJ, rebuild/restart the correct checkout first if resource changes have not been copied to `target/classes`.
- Database-backed vehicle page rendering, paint configuration, door animation, engine audio and driving simulation remain out of scope. Customer authentication is available through the opt-in `mysql` profile in Phase 3.2.

## Ferrari multi-angle gallery

Preview: [Ferrari SF90 Stradale gallery](http://localhost:8080/cars/ferrari-sf90-stradale#ferrari-gallery).
Open the Ferrari detail page and use **Explore 24 perspectives**. The original hero and other vehicle pages remain unchanged.

This is an editorial photo gallery, **not** a true 360° sequence. All 24 supplied PNGs are preserved in
`src/main/resources/static/assets/cars/ferrari-sf90-stradale/ferrari_sf90_20_views/`.
Several supplied views differ in door configuration, seat colors or body details; these are documented rather than corrected by inventing images.

- Source of truth: `src/main/resources/galleries/ferrari-sf90-stradale.json`. Edit `displayOrder` (unique positive numbers), category, English title/description and transition hint; restart Spring Boot to reload the manifest.
- The JSON repository loads and validates local original/full/thumbnail assets once at startup. Invalid entries fail fast with the asset path in the error.
- Full-size WebP delivery copies and 360px thumbnails are in `assets/cars/ferrari-sf90-stradale/gallery/`. Browser code retains at most five decoded image promises and preloads only adjacent views, after the gallery approaches the viewport.
- Use arrows / touch swipe / thumbnails to change photographs. `+` / `-` zoom; `0` resets zoom. Drag a zoomed image to pan, not to rotate a car.
- The fullscreen button opens a native modal lightbox that fills the browser viewport; Escape or ✕ closes it and restores focus. It does not request OS-level fullscreen, so browser chrome can remain visible.
- Category filtering preserves the current photograph if it belongs to the selected category, and otherwise selects that category's first ordered image. There is no autoplay.
- GSAP animates the viewer when available. Web Animations is the local fallback; reduced motion skips transitions. No new CDN or frontend runtime is needed.
- Without JavaScript, the initial photo and all thumbnail links still work; each link opens its untouched PNG source.

Checks:

```powershell
.\mvnw.cmd verify
# Optional development-only JS tests; Node is not needed to run the application:
node --test tools/vehicle-gallery.test.cjs
```

To regenerate delivery images after intentionally changing supplied images or manifest URLs, install Pillow in your Python development environment and run `python tools/prepare_ferrari_gallery.py`. This checks coverage/dimensions and original checksums; it never modifies the PNG sources. Committed WebPs make Python unnecessary for normal Maven builds and application startup. Change the asset URL when replacing an image because image responses are cached for 30 days; bump the CSS/JS version query when changing viewer code.

Full image classification, implementation notes, executed tests and remaining limitations: [FERRARI_GALLERY_REPORT.md](FERRARI_GALLERY_REPORT.md).
