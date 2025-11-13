# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java/software/sebastian/mondragon/battleship/` contains the production code split into `game` (server/client orchestration, domain models, repositories, services) and `ui` (Swing windows such as `MainMenuFrame`). Entry point: `game/Main.java`.
- `src/test/java/...` mirrors the main tree; place unit tests beside the classes they cover (e.g., `game/MainTest.java`).
- Build artifacts and coverage live under `target/`; clean it before committing to keep diffs readable.

## Build, Test, and Development Commands
```bash
mvn clean verify          # compile, run all tests, and produce the JaCoCo report
mvn exec:java             # start the app; pass args like -Dexec.args="server 9090"
mvn package               # build the runnable JAR in target/battleship-*.jar
```
Run `mvn test -Dtest=ClassNameTest` when iterating on a single suite. Use `JAVA_TOOL_OPTIONS="-Djava.awt.headless=true"` for CI runs that should skip Swing rendering.

## Coding Style & Naming Conventions
- Java 17, UTF-8, 4-space indentation, and braces on the same line as declarations.
- Package names stay under `software.sebastian.mondragon.battleship`. New modules follow the existing folder pattern (`game/server`, `game/service`, etc.).
- Favor descriptive class names (`TcpServer`, `GameClientSession`) and suffix tests with `Test`.
- Use SLF4J-style formatting available in `java.util.logging` (`LOGGER.log(Level.INFO, "Mensaje {0}", value)`).

## Testing Guidelines
- JUnit Jupiter 5 is the primary test framework; AssertJ Swing is available for UI robot tests.
- Keep tests deterministic and headless-friendly. Mock sockets/threads with utility latches as seen in `MainTest`.
- Generate coverage with `mvn jacoco:report` (HTML at `target/site/jacoco/index.html`). Aim to keep coverage trending upward even though the enforcement threshold is minimal.

## Commit & Pull Request Guidelines
- Follow the existing short, imperative commit style (`fix pom`, `tcp client`). Keep subject lines ≤72 characters and include scope when it adds clarity.
- Every PR should describe the change, outline test evidence (`mvn clean verify` output), and link the relevant issue or TODO item. Add screenshots or GIFs when UI elements change.

## Security & Configuration Tips
- The TCP server defaults to port 9090; override via `Main` args instead of editing constants.
- Never commit secrets or OS-specific config files. Keep `.env`-like values local and document needed variables inside the PR body.
