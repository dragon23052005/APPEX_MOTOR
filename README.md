# APEX MOTORS - Phase 2 Interactive 3D Showroom

APEX MOTORS is a server-rendered premium supercar showroom built with Java 21, Spring Boot, Spring MVC, Thymeleaf, Bootstrap, GSAP, and Three.js. Phase 2 preserves every Phase 1 route and adds an optional, progressively enhanced 3D studio to vehicle detail pages.

There is still no database, authentication, React, or Node.js application runtime. Vehicle and 3D asset metadata remain in the in-memory Java repository.

## Features

### Preserved from Phase 1

- Cinematic responsive homepage and navigation
- Eight demonstration vehicles in an in-memory repository
- Search by vehicle name or powertrain
- Manufacturer filtering and price/horsepower sorting
- Dynamic vehicle details at `/cars/{slug}`
- Featured vehicles, About, Contact, server-side validation, and custom 404
- GSAP animations with reduced-motion support

### Added in Phase 2

- Three.js `0.186.1` loaded only for vehicles that declare a 3D asset
- GLB loading with percentage feedback and a retryable error state
- Mouse/touch orbit rotation, wheel/pinch zoom, and camera reset
- Responsive WebGL canvas with capped mobile pixel ratio
- Studio environment lighting, key/fill/rim lights, and soft ground shadows
- Static-image fallback when a vehicle has no model or WebGL fails
- Pause when the viewer is offscreen or the tab is hidden
- Model request de-duplication and explicit geometry/material/texture disposal
- Reduced-motion-aware controls
- Visible attribution for the representative 3D concept model

Vehicle prices, specifications, images, and the representative concept visualization are demonstration content, not sales quotations or exact digital twins.

## Technology

- Java 21 LTS
- Spring Boot 3.5.7
- Spring MVC and Thymeleaf
- Maven 3.9+
- Bootstrap 5.3.8 (CDN)
- GSAP 3.13 (CDN)
- Three.js 0.186.1 and official addons (CDN import map)
- HTML5, CSS3, and modular vanilla JavaScript

Bootstrap, GSAP, Google Fonts, and the Three.js modules use CDNs. The licensed GLB model itself is stored locally and served by Spring Boot.

## Architecture

```text
src/main/java/com/apexmotors/
|-- config/          MVC static-resource cache configuration
|-- controller/      MVC request handling
|-- dto/             Contact form and collection query DTOs
|-- model/           Vehicle and optional VehicleModel3d metadata
|-- repository/      Replaceable repository abstraction + in-memory adapter
`-- service/         Search, sorting, lookup, and demo submission logic

src/main/resources/
|-- templates/       Thymeleaf pages and reusable fragments
`-- static/
    |-- css/          Responsive visual system and viewer states
    |-- images/       Static image fallbacks
    |-- js/           Site scripts and modular Three.js viewer
    `-- models/       Local licensed GLB assets and license files
```

`VehicleRepository` remains storage-agnostic. A future JPA implementation can replace the in-memory adapter without changing the controllers or `VehicleService`.

## Optional 3D model metadata

`Vehicle` has an optional `VehicleModel3d` value. Only the Rimac Nevera demonstration entry currently declares one. Every other vehicle renders the existing static image without downloading Three.js.

To enable a model for another vehicle:

1. Place its `.glb` file and license under `src/main/resources/static/models/<asset-name>/`.
2. Add a `VehicleModel3d` value to that vehicle in `InMemoryVehicleRepository`.
3. Record the model name, creator, license, source URL, and initial yaw.
4. Confirm that redistribution and commercial/demo usage are permitted by its license.

## Run from the command line

Prerequisites: JDK 21 and Maven 3.9 or newer.

```powershell
java -version
mvn -version
mvn spring-boot:run
```

Open <http://localhost:8080>. Visit <http://localhost:8080/cars/rimac-nevera> for the interactive viewer.

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

1. Choose **File -> Open** and select the folder containing `pom.xml`.
2. Allow IntelliJ IDEA to import the Maven project.
3. In **File -> Project Structure -> Project**, select a Java 21 SDK and language level 21.
4. In **Settings -> Build, Execution, Deployment -> Build Tools -> Maven -> Runner**, select the same Java 21 JDK.
5. Open `src/main/java/com/apexmotors/ApexMotorsApplication.java`.
6. Run its `main` method and browse to <http://localhost:8080>.

If port 8080 is occupied, change `server.port` in `src/main/resources/application.properties`.

## Primary routes

| Route | Purpose |
|---|---|
| `/` | Cinematic homepage and featured vehicles |
| `/collection` | Searchable, filterable, sortable catalog |
| `/cars/rimac-nevera` | Vehicle details with interactive 3D concept viewer |
| `/cars/{other-slug}` | Vehicle details with static-image fallback |
| `/about` | Brand philosophy |
| `/contact` | Validated demonstration enquiry |
| Any unknown route | Custom 404 page |

## 3D asset license

`CarConcept.glb` comes from the official [Khronos glTF Sample Assets repository](https://github.com/KhronosGroup/glTF-Sample-Assets/tree/main/Models/CarConcept).

- Model and textures: Eric Chadwick / Darmstadt Graphics Group GmbH
- License: [Creative Commons Attribution 4.0 International](https://creativecommons.org/licenses/by/4.0/)
- Local license: `src/main/resources/static/models/car-concept/LICENSE.md`
- Local attribution: `src/main/resources/static/models/car-concept/ATTRIBUTION.md`

The viewer labels this asset as a representative concept visualization. It is not presented as an exact Rimac Nevera digital twin, and no affiliation or endorsement by Khronos is implied.

## Current limitations and phase boundary

- The Three.js library and addons require browser internet access because they are loaded from jsDelivr.
- The local GLB is approximately 9.8 MB, so its first visit can take time on slower mobile networks.
- Visual browser QA depends on WebGL/GPU availability; static fallback remains usable without it.
- Phase 2 intentionally excludes paint customization, door animation, engine audio, driving simulation, database persistence, and authentication.

## Image assets

The three automotive images in `src/main/resources/static/images` were generated specifically for this project with the built-in OpenAI image generation tool. They contain no intended manufacturer logos or model badges and serve as original showroom fallbacks.
