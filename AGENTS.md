# Agent guide

## Project
- Kotlin Multiplatform / Compose Multiplatform visualizer for the external `io.github.cponfick:komp-geom` library (version in `gradle/libs.versions.toml`). One Gradle module: `:composeApp`; targets JVM desktop and browser Wasm JS. No Android target.
- Shared code is under `composeApp/src/commonMain/kotlin/io/github/cponfick/`; platform implementations and entry points are under `jvmMain` and `wasmJsMain`. Browser resources live in `composeApp/src/wasmJsMain/resources`.

## Where to change things
- `App.kt` wires the canvas and controls together and starts the transformation loop.
- `state/CanvasState.kt` owns observable points, selection, coordinate transforms, and algorithm results; it also defines `GeometricAlgorithm` and the `AlgorithmResult` variants. Points use integer IDs and geometry uses `Vec2` from komp-geom.
- `components/BaseCanvas.kt` draws the grid, points, labels, and results. `components/CanvasPointerHandling.kt` translates pointer/scroll/resize events into state operations. `components/AlgorithmPanel.kt` provides selection, execution, clearing, and label controls.
- `algorithms/` contains adapters around komp-geom algorithms (currently two closest-pair variants and Quick Hull). To add one, implement `GeometricAlgorithm`, return a drawable `AlgorithmResult`, and register it in `AlgorithmRegistry.kt`.
- Platform-specific `actual` implementations provide `scrollFactor`, `getPlatform()`, and `formatDouble`; update both JVM and Wasm implementations when changing their `expect` declarations.

## Build and check
- Desktop run: `./gradlew jvmRun -DmainClass=io.github.cponfick.MainKt --quiet`
- Browser dev run: `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`
- Compile checks: `./gradlew :composeApp:compileKotlinJvm :composeApp:compileKotlinWasmJs`
- No test source sets or tests are currently checked in. Add tests in the appropriate source set for new logic and run the relevant Gradle test task when available. Build dependencies are resolved through Maven Central/Google; the Gradle wrapper may download a distribution.

## Conventions and behavior to preserve
- Keep platform-neutral UI/algorithm logic in `commonMain`; use `expect`/`actual` only for platform-specific behavior. Follow existing Kotlin style (two-space indentation; `kotlin.code.style=official`).
- Geometry is stored in world coordinates. `CanvasState` maintains `cordToScreen`/`screenToCord` and applies panning/zoom via `tempCordToScreen`; use the correct transform for input versus rendering. The Y axis is inverted in the initial screen transform.
- Algorithms run on **selected** points, not all points; the UI enables Run only after the adapter's minimum count is selected. Results are stored by algorithm name and rendered from `AlgorithmResult`. Keep result rendering in sync with new result types.
- Consult `README.md` for user-facing run instructions and `gradle/libs.versions.toml` for dependency/plugin versions. Avoid editing generated build output or `kotlin-js-store/wasm/yarn.lock` unless dependency changes require it.
