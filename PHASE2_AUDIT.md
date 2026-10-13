# APEX MOTORS Phase 2 Audit

- Audit date: 2026-10-13
- Branch: `phase-2-cinematic-showroom`
- Audited baseline: `08eaf13`
- Scope: Phase 2 cinematic showroom and image-sequence viewer only
- Phase 3 work: not started

## Executive summary

The cinematic showroom, Spring MVC collection features, responsive presentation, GSAP animation system, per-vehicle imagery, and honest static fallback are implemented and working. Phase 1 routes and the Controller -> Service -> Repository -> Model architecture remain intact.

The reusable image-sequence viewer is implemented in code, but the repository contains no real 360-degree image sequences. All eight vehicles therefore correctly have `viewerEnabled=false`; the application does not expose fake rotation controls or duplicate a static image into artificial frames. The active 360-degree experience and its drag, touch, autoplay, zoom, reset, and fullscreen controls cannot be considered end-to-end complete until a licensed, consistent multi-angle sequence is supplied.

## Requirement audit

| Area | Status | Evidence and result |
|---|---|---|
| Preserve Phase 1 and Java architecture | Complete | Spring MVC controllers, services, repository abstraction, models, DTOs, Thymeleaf, contact validation, and custom 404 remain operational. No database or authentication was added. |
| Remove obsolete real-time 3D | Complete | No Three.js, WebGL viewer, GLB/GLTF asset, or 3D runtime dependency exists in application code. |
| Cinematic homepage | Complete | Full-viewport hero, editorial imagery, premium dark palette, restrained red accents, animated type, image reveals, parallax, counters, page transition, and featured vehicle cards are present. |
| GSAP and ScrollTrigger | Complete | GSAP 3.13.0 and ScrollTrigger are loaded from jsDelivr. Headless Edge reported both globals available and 21 active ScrollTrigger instances. Scroll-dependent code is skipped safely if ScrollTrigger fails to load. |
| Reduced motion | Complete in code | JavaScript bypasses motion/autoplay when `prefers-reduced-motion: reduce` matches; CSS disables smooth scrolling, transitions, animations, and `will-change`. A final browser preference-emulation pass was not completed, so this remains source-verified rather than device-verified. |
| Premium showroom | Complete | Editorial cards, hover states, featured ordering, page transitions, responsive grid, specifications, and dynamic details pages are implemented. |
| Search | Complete | Server-side GET search matches display name and powertrain case-insensitively using `Locale.ROOT`. |
| Manufacturer filter | Complete | Server-side case-insensitive manufacturer filtering is implemented and manufacturers are generated from repository data. |
| Sorting | Complete | Featured, price ascending/descending, and horsepower ascending/descending are implemented. URL-friendly values such as `price-desc` are accepted. |
| Viewer Java metadata | Complete | `VehicleImageSequence` provides `viewerEnabled`, `frameCount`, `framePathPattern`, and `posterImage`, with structural validation for enabled sequences. |
| Reusable image-sequence viewer engine | Partial | Loading progress, frame cache, nearby preloading, pointer drag, touch/pinch, keyboard input, autoplay/pause, zoom, reset, fullscreen, visibility pause, error handling, retry, cleanup, and accessibility announcements are implemented. No real sequence exists for runtime validation. |
| Actual 360-degree assets | Missing | Filesystem audit found 0 `360` directories and 0 `frame-NNN` files. This is intentionally not fabricated. |
| Active drag/touch/autoplay/zoom/fullscreen experience | Partial | Implemented by `image-sequence-viewer.js`, but all viewer instances are disabled because assets are absent. Fullscreen also still needs real-browser user-gesture testing on desktop, Android, and iOS. |
| Static fallback | Complete | All eight vehicles render their matching studio poster, a clear “360 imagery not available” message, and no inactive viewer toolbar. |
| Static fallback gallery | Partial | A polished hero and secondary studio image treatment exist, but there is no multi-image per-vehicle gallery asset set. |
| Responsive navigation and layouts | Complete | Breakpoints cover navigation, hero, card grid, filters, details, viewer toolbar, and contact/about layouts. Headless mobile testing confirmed the hamburger expands the menu. |
| Responsive images | Complete | All project raster assets are WebP. Eleven 960-pixel derivatives are connected through `srcset`/`sizes`; intrinsic dimensions, lazy loading, and async decoding are used where appropriate. The viewer poster intentionally keeps a single source so future frame swaps cannot be overridden by `srcset`. |
| Performance and caching | Mostly complete | Images/assets receive 30-day public cache headers; CSS/JS receive 7-day headers and query-string versions. Thymeleaf caching is enabled by default, with a separate `dev` profile. Nearby frames are loaded progressively rather than all at startup. |
| Documentation | Complete | README documents architecture, viewer metadata, honest asset status, controls, IntelliJ Java 21 setup, production defaults, and the `dev` profile. |

## Completed features

- Cinematic homepage with hero entrance, scroll reveals, parallax, staggered cards, counters, and page transitions.
- GSAP and ScrollTrigger integration with a graceful no-ScrollTrigger path.
- Search, manufacturer filtering, all required price/horsepower sorts, and featured ordering.
- Dynamic vehicle detail pages for all eight vehicles.
- A distinct slug-specific hero image for every named vehicle.
- Java image-sequence metadata and a reusable, progressively loaded viewer implementation.
- Truthful static fallback for every vehicle while no real sequences exist.
- Responsive navigation, collection, details, about, contact, and error layouts.
- Server-side contact validation and demo submission redirect.
- Custom 404 response.
- Keyboard/focus styling and accessible viewer labels/status messages.
- WebP media, responsive 960-pixel variants, intrinsic dimensions, lazy loading, and cache headers.
- Production Thymeleaf caching plus an opt-in `dev` profile.

## Partially completed features

- The 360-degree viewer engine is code-complete enough for integration, but none of its frame-based interactions can be validated end to end without a genuine sequence.
- Mobile layout and menu behavior were smoke-tested in headless Edge; physical iOS/Android touch, pinch, orientation, and fullscreen behavior were not tested.
- Static fallback uses strong cinematic imagery but not a true multi-image vehicle gallery.
- Frame caching is appropriate for the current planned 36-frame example, but memory limits should be tuned using the real frame resolution and compressed size before enabling a sequence.
- Bootstrap, GSAP, ScrollTrigger, and Google Fonts are CDN-hosted. Core content and static fallbacks remain usable if GSAP is unavailable, but the Bootstrap mobile collapse behavior and web fonts do not have local offline fallbacks.
- The repository describes the images as generated project assets, but there is no separate asset provenance/license manifest to independently verify redistribution terms.

## Missing features

- Licensed multi-angle image sequences showing the same vehicle with consistent camera, lighting, and framing.
- Any enabled production 360-degree viewer instance.
- End-to-end verification of drag rotation, touch swipe/pinch, autoplay/pause, zoom, reset, fullscreen, progress, and mid-sequence error recovery using real frames.
- Per-vehicle static gallery image sets.
- Automated browser-level viewer interaction tests.

## Bugs fixed during this audit

- Scoped the homepage GSAP timeline to the homepage hero so other pages do not animate missing targets.
- Prevented ScrollTrigger-dependent animation setup when the plugin CDN is unavailable.
- Restored the original poster image and accessible alt text after a sequence load error.
- Invalidated stale frame renders on retry and disposal.
- Added a load-generation guard so old asynchronous image callbacks cannot repopulate cache/progress after retry or navigation.
- Cancelled scheduled idle preloads during retry and disposal.
- Kept sequence resources, event listeners, timers, and observers disposable on page exit.
- Made case-insensitive search locale-stable with `Locale.ROOT`.
- Corrected the homepage marque count from 7 to 8.
- Corrected detail/about image dimensions to reduce layout shift.
- Updated stale Phase 1 contact copy to Phase 2.
- Added visible focus states for navigation, filters, buttons, and viewer controls.
- Added explicit cache policy for CSS and JavaScript and bumped static asset query versions.
- Converted 11 original PNG images (22.64 MiB) to WebP and added 11 responsive 960-pixel derivatives. The complete static directory is now 2.26 MiB, approximately 90% smaller even with CSS, JavaScript, documentation, and responsive variants included.
- Enabled Thymeleaf caching by default and added `application-dev.properties` for template development.
- Expanded service and MVC tests for both price directions, case-insensitive name/powertrain search, GSAP/ScrollTrigger tags, fallback behavior, cache headers, and all full/responsive vehicle images.

## Test results

### Toolchain

- Java: Oracle JDK 21.0.10 LTS
- Maven: 3.9.16
- Spring Boot: 3.5.7
- Browser smoke test: Microsoft Edge 155 headless via DevTools Protocol

### Automated checks

- `node --check src/main/resources/static/js/app.js`: passed.
- `node --check src/main/resources/static/js/image-sequence-viewer.js`: passed.
- `mvn test`: passed, 14 tests, 0 failures, 0 errors, 0 skipped.
- `mvn -DskipTests package`: passed.
- Packaged artifact: `target/apex-motors-0.0.1-SNAPSHOT.jar` (26,500,432 bytes).
- `git diff --check`: passed.

The JDK emitted a non-failing Mockito/Byte Buddy dynamic-agent warning during tests. It did not affect test or build status.

### Route and browser checks

- `/`, `/collection`, `/cars/rimac-nevera`, `/about`, and `/contact`: HTTP 200.
- `/does-not-exist`: HTTP 404 with the custom page.
- Shared and vehicle WebP assets: HTTP 200 with `image/webp`.
- Mobile runtime: GSAP loaded, ScrollTrigger loaded, 21 triggers registered, hero visible.
- Mobile navigation: toggle reached `aria-expanded=true`, `.show` was applied, and the expanded panel had non-zero height.
- Filtered showroom query `q=nevera&manufacturer=Rimac&sort=price-desc`: exactly one Nevera card and `PRICE_DESC` selected.
- Nevera details: `data-viewer-enabled=false`, no toolbar, static fallback present, correct 1672 x 941 poster loaded.
- Temporary non-versioned screenshots were captured under `target/` for desktop home, mobile home, filtered collection, and mobile Nevera details.

## Remaining tasks

1. Obtain and document redistribution rights for a complete same-vehicle image sequence.
2. Add the sequence as `assets/cars/<slug>/360/frame-001.webp` through the declared final frame and add a license/provenance entry.
3. Verify every declared frame before changing that vehicle from `VehicleImageSequence.unavailable(...)` to an enabled sequence.
4. Run real desktop and mobile interaction testing for drag, swipe, pinch, autoplay, pause, keyboard controls, reset, error recovery, and fullscreen.
5. Measure decoded memory usage with the real images and introduce/tune an LRU cache limit if needed.
6. Add Java-based browser automation for the enabled viewer and reduced-motion mode once real assets exist.
7. Add genuine per-vehicle gallery photographs if a multi-image static fallback is required.
8. Consider self-hosting Bootstrap, GSAP, and fonts if offline operation or CDN independence becomes a requirement.

Do not enable or advertise the 360-degree viewer until steps 1–4 are complete. No Phase 3 work, merge, or branch deletion was performed.
