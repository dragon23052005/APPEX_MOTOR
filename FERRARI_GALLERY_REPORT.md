# Ferrari SF90 Stradale — Phase 2 Gallery Report

Reviewed: 14 October 2026. Project: APEX MOTORS, Phase 2 cinematic showroom.
Branch: `phase-2-cinematic-showroom`. No commits, pushes, merges or branch deletions.

## 1. Image analysis

All **24 supplied PNGs** were opened and visually inspected before implementation. Content was not inferred from UUID filenames. No replacement images, generated views or fabricated rotation frames were added.

Original directory, preserved unchanged:

```text
src/main/resources/static/assets/cars/ferrari-sf90-stradale/ferrari_sf90_20_views/
```

- 13 exterior images, including the two closing shots.
- 6 exterior/engine-bay detail images.
- 5 interior images. The console and gear selector share one photograph; they were not split into artificial additional views.
- 23 images are 1536 × 1024; one side profile is 1881 × 836.
- SHA-256 checks found 24 distinct file contents and 24 distinct RGB pixel contents. No exact duplicates.
- Checksums before and after WebP preparation confirmed that **all original PNGs remained unchanged**.

### Final editorial sequence

Each original path consists of the source directory above plus its filename. The delivery URL is `/assets/cars/ferrari-sf90-stradale/gallery/<id>.webp`. The manifest includes the complete image URL, original filename, display order, camera angle, English title/description, confidence, similarity notes and discrepancies for every image.

Screen-left/right describes the direction in the image, not an assumed driver/passenger side. The `displayOrder` values increase by 10 to allow future insertion without rewriting JavaScript.

| Order | ID | Category | Observed angle | Caption | Original filename |
|---|---|---|---|---|---|
| 1 | first-impression | exterior | Front three-quarter; nose screen-left | First impressions | `089e9270-2f71-4acf-b063-a1d24cc254e1.png` |
| 2 | front | exterior | Direct front | Face to face | `5ab74daa-6ecb-4763-8034-4fde805ad7de.png` |
| 3 | front-opposite | exterior | Opposite front three-quarter; nose screen-right | Another perspective | `e80d9a8c-a8c5-4148-84ca-366d5d111383.png` |
| 4 | side-right | exterior | Side profile; nose screen-right | An unbroken line | `5e372efb-e3d7-4b25-8c03-3dde0483fd76.png` |
| 5 | rear-quarter-right | exterior | Rear three-quarter; nose screen-right | A lasting signature | `5538b9cf-d935-4a72-bed9-13372abaa044.png` |
| 6 | rear | exterior | Direct rear / 180-degree viewpoint, not a rotation frame | Rear symmetry | `5ac8132d-1cd0-4047-a6cb-a2ddf97d2bf0.png` |
| 7 | rear-quarter-left | exterior | Opposite rear three-quarter; nose screen-left | Around the other side | `1cb86838-7588-471a-ba86-9a1152df5322.png` |
| 8 | side-left | exterior | Wide side profile; nose screen-left | The proportions of speed | `9a6507d5-bf03-4a3e-9975-677e522b8c1a.png` |
| 9 | elevated | exterior | Elevated diagonal / roof and body overview | Lines from above | `03edf745-508b-4e7e-8bec-7e76e9768d47.png` |
| 10 | doors-raised | exterior | Front three-quarter with two raised doors | Open doors — an image variation | `078a7b3e-b19e-4c63-bdf5-72d6d5a46491.png` |
| 11 | door-open | exterior | Rear three-quarter; one conventional door open | A glimpse inside | `8cce1e26-65b1-42fb-9360-1a66a0654685.png` |
| 12 | engine-open | details | Rear elevated three-quarter, engine cover raised | Beneath the engine cover | `d40a6191-4f75-465f-a75d-025639ee233d.png` |
| 13 | engine-detail | details | Close elevated engine bay | Inside the engine bay | `59d807c1-7a13-403c-bc11-25a1ae6d1ca4.png` |
| 14 | rear-detail | details | Close low rear, lights / exhaust / diffuser | Light and aerodynamics | `d8a73c28-0cf6-4c86-a212-2735b6a23a1a.png` |
| 15 | badge | details | Exterior Ferrari script and prancing horse close-up | An unmistakable signature | `ae76488e-7378-4ea0-88c7-549bb80d9bea.png` |
| 16 | wheel | details | Front wheel, brake and side sill close-up | Where power meets the road | `ee537867-4d64-48bf-88f9-89b9edfe1878.png` |
| 17 | headlight | details | Front headlight and fender close-up | A line of light | `d14e918a-40ad-4e57-b86e-dc3dd7247c75.png` |
| 18 | cockpit | interior | Wide cabin from passenger doorway | Step inside | `11c5a0a9-8540-4600-9256-2ad433a9758e.png` |
| 19 | dashboard | interior | Wide centered dashboard and cockpit | Built around the driver | `3b0e0bc4-d7ed-4c06-934e-85ddd61b81fa.png` |
| 20 | steering | interior | Steering wheel and instrument cluster close-up | The center of control | `aadba3ad-be4f-4ae1-8b2e-b2c8bb4a034f.png` |
| 21 | seats | interior | Two sport seats viewed through doorway | Shaped around you | `3aaa167d-75a0-4c1c-a45b-dfff37ed91b8.png` |
| 22 | console | interior | Center console / gear selector close-up | Details within reach | `9e54179e-adaf-4eaa-b147-24acba823a55.png` |
| 23 | rear-finale | exterior | Low rear three-quarter; cinematic closing view | An echo of red | `f37417c5-c6af-4823-b1af-1ef955ed7cec.png` |
| 24 | finale | exterior | Low dramatic front three-quarter; nose screen-left | The impression remains | `8758968e-7c01-476c-b251-d80fb0279988.png` |

### Narrative and visual similarity

Images 1–8 form an intentional walkaround: front three-quarter → direct front → opposite front → side → rear three-quarter → direct rear → opposite rear → opposite side. Elevated and open-compartment images follow; these are not equally spaced rotation frames.

Images 11–13 share a rear/open-compartment theme before moving into the engine bay. Images 14–17 move from the rear and badge toward the front wheel and headlight. Images 18–22 establish the cabin before showing the dashboard, steering wheel, seats and console. The final two wide views are an editorial closing montage, not continuous physical motion.

Similar compositions intentionally retained:

- `first-impression` / `finale`: similar front-facing direction, but different camera angle, lighting and flank coverage; separated into opening and closing views.
- `rear-quarter-right` / `rear-finale`: similar rear angle, different rear details and framing; separated in the sequence.
- `rear-quarter-left` / `door-open`: same rear orientation, different door position and crop.
- `front-opposite` / `doors-raised`: similar front orientation, different door positions.
- `engine-open` / `engine-detail`: the same subject at different distances.

Similarity was assessed visually. No automated embedding or near-duplicate model was run. Each manifest item has its own `similarityNote` and `duplicateNote`.

### Verification and ambiguity

Every image was inspected. The `visually-reviewed` status and 0.98–0.99 confidence refer only to **classification of visible content**, not proof that the images show a real production SF90 or one consistent vehicle configuration.

Preserved and documented discrepancies:

- `doors-raised` has raised doors, while `door-open` has an outward-opening conventional door.
- `cockpit` shows predominantly red seats; `seats` shows different, predominantly black seats with red accents.
- `headlight` differs from the headlight geometry in some full-car views.
- Exhaust locations, diffusers, plate lettering and rear details vary across some views.
- Mechanical accuracy of the engine-bay images and commercial redistribution rights have not been authenticated.

The gallery discloses configuration differences; the raised-door image carries an explicit explanatory caption. No separate taillight-only or interior-material source photograph exists, so none was invented. Vehicle geometry was not edited to hide inconsistencies.

## 2. Implementation

### Architecture

`VehicleController` looks up an optional gallery by slug through `VehicleGalleryService` → `VehicleGalleryRepository` → `JsonVehicleGalleryRepository`. The repository loads the manifest once at startup, validates unique IDs/orders/source names and checks all original, full-size and thumbnail resources. The immutable list is sorted by `displayOrder`.

Thymeleaf renders the first image and all thumbnail metadata from the manifest. JavaScript consumes those rendered `data-*` attributes; it does not scan folders or randomize order. Gallery CSS and JavaScript load only on the Ferrari page. Other vehicles keep the existing static gallery. Vehicle data, search/filter/sort, `VehicleRepository` and `image-sequence-viewer.js` are unchanged.

### Created files

- `src/main/java/com/apexmotors/model/GalleryImage.java`
- `src/main/java/com/apexmotors/model/VehicleGallery.java`
- `src/main/java/com/apexmotors/repository/VehicleGalleryRepository.java`
- `src/main/java/com/apexmotors/repository/JsonVehicleGalleryRepository.java`
- `src/main/java/com/apexmotors/service/VehicleGalleryService.java`
- `src/main/resources/galleries/ferrari-sf90-stradale.json`
- `src/main/resources/templates/fragments/vehicle-gallery.html`
- `src/main/resources/static/css/vehicle-gallery.css`
- `src/main/resources/static/js/vehicle-gallery.js`
- 24 delivery WebPs and 24 thumbnails in `src/main/resources/static/assets/cars/ferrari-sf90-stradale/gallery/`
- `src/test/java/com/apexmotors/VehicleGalleryTest.java`
- `tools/prepare_ferrari_gallery.py`
- `tools/vehicle-gallery.test.cjs`
- This report.

### Modified existing files

- `VehicleController.java`: optional gallery lookup.
- `templates/car-details.html`: gallery jump link, Ferrari fragment, conditional script and scoped body class.
- `templates/fragments/site.html`: conditional gallery stylesheet and cache version.
- `README.md`: usage, manifest structure, checks and IntelliJ instructions for the correct directory.

No `pom.xml` changes or new runtime dependencies. Java 21, Spring Boot 3.5.7, MVC and Thymeleaf remain in place. Node is only an optional development test runner; Python/Pillow is only an asset-preparation tool. Neither is required to run Spring Boot.

### Interaction and transitions

- All / Exterior / Details / Interior filters preserve the current image if it belongs to the selected category; otherwise they choose the first ordered image in that category. Counter, accessible labels and thumbnails update together after successful loading.
- Arrows, thumbnails, keyboard arrows and horizontal swipes navigate photographs. Dragging never simulates vehicle rotation.
- Zoom ranges from 100% to 250%, using buttons or +/− keys. Press 0 to reset. Dragging a zoomed image pans within bounds.
- A native `<dialog>` fills the viewport, locks background scrolling, contains focus and restores focus to the opener after close/Escape. This is a fullscreen lightbox, not OS-level fullscreen; browser chrome may remain visible.
- Two image layers wait for load/decode before revealing the incoming image above the current image. The current layer stays opaque beneath the crossfade to avoid black flashes. `object-fit: contain` preserves the whole vehicle, including the wide side profile.
- GSAP transitions take 500–700 ms with very small scale changes. Exterior walkaround moves at most 10 px according to navigation direction; exterior-to-detail adds subtle focus; interior changes use restrained fades; closing images use a reveal.
- Web Animations provides a fallback if GSAP is unavailable; otherwise images switch directly. Reduced motion skips transitions and uses instant thumbnail scrolling. No autoplay.
- Navigation requests are serialized, obsolete selections are discarded before animation and the latest request takes priority. Animations do not overlap.
- Loading feedback appears after 180 ms to avoid flashing on cache hits. Image requests time out after 15 seconds. Errors retain the previous image, caption and filter and offer a retry.
- Without JavaScript, the first image remains visible and thumbnail links open the untouched original PNGs in a new tab.

### Performance

| Resources | Total bytes |
|---|---:|
| 24 untouched original PNGs | 54,079,326 |
| 24 quality-88 delivery WebPs | 5,957,038 |
| 24 WebP thumbnails, at most 360 × 240 | 372,248 |

Full-size WebPs are almost 89% smaller than the PNG sources, averaging approximately 248 KB per image. All 24 full-size images are not loaded on entry. The below-fold gallery uses lazy loading; neighboring full-size preloads start when the stage approaches the viewport within 300 px.

The JavaScript cache retains at most five image promises, in addition to two display layers and the browser-managed cache. This is not a claim that the browser holds exactly five decoded images in total. Page exit aborts listeners/pending loads and clears retained cache references; back-forward-cache navigation is preserved.

Existing HTTP image caching remains 30 days. CSS uses version `1.0.2`; the English JavaScript update uses `1.0.3`. Bump version queries when changing code and change image URLs when replacing image contents. Original PNGs remain in the repository/JAR, so total storage size is not reduced by the same percentage as gallery transfer size.

### Fixes made during verification

- The incoming image is always above the outgoing layer, including alternating forward/back navigation.
- An opaque image background prevents the previous image showing through letterboxed areas.
- Resizing and opening/closing the lightbox bring the active thumbnail into view without scrolling the whole page.
- Accessible thumbnail numbering matches the filtered counter.
- The Ferrari overview's mobile Bootstrap gutter is reduced to avoid horizontal overflow when background scrolling is locked.
- Asset versioning and preview restarts prevent testing stale JavaScript/templates.

### English-language update

At the user's request, all gallery headings, categories, 24 captions/descriptions, loading/error/retry messages, keyboard hints, accessible labels, progress announcements, no-JavaScript text, editorial notes, README references and this report are now in English. Both gallery language attributes are `lang="en"`.

IDs, asset paths, image contents, classification confidence, transition behavior and display order are unchanged. A new MVC test checks English copy and accessibility labels, and the JavaScript tests assert English category labels and progress announcements.

## 3. Executed tests

| Check | Result |
|---|---|
| Baseline before gallery integration | 14 tests passed |
| Latest `mvn -B verify`, Java 21.0.10 and Maven 3.9.16 | BUILD SUCCESS; 23 tests, no failures/errors/skips; executable JAR produced |
| Gallery Java tests | 9 passed: source coverage, metadata, ordering, immutability, 72 image URLs, markup, English copy/accessibility, isolation of seven other vehicles, routes |
| Existing MVC/service regression tests | 14 passed: homepage, collection/search/sort, detail/404, contact validation and assets |
| `node --check src/main/resources/static/js/vehicle-gallery.js` | Passed during initial implementation |
| Latest `node --test tools/vehicle-gallery.test.cjs` | 11 passed |
| Initial production JAR startup | Successful on loopback port 8080 |
| Initial browser check, Chromium 1440 × 1000 | Gallery, navigation, filters, keyboard, zoom, modal/Escape worked |
| Ten rapid next clicks from the first image | Finished at image 11 with one visible layer and synchronized caption/counter/thumbnail |
| Interior → dashboard → All | Preserved dashboard; counter changed from 2/5 to 19/24 |
| Escape after zooming in the lightbox | Closed modal, reset zoom, unlocked scrolling and restored focus |
| Initial mobile viewport 390 × 844 | Filters/lightbox/zoom worked; final root width matched viewport width at 390 |
| Initial landscape viewport 844 × 390 | Toolbar, image, caption and filmstrip remained available |
| Browser pointer drag at mobile viewport | Advanced correctly from image 11 to image 12 |
| Initial inspected browser console | No JavaScript warnings/errors recorded |
| Asset preparation | All 24 file/pixel hashes distinct; source checksums unchanged |

The 11 JavaScript tests execute production code with a small DOM/Image adapter: filtering/order, rapid navigation, slow/stale requests, failed image/retry, keyboard/zoom, touch pointer plus vertical/cancel/pan behavior, modal/focus, reduced motion/cache, missing-GSAP fallback, hidden document and cleanup. These are logic tests, not substitutes for real-browser testing.

The English update reran Maven and JavaScript tests. Earlier browser interaction results above are explicitly labeled as initial implementation checks, not claims that every interaction was repeated after translation.

Maven reports the existing Mockito/Byte Buddy dynamic Java-agent warning on Java 21; builds and tests succeed. JVM settings were not changed just to suppress it.

### Remaining verification limits

- No physical iOS/Android device, Safari or Firefox testing. Mobile validation used Chromium viewport sizes, browser pointer drag and synthetic touch-pointer logic tests.
- Slow/failing network, reduced motion and unavailable GSAP were covered with the test adapter, not real network throttling or Lighthouse.
- No dedicated FPS/GPU/heap measurements or complete NVDA/VoiceOver audit. Focus, labels and keyboard behavior were checked.
- Source-image geometry and authenticity cannot be corrected by a gallery. A verified photo set and redistribution rights are needed for a real commercial vehicle listing.
- This remains Phase 2: no database, authentication, 3D model, paint customization, door animation, engine audio or driving simulation.

## 4. Preview and IntelliJ IDEA

Gallery URL: [http://localhost:8080/cars/ferrari-sf90-stradale#ferrari-gallery](http://localhost:8080/cars/ferrari-sf90-stradale#ferrari-gallery).

1. Download the `phase-2-cinematic-showroom` branch and open its root **`pom.xml`** in IntelliJ as a Maven project. The checkout directory can have any name or location.
2. Use Java 21 for Project SDK and Maven Runner. Reload All Maven Projects if dependencies have not been imported.
3. Run the shared **APEX MOTORS** configuration (Maven Wrapper and `dev` profile). Running `com.apexmotors.ApexMotorsApplication` after Maven sync also works. Stop an existing preview first if it occupies port 8080, or use `--server.port=8081` and adjust the URL.
4. Open the Ferrari detail page and choose **Explore 24 perspectives**, or use the anchored URL above.
5. Restart the application to reload manifest/cached template changes. Hard-refresh the browser with Ctrl+F5 after updating. For template development, use `--spring.profiles.active=dev`; the manifest still loads at startup.

Command-line alternative, from the checkout directory containing `pom.xml`:

```powershell
.\mvnw.cmd verify
java -jar target/apex-motors-0.0.1-SNAPSHOT.jar
```

On macOS/Linux, use `./mvnw verify`. JDK 21 must be installed or downloaded once on each computer; Maven is provided by the wrapper. Node and Python are not required to run the application. Current setup instructions, optional test/asset-preparation commands and manifest editing instructions are in the README.

## 5. Git safety and scope

Repository root and branch were checked before changes. Changes are limited to the Ferrari gallery and its related documentation/tests. Original untracked PNGs are preserved. Unrelated untracked directories such as `long5_web-201_practice_t02/` and `tmp/` were not modified or cleaned.

No commit, push or merge was performed. No original images, branches, existing 360° viewer code or unrelated repositories were deleted.
