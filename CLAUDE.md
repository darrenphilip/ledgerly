# CLAUDE.md

See [AGENTS.md](AGENTS.md) for the full operating manual: stack, commands, code layout,
non-negotiable rules, definition of done, and delivery/git conventions.

Quick summary:

- Kotlin + Jetpack Compose (Material 3), minSdk 26 / targetSdk 35, JDK 17.
- Money is `Long` cents; finance math is pure (`util/BudgetMath.kt`, `util/Money.kt`) and unit-tested.
- DB/file I/O on `Dispatchers.IO`; UI state via `StateFlow` in `AppViewModel`.
- Screens are dumb: plain data in, lambdas out; tokens only, no hard-coded colors.
- Done = tests green, debug + release build, exercised in a running app, light **and** dark
  checked, README updated. Report anything you could not verify.
- Never commit APKs, keystores, or `local.properties`.

```bash
./gradlew :app:testDebugUnitTest && ./gradlew :app:assembleDebug
```

For the reusable, project-agnostic version of this style, see
[docs/APP_BUILD_STYLE.md](docs/APP_BUILD_STYLE.md).
