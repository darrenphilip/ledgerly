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
./gradlew :app:testDebugUnitTest    # 107 tests (JVM + Robolectric) — must stay green
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
  MainActivity.kt          entry point: app-lock state, shortcut intent, edge-to-edge
  data/Migrations.kt       forward-only schema steps; never drop user tables
  data/                    models, SQLiteOpenHelper, repository, sample data
  ui/App.kt                theme + tabs + FAB + sheets (app shell)
  ui/AppViewModel.kt       StateFlow state, coroutine writes
  ui/components/           reusable UI (cards, charts, rows)
  ui/screens/              one file per screen
  ui/sheets/               bottom sheets + dialogs
  ui/theme/                color scheme, typography, shapes, custom tokens
  util/                    Money, BudgetMath, Cycles, RecurringMath, BackupCodec, Json, CSV
  data/Reminders.kt        notification channels + texts
  data/CaptureParser.kt    pure: reads an amount out of a bank notification (unit tested)
app/src/test/java/…        JVM unit tests for pure logic; MigrationTest uses Robolectric
```

## 4. Non-negotiable rules

**Money & data**
- Amounts are `Long` **cents**. Never `Float`/`Double` for money.
- Category buckets are data, not UI logic: a category says whether it is a need, want or
  saving, and `BudgetMath.bucketSpend` reads that. Never hard-code a category→bucket list in a
  screen, and never ask the user to bucket individual transactions.
- Parsing/formatting lives in `util/Money.kt`; formatting is currency-aware
  (including zero-decimal currencies like JPY and whole-unit currencies like IDR).
  `parseToCents` takes the currency code, because rupiah grouping (`50.000`) and
  decimal grouping (`1,234.56`) mean different things.
- The 50/30/20 split is `BudgetMath.splitIncome`; bucket aggregation is
  `BudgetMath.bucketSpend` / `BudgetMath.plan`. Category→bucket mapping is data
  (`Category.bucket`), not a hard-coded list in the UI.
- All aggregation/insight logic is a **pure function** in `util/BudgetMath.kt`.
  If you add a stat, add a unit test for it.
- Persistence stays on-device. The app holds four permissions, all explained in the README:
  `INTERNET` and `REQUEST_INSTALL_PACKAGES` for updates, `POST_NOTIFICATIONS` for reminders, and
  notification access (a system switch) for payment detection. Any further permission is a
  design decision that must be called out explicitly. Keep the network surface to
  `data/UpdateClient.kt` — one GET for the release feed plus the APK download. Don't add
  analytics or identifiers.
- Reading other apps' notifications is the most sensitive thing this app does. Keep it off by
  default, keep the parsing in `CaptureParser` (pure, tested), never write a transaction
  automatically, and never send the text anywhere.
- Sheets and dialogs must let back close the keyboard first: use `ImeBackHandler` and set
  `ModalBottomSheetProperties(shouldDismissOnBackPress = false)` so there is one handler, not two.
- Schema changes go through `data/Migrations.kt`: bump `Migrations.VERSION`, add a step,
  and extend `MigrationTest` so an upgrade from the previous version keeps every row.
  `LedgerDatabase.onUpgrade` must never drop a table that holds user data.
- Android auto-backup stays disabled (`allowBackup="false"`); the user's own backup file is
  the export path. Don't turn it back on without updating the README's privacy section.
- Demo data is **debug-only**. Everything in `data/SampleData.kt` is reachable only when
  `SampleData.available` (`BuildConfig.DEBUG`) is true. Release builds must start empty:
  no seeded transactions, no seeded budgets, no prefilled budget field. If you add a new
  demo-data entry point, gate it on the same flag and hide its UI.

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
- Releases: bump the version, write `docs/release-notes/vX.Y.Z.md`, tag `vX.Y.Z` and push the
  tag — CI builds the signed APK and publishes the GitHub Release. Attach the signing
  certificate SHA-256 to the notes. Never rotate the release keystore between versions.
- CI (`.github/workflows/android.yml`) must stay green: tests + both APKs on every push.

## 7. Communication style

- Be concise. Lead with what changed and where; no filler.
- State decisions and trade-offs in one or two lines rather than asking permission for
  reversible steps.
- Report verification honestly — show the command output / screenshot, or say what wasn't
  checked.
- Flag anything that would surprise the owner later (new permission, data migration,
  breaking schema change).
