# APEX MOTORS — Phase 3.1 Database Foundation

APEX MOTORS is a server-rendered luxury automotive showroom built with Java 21, Spring Boot, Spring MVC, Thymeleaf, Bootstrap and GSAP. Phase 2 preserves the Phase 1 routes and backend architecture while replacing the previous real-time 3D experiment with a cinematic presentation and a reusable image-sequence 360° viewer.

Phase 3.1 adds an opt-in MySQL 8.4 persistence foundation. The default and `dev` profiles still run the complete Phase 2 showroom without a database. Authentication and booking workflows are not implemented.

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

`V1__database_foundation.sql` creates tables, foreign keys, checks and indexes. `V2__seed_showroom_catalog.sql` inserts two roles, eight brands, the eight current showroom vehicles and eight hero URL records. Prices are the existing demonstration EUR prices, not new quotations. Availability is `UNSPECIFIED` because the source catalog does not track inventory status. Brand descriptions remain null because none exist in the source. The Ferrari 24-image manifest remains authoritative and is not duplicated in MySQL. Existing presentation-only fields (powertrain, highlights, body style, etc.) remain intact in the in-memory catalog.

Versioned seed migrations run once under Flyway's history/locking mechanism; restarting does not overwrite local data. No user or administrator account is seeded. The foundation accepts encoded bcrypt hashes, rejects plaintext at entity validation, and deliberately supplies no password encoder or authentication API. All stored `DATETIME(6)` values represent UTC; future forms must convert users' local times at their boundary.

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

Compose pins the MySQL 8.4 release line, creates `demotesting`, uses a project-scoped persistent `mysql_data` volume, restarts unless stopped, and checks an authenticated `SELECT 1`. MySQL inherits `utf8mb4` / `utf8mb4_0900_ai_ci`, InnoDB and UTC. The host port binds **only to `127.0.0.1`**, never all interfaces. The app uses the non-root account scoped to `demotesting`; local Flyway migrations require DDL privileges in that database. Production separation of migration/runtime privileges is a later deployment concern.

| Variable | Meaning |
|---|---|
| `MYSQL_PORT` | Host port, default `3306`; choose another free port if necessary |
| `MYSQL_USER` | Non-root application/database migration user; example `apex_app` |
| `MYSQL_PASSWORD` | Application user's local secret; required |
| `MYSQL_ROOT_PASSWORD` | Container initialization secret; not used by Spring |
| `DB_HOST` | JDBC host, default `127.0.0.1` |
| `MYSQL_SSL_MODE` | Connector/J TLS mode, default `PREFERRED` for local development |

`.env`, `.env.*` (except the example), local application configuration files, `secrets/`, compressed SQL exports and dumps are ignored. Do not run `docker compose config` without `--quiet` in shared logs: resolved environment values can contain secrets.

Connect from a client with host `127.0.0.1`, the configured port, database `demotesting`, and the application user's credentials from `.env`. In MySQL Workbench/IntelliJ Database, enter the password privately in the credential field. From the container, request an interactive password prompt:

```powershell
docker compose exec mysql sh -c 'mysql -u "$MYSQL_USER" -p demotesting'
```

Stop while keeping the data:

```powershell
docker compose stop mysql
```

Do not remove volumes to fix a startup issue. Container initialization variables apply only to an empty data directory; changing `.env` does not rotate credentials in an existing database. Back up and deliberately manage existing users/credentials instead. Never point this development configuration at an unknown or production database.

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

After V1/V2 have run, never edit their checksums on an established database. Add a new versioned migration for future changes. Flyway clean is disabled. If a checksum mismatch appears, restore the original migration and investigate; do not blindly run repair. MySQL DDL can commit implicitly: a failed migration needs inspection and a reviewed recovery plan, not automatic table deletion or baselining.

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

H2 is test-scoped, runs the same SQL in MySQL compatibility mode, and still uses `ddl-auto=validate`. It checks isolated application behavior; it is **not** proof of MySQL compatibility. The opt-in Maven `mysql-it` profile runs `MySqlDatabaseIT` through Failsafe and fails when Docker is unavailable. It uses random test credentials, a fresh ephemeral database, and loopback-only MySQL port binding; it does not connect to `.env`'s database or reuse/delete development volumes. Tests cover exact catalog seeds, decimal values, lazy associations, UTC audit fields, unique/foreign/check constraints, restricted/cascading deletion, migration re-entry, password serialization, profile isolation and Ferrari rendering.

Implementation and verification status: [PHASE3_DATABASE_REPORT.md](PHASE3_DATABASE_REPORT.md).

## Start on another computer

The Phase 3.1 implementation is on `phase-3-database-foundation`. It includes the existing Phase 2 showroom and keeps MySQL optional. Real MySQL verification remains incomplete as detailed in the implementation report.

Clone **the `phase-3-database-foundation` branch** (these changes are not automatically merged into `main`):

```shell
git clone --branch phase-3-database-foundation --single-branch https://github.com/dragon23052005/APPEX_MOTOR.git APEX-MOTORS
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
- Database-backed page rendering, authentication, paint configuration, door animation, engine audio and driving simulation remain out of scope. Phase 3.1 only adds the opt-in persistence foundation described above.

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
