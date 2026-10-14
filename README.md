# APEX MOTORS — Phase 2 Cinematic Showroom

APEX MOTORS is a server-rendered luxury automotive showroom built with Java 21, Spring Boot, Spring MVC, Thymeleaf, Bootstrap and GSAP. Phase 2 preserves the Phase 1 routes and backend architecture while replacing the previous real-time 3D experiment with a cinematic presentation and a reusable image-sequence 360° viewer.

There is no database, authentication, React, Node.js application runtime, Three.js, WebGL, Blender asset or GLB/GLTF model in this phase.

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
- Maven 3.9+
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

Prerequisites: JDK 21 and Maven 3.9 or newer.

```powershell
java -version
mvn -version
mvn spring-boot:run
```

Open <http://localhost:8080>.

Build and run the packaged application:

```powershell
mvn clean package
java -jar target/apex-motors-0.0.1-SNAPSHOT.jar
```

Run tests only:

```powershell
mvn test
```

## Run in IntelliJ IDEA with Java 21

1. Open IntelliJ IDEA and select **File → Open**.
2. Choose the `APPEX_MOTOR_PHASE2` folder containing `pom.xml`.
3. Wait for the Maven import and dependency download to finish.
4. Open **File → Project Structure → Project**, select a Java 21 SDK and language level 21.
5. Open **Settings → Build, Execution, Deployment → Build Tools → Maven → Runner** and select the same Java 21 JDK.
6. Open `src/main/java/com/apexmotors/ApexMotorsApplication.java`.
7. Click the green Run icon beside `main`, then visit <http://localhost:8080>.

If IntelliJ reports missing Spring or Jakarta packages, open the Maven tool window and click **Reload All Maven Projects**. Confirm that Maven uses JDK 21 rather than compiling the source folder as a plain Java project.

The default configuration enables Thymeleaf template caching for production-like performance. While actively editing templates in IntelliJ, add `--spring.profiles.active=dev` to the run configuration's program arguments to disable that cache.

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
- Database persistence, authentication, paint configuration, door animation, engine audio and driving simulation remain out of scope.

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
mvn verify
# Optional development-only JS tests; Node is not needed to run the application:
node --test tools/vehicle-gallery.test.cjs
```

To regenerate delivery images after intentionally changing supplied images or manifest URLs, install Pillow in your Python development environment and run `python tools/prepare_ferrari_gallery.py`. This checks coverage/dimensions and original checksums; it never modifies the PNG sources. Committed WebPs make Python unnecessary for normal Maven builds and application startup. Change the asset URL when replacing an image because image responses are cached for 30 days; bump the CSS/JS version query when changing viewer code.

Full image classification, implementation notes, executed tests and remaining limitations: [FERRARI_GALLERY_REPORT.md](FERRARI_GALLERY_REPORT.md).
