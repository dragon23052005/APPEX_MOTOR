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
    |-- assets/cars/  Future per-vehicle hero, gallery and 360° sequences
    |-- css/          Cinematic responsive design system
    |-- images/       Current generated studio photographs
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
2. Choose the `APPEX_MOTOR` folder containing `pom.xml`.
3. Wait for the Maven import and dependency download to finish.
4. Open **File → Project Structure → Project**, select a Java 21 SDK and language level 21.
5. Open **Settings → Build, Execution, Deployment → Build Tools → Maven → Runner** and select the same Java 21 JDK.
6. Open `src/main/java/com/apexmotors/ApexMotorsApplication.java`.
7. Click the green Run icon beside `main`, then visit <http://localhost:8080>.

If IntelliJ reports missing Spring or Jakarta packages, open the Maven tool window and click **Reload All Maven Projects**. Confirm that Maven uses JDK 21 rather than compiling the source folder as a plain Java project.

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
- Current PNG studio photographs are original assets generated for this project. Future WebP/AVIF media can be added through the documented `assets/cars` structure.
- Database persistence, authentication, paint configuration, door animation, engine audio and driving simulation remain out of scope.
