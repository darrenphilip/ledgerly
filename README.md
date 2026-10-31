# Ledgerly — Budgeting & Spending Tracker

A private, offline-first personal finance app for Android, built with Kotlin and Jetpack Compose.

Ledgerly keeps your money picture clear without accounts, cloud sync, or tracking. Everything
lives in a local SQLite database on the device.

<p align="center"><em>Log expenses in seconds · budget by category · see where your money goes</em></p>

<p align="center">
  <img src="docs/screenshots/home.png" width="190" alt="Home dashboard" />
  <img src="docs/screenshots/activity.png" width="190" alt="Activity list" />
  <img src="docs/screenshots/budgets.png" width="190" alt="Budgets" />
  <img src="docs/screenshots/insights.png" width="190" alt="Insights" />
</p>
<p align="center">
  <img src="docs/screenshots/editor.png" width="190" alt="Add transaction" />
  <img src="docs/screenshots/settings.png" width="190" alt="Settings" />
</p>

## Highlights

**Home dashboard**
- Hero card showing **left to spend** for the month, budget progress and a
  **safe-to-spend-per-day** figure.
- Month switcher to browse history, income/spending tiles, and a category donut chart.
- Budget alerts when a category is close to — or over — its limit.
- Recent transactions with one tap to edit.

**Activity**
- Search notes/categories, filter by expense/income and by category.
- Transactions grouped by day with per-day net totals.
- Swipe a row left to delete, with **Undo** in the snackbar.
- Tap to edit or delete any transaction.

**Budgets**
- Overall monthly budget with progress and remaining amount.
- Per-category monthly limits with colour-coded progress
  (healthy → warning at 80% → over).
- "Outside category budgets" summary so nothing hides.
- Add/edit/remove budgets from a bottom sheet.

**Insights**
- Daily average, top category, and month-over-month comparison.
- Six-month spending trend chart.
- Full category breakdown with shares.
- Auto-generated highlights: pace vs. budget, subscriptions, biggest expense, overspent budgets.

**Everywhere**
- Add/edit sheet with a big amount pad, emoji category grid, note, and date picker
  (Today / Yesterday / any date).
- 12 expense categories + 4 income categories, each with its own colour and emoji.
- Light / dark / system theme, 18 currencies, CSV export (share sheet), sample data.
- Onboarding that sets your name, currency and monthly budget.

## Install

Prebuilt APKs are attached to each **GitHub Release** — download the APK on your phone
and open it (allow install from unknown sources), or:

```bash
adb install -r ledgerly-1.0.0.apk     # signed release build
```

Requires **Android 8.0+** (minSdk 26).

## Build

```bash
export ANDROID_HOME=/path/to/android-sdk   # SDK 35 + build-tools 35.0.0
./gradlew :app:testDebugUnitTest           # 21 unit tests
./gradlew :app:assembleDebug               # debug APK
./gradlew :app:assembleRelease             # release APK
```

`local.properties` must point at your SDK (`sdk.dir=...`).

### Signing a release build

Signing credentials are **not** in the repo. Create a keystore and add these keys to
`local.properties` (already git-ignored), or set the same names as environment variables:

```properties
LEDGERLY_KEYSTORE=keystore/ledgerly.jks
LEDGERLY_KEYSTORE_PASSWORD=…
LEDGERLY_KEY_ALIAS=ledgerly
LEDGERLY_KEY_PASSWORD=…
```

Without those keys, `assembleRelease` still works but produces an unsigned APK.

## Architecture

```
app/src/main/java/com/ledgerly/app/
├── MainActivity.kt              # edge-to-edge entry point
├── data/
│   ├── Models.kt                # Transaction, Budget, AppSettings, categories
│   ├── LedgerDatabase.kt        # SQLiteOpenHelper (transactions + budgets)
│   ├── LedgerRepository.kt      # DB + SharedPreferences access
│   └── SampleData.kt            # deterministic 4-month demo dataset
├── ui/
│   ├── App.kt                   # theme, tabs, FAB, sheets, navigation
│   ├── AppViewModel.kt          # StateFlow state + coroutine writes
│   ├── components/              # cards, charts, transaction row
│   ├── screens/                 # Home, Activity, Budgets, Insights, Settings, Onboarding
│   ├── sheets/                  # transaction / budget editors, amount dialog
│   └── theme/                   # colour scheme, typography, shapes
└── util/
    ├── Money.kt                 # cent-based parsing/formatting, currencies
    ├── BudgetMath.kt            # pure aggregation/insight functions (unit tested)
    ├── DateFormats.kt
    └── CsvExporter.kt
```

- Money is stored as **integer cents** to avoid floating-point drift.
- All statistics are pure functions in `BudgetMath` and covered by unit tests.
- State is exposed from the ViewModel as `StateFlow`; writes go to `Dispatchers.IO`.
- No third-party runtime dependencies beyond AndroidX/Compose — no ads, no analytics,
  no network permission at all.

## Continuous integration

`.github/workflows/android.yml` runs the unit tests and builds both APKs on every push and
pull request. Pushing a `v*` tag also creates a GitHub Release with the APKs attached.

## Tests

21 JVM unit tests cover money parsing/formatting (including zero-decimal currencies like JPY)
and the budgeting math (summaries, slices, budget status, projections, trends).

```bash
./gradlew :app:testDebugUnitTest
```

## Privacy

Ledgerly requests **no permissions** — not even internet access. Your transactions never
leave the device unless you explicitly export them.

## License

MIT — see [LICENSE](LICENSE).

## What I'd build next

- Recurring transactions and upcoming-bill reminders
- Home-screen widget with the "left to spend" figure
- Multiple accounts/wallets
- Receipt photo attachments
- Backup/restore of the SQLite database
- Optional biometric lock
