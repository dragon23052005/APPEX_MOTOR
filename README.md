# APEX MOTORS — Phase 1

APEX MOTORS is a server-rendered premium supercar showroom built with Java 21, Spring Boot, Spring MVC, Thymeleaf, Bootstrap 5, and GSAP. Phase 1 uses an in-memory repository: there is no database, authentication, Node.js runtime, React, or Three.js dependency.

## Included in Phase 1

- Cinematic, responsive homepage with an editorial luxury design system
- Responsive Bootstrap navigation and accessible mobile menu
- In-memory collection of eight demonstration vehicles
- Vehicle search by name or powertrain
- Manufacturer filtering
- Price and horsepower sorting
- Dynamic vehicle details at `/cars/{slug}`
- Featured vehicle selection
- About and contact pages
- Jakarta Bean Validation and Post/Redirect/Get demo enquiry flow
- Custom HTTP 404 experience
- GSAP entrance, scroll, parallax, and count animations with reduced-motion support
- A progressive-enhancement stage (`data-three-stage`) reserved for a future Three.js viewer
- Spring MVC and service integration tests

Vehicle prices and specifications are illustrative showroom data for this demonstration and are not sales quotations.

## Technology

- Java 21 LTS
- Spring Boot 3.5.7
- Spring MVC and Thymeleaf
- Maven 3.9+
- Bootstrap 5.3.8 (CDN)
- GSAP 3.13 (CDN)
- HTML5, CSS3, vanilla JavaScript

Bootstrap, GSAP, and Google Fonts use CDNs, so the polished UI requires an internet connection in the browser. The Java application itself has no external runtime service dependency.

## Architecture

```text
src/main/java/com/apexmotors/
├── controller/      MVC request handling
├── dto/             Contact form and collection query DTOs
├── model/           Domain model
├── repository/      Replaceable VehicleRepository abstraction + in-memory adapter
└── service/         Search, sorting, lookup, and demo submission logic

src/main/resources/
├── templates/       Thymeleaf pages and reusable fragments
└── static/          CSS, JavaScript, and original image assets
```

`VehicleRepository` deliberately exposes domain-oriented operations without tying callers to storage details. A future JPA adapter can implement the same interface while controllers and services remain unchanged.

## Run from the command line

Prerequisites: JDK 21 and Maven 3.9 or newer.

```powershell
java -version
mvn -version
mvn spring-boot:run
```

Open <http://localhost:8080>.

To build and run the packaged application:

```powershell
mvn clean package
java -jar target/apex-motors-0.0.1-SNAPSHOT.jar
```

Run tests only:

```powershell
mvn test
```

## Run in IntelliJ IDEA with Java 21

1. Open IntelliJ IDEA and choose **File → Open**.
2. Select this folder (the folder containing `pom.xml`) and choose **Open as Project**.
3. Allow IntelliJ to import and sync the Maven project.
4. Open **File → Project Structure → Project** and set **SDK** to a Java 21 JDK. Set **Language level** to **21**.
5. Open **Settings → Build, Execution, Deployment → Build Tools → Maven → Runner** and set **JRE** to the same Java 21 JDK.
6. Open `src/main/java/com/apexmotors/ApexMotorsApplication.java`.
7. Click the green run icon beside `main`, or create an **Application** run configuration with main class `com.apexmotors.ApexMotorsApplication`.
8. Visit <http://localhost:8080> after the console reports that Tomcat started on port 8080.

If port 8080 is occupied, change `server.port` in `src/main/resources/application.properties`.

## Primary routes

| Route | Purpose |
|---|---|
| `/` | Cinematic homepage and featured vehicles |
| `/collection` | Searchable, filterable, sortable catalog |
| `/cars/{slug}` | Dynamic vehicle details |
| `/about` | Brand philosophy |
| `/contact` | Validated demonstration enquiry |
| Any unknown route | Custom 404 page |

## Phase boundary

This implementation intentionally does not include a database, authentication, payments, admin tooling, or Three.js. These remain candidates for later phases. Contact submissions are validated and logged with a generated reference but are not persisted.

## Visual assets

The three automotive images in `src/main/resources/static/images` were generated specifically for this project with the built-in OpenAI image generation tool. They contain no intended manufacturer logos or model badges and serve as original showroom visuals.
