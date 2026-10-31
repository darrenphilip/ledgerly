# AGENTS.md — how to work in this repo

This file is the operating manual for any AI coding agent (Claude Code, Codex, Cursor,
Copilot, Gemini, …) working on Ledgerly. Read it before changing code. It is also a
description of the *style* this project was built in — see
[`docs/APP_BUILD_STYLE.md`](docs/APP_BUILD_STYLE.md) for the portable version you can reuse
on other projects.

## 1. What this project is

**Ledgerly** is a private, offline-first budgeting app for Android. Kotlin + Jetpack Compose
(Material 3), single activity, local SQLite, **no network permission, no accounts, no
analytics**. Money is stored as integer cents. All finance math is pure and unit-tested.

Target: **minSdk 26, targetSdk/compileSdk 35, JDK 17**.

## 2. Commands

```bash
./gradlew :app:testDebugUnitTest    # 21 unit tests — must stay green
./gradlew :app:assembleDebug        # debug APK  (com.ledgerly.app.debug)
./gradlew :app:assembleRelease      # release APK (signed if keys exist, else unsigned)
./gradlew lintVitalRelease          # runs automatically as part of assembleRelease
```

`local.properties` needs `sdk.dir=…`. Release signing comes from `local.properties` or env
vars (`LEDGERLY_KEYSTORE`, `LEDGERLY_KEYSTORE_PASSWORD`, `LEDGERLY_KEY_ALIAS`,
`LEDGERLY_KEY_PASSWORD`). **Never commit keys, `local.properties`, or APKs.**

## 3. Code layout

```
app/src/main/java/com/ledgerly/app/
  MainActivity.kt          edge-to-edge entry point, nothing else
  data/                    models, SQLiteOpenHelper, repository, sample data
  ui/App.kt                theme + tabs + FAB + sheets (app shell)
  ui/AppViewModel.kt       StateFlow state, coroutine writes
  ui/components/           reusable UI (cards, charts, rows)
  ui/screens/              one file per screen
  ui/sheets/               bottom sheets + dialogs
  ui/theme/                color scheme, typography, shapes, custom tokens
  util/                    Money, BudgetMath, DateFormats, CsvExporter
app/src/test/java/…        JVM unit tests for pure logic
```

## 4. Non-negotiable rules

**Money & data**
- Amounts are `Long` **cents**. Never `Float`/`Double` for money.
- Parsing/formatting lives in `util/Money.kt`; formatting is currency-aware
  (including zero-decimal currencies like JPY).
- All aggregation/insight logic is a **pure function** in `util/BudgetMath.kt`.
  If you add a stat, add a unit test for it.
- Persistence stays on-device. Adding `INTERNET` or any new permission is a design
  decision that must be called out explicitly.

**Threading**
- DB and file I/O on `Dispatchers.IO`; UI state via `StateFlow` from `AppViewModel`.
- No blocking calls on the main thread. No `runBlocking` in app code.

**Compose**
- One screen = one file, `@Composable fun XScreen(...)` taking plain data + lambdas.
  Screens never touch the repository directly.
- Use design tokens: `MaterialTheme.colorScheme`, `MaterialTheme.typography`,
  `MaterialTheme.shapes`, plus `MaterialTheme.ledgerColors` for semantic colors
  (`success`, `warning`, `income`, `heroStart`, …). No hard-coded hex in screens.
- Every list has an empty state; every destructive action has undo or confirmation.
- Every interactive element has a `contentDescription` or a text label.
- Support light **and** dark theme; check both before calling it done.

**Taste**
- Generous spacing (16–24dp rhythm), 20–28dp corner radii, one accent gradient per screen.
- Numbers are the hero: big, bold, tabular-feeling. Labels are small and muted.
- Motion is subtle: 200–700ms fades/animations, nothing bouncy.
- Copy is plain and human ("Left to spend", "1 day left" — handle plurals).

## 5. Definition of done

A change is done when **all** of these are true:

1. `:app:testDebugUnitTest` passes (add tests for new pure logic).
2. `:app:assembleDebug` and `:app:assembleRelease` succeed with no new warnings.
3. It was exercised in a running app (emulator/device) when the environment allows it.
   If you could not run it, say so explicitly and list what remains unverified.
4. Light and dark theme both look right; empty states still make sense.
5. `README.md` is updated if commands, features, or setup changed.

## 6. Git & delivery

- Branch: `main`. Commit messages: imperative summary line + short body of *why*.
- Keep the repo lean: build outputs (`app/build/`, `dist/`), keystores and
  `local.properties` are git-ignored. Never force-push `main`.
- Releases: tag `vX.Y.Z`, build a signed APK, attach it to a GitHub Release with
  install instructions and the signing certificate SHA-256.
- CI (`.github/workflows/android.yml`) must stay green: tests + both APKs on every push.

## 7. Communication style

- Be concise. Lead with what changed and where; no filler.
- State decisions and trade-offs in one or two lines rather than asking permission for
  reversible steps.
- Report verification honestly — show the command output / screenshot, or say what wasn't
  checked.
- Flag anything that would surprise the owner later (new permission, data migration,
  breaking schema change).
