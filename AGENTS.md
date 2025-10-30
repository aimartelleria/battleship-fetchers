# Repository Guidelines

## Project Structure & Module Organization
- Source lives in `src/main/java/software/sebastian/mondragon/battleship`, grouped by layer: `model/` for domain entities, `service/` for orchestration, `repo/` for persistence helpers, and `common/` for shared utilities.
- Integration and unit tests mirror that layout under `src/test/java/...`; keep new tests in the matching package.
- Maven writes build artifacts to `target/`; treat it as disposable and never commit its contents.

## Build, Test, and Development Commands
- `mvn clean compile` rebuilds classes from scratch and will surface compilation issues early.
- `mvn test` runs the JUnit 5 suite; use `mvn -Dtest=MapaTest test` to focus on a class when troubleshooting.
- `mvn verify` executes tests and generates the JaCoCo coverage report in `target/site/jacoco/index.html`.
- `mvn package` produces `target/battleship-0.1.0-SNAPSHOT.jar`; run it with `java -jar` if you need to smoke-test `Main`.

## Coding Style & Naming Conventions
- Target Java 17; stay compatible with the existing Maven compiler settings.
- Use four-space indentation, `UpperCamelCase` for classes (`Partido`, `GameService`), and `lowerCamelCase` for methods and fields.
- Keep each public type in its own file and prefer meaningful, domain-aligned names (current code uses Spanish game terminology—stay consistent).
- No formatter is wired into Maven, so rely on your IDE’s default Java formatting and keep imports tidy before sending a review.

## Testing Guidelines
- Write JUnit 5 tests in the mirrored `src/test/java/...` package and name files `*Test`.
- Cover both happy paths and boundary checks (e.g., invalid coordinates, duplicate shots) when touching gameplay logic.
- Generate coverage with `mvn verify`; while the minimum threshold is currently 0%, keep or raise existing coverage by extending affected suites.

## Commit & Pull Request Guidelines
- Follow the existing short imperative commit style (`add src/test`, `Test Berrik`); keep the first line under 72 characters and add detail in the body if needed.
- Reference related issues in the body (e.g., `Refs #123`) and describe observable behaviour changes.
- For pull requests, include: summary of changes, testing evidence (`mvn test` output), screenshots if UI behaviour changes, and call out any follow-up TODOs.
