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
- Limits can apply to **every month** or only to the month you're viewing, so a one-off month
  doesn't rewrite your defaults.
- **Your own categories**: create one with a name, icon and colour, and pick whether it counts
  as a **need, want or saving**. Every category is bucketed, so the 50:30:20 plan sorts your
  spending automatically — a "Gym" category marked as a need is counted against needs.
  Built-in categories can be renamed, re-bucketed or hidden without touching history.
- **Budget cycles**: if your salary doesn't land on the 1st, set the day your month starts
  (payday). Totals, budgets and the plan then run from that day to the day before the next,
  labelled like "25 Sep – 24 Oct".
- **50:30:20 mode** (opt-in in Settings): enter your monthly income and Ledgerly splits it
  into 50% needs / 30% wants / 20% savings, with a plan card showing each bucket's target,
  spending, remaining and a plain-language verdict. Savings is a category, so logging a
  transfer as a Savings expense counts toward the 20%.
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
- 13 expense categories + 4 income categories, each with its own colour and emoji.
- **Recurring entries**: rent, salary and subscriptions that come due show up in a card on
  Home; log them in one tap (or skip a cycle) and the next date advances itself.
- **App lock**: optionally require the device screen lock before the ledger opens. No
  password is stored by the app.
- **Backup and restore**: one JSON file holds transactions, budgets, recurring entries and
  settings; restore it by replacing everything or merging into what's on the device.
- **CSV import** alongside export, so an export is a usable backup — duplicates are skipped
  and unreadable rows are reported rather than silently dropped.
- **Reminders**: an evening nudge to log the day (it knows whether you already have), and an
  optional weekly summary. Both off until you turn them on, both previewable from Settings.
- **Payments from your notifications**: with your permission, Ledgerly watches bank and wallet
  notifications for amounts, then shows a card on Home — "Rp 87.500 · payment" — with category
  chips and a one-tap Add. Conservative by design: the text must contain both an amount and a
  payment word, and **nothing is written to your ledger until you confirm it**. You can also
  hand it to a category of your choosing, or dismiss it.
- **In-app updates**: Ledgerly asks the public GitHub release feed for a newer version and,
  if there is one, shows a banner on Home; one tap downloads the APK and hands it to the
  system installer.
- Light / dark / system theme, 19 currencies including **Indonesian Rupiah**, CSV export
  (share sheet), sample data (debug builds only).
- Launcher shortcut (long-press the icon → *Add expense*) that opens the amount pad directly.
- Rupiah-aware money entry: `50000`, `50.000` and `50,000` all mean Rp 50.000, with a live
  preview of the amount that will be saved. IDR is the default for new installs.
- Onboarding that sets your name, currency and monthly budget.

## Install

Prebuilt APKs are attached to each **GitHub Release** — download the APK on your phone
and open it (allow install from unknown sources), or:

```bash
adb install -r ledgerly-1.2.0.apk     # signed release build
```

Requires **Android 8.0+** (minSdk 26).

## Build

```bash
export ANDROID_HOME=/path/to/android-sdk   # SDK 35 + build-tools 35.0.0
./gradlew :app:testDebugUnitTest           # 63 unit tests (JVM + Robolectric)
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

CI signs tagged releases with a keystore held in repo secrets (`LEDGERLY_KEYSTORE_BASE64`,
`LEDGERLY_KEYSTORE_PASSWORD`, `LEDGERLY_KEY_ALIAS`, `LEDGERLY_KEY_PASSWORD`). The keystore
must not change: a new key means Android refuses to install the update over the old app.

## How this was built

This project follows a documented build style: pure, tested domain logic; a token-based
design system; local-first data; and a verify-by-running loop. Two files capture it:

- [`AGENTS.md`](AGENTS.md) — operating manual for AI coding agents in this repo
  (commands, layout, rules, definition of done). `CLAUDE.md` points to it.
- [`docs/APP_BUILD_STYLE.md`](docs/APP_BUILD_STYLE.md) — the portable playbook, including a
  copy-paste prompt you can give any AI on any project.

## Architecture

```
app/src/main/java/com/ledgerly/app/
├── MainActivity.kt              # edge-to-edge entry point, app-lock + shortcut intent
├── data/
│   ├── Models.kt                # Transaction, Budget, Recurring, Category, AppSettings
│   ├── Migrations.kt            # forward-only schema steps (v1 → v3)
│   ├── LedgerDatabase.kt        # SQLiteOpenHelper (transactions, budgets, recurring, categories)
│   ├── LedgerRepository.kt      # DB + SharedPreferences access, backup/restore
│   └── SampleData.kt            # deterministic 4-month demo dataset
├── ui/
│   ├── App.kt                   # theme, tabs, FAB, sheets, navigation
│   ├── LockScreen.kt            # device-credential gate
│   ├── AppViewModel.kt          # StateFlow state + coroutine writes
│   ├── components/              # cards, charts, transaction row
│   ├── screens/                 # Home, Activity, Budgets, Insights, Settings, Onboarding
│   ├── sheets/                  # transaction / budget / recurring editors, amount dialog
│   └── theme/                   # colour scheme, typography, shapes
└── util/
    ├── Money.kt                 # cent-based parsing/formatting, currencies
    ├── BudgetMath.kt            # pure aggregation/insight functions (unit tested)
    ├── Cycles.kt                # payday budget cycles (unit tested)
    ├── RecurringMath.kt         # due list + next-date rollover (unit tested)
    ├── BackupCodec.kt           # JSON backup encode/decode (unit tested)
    ├── Json.kt                  # tiny JSON reader/writer
    ├── DateFormats.kt
    ├── CsvExporter.kt
    └── CsvImporter.kt           # CSV parse + duplicate detection (unit tested)
```

- Money is stored as **integer cents** to avoid floating-point drift.
- Money formatting and parsing are currency-aware: `Money.format` renders `Rp 1.234.567` for
  IDR and keeps the existing `$1,234.56` output for two-decimal currencies; `Money.parseToCents`
  takes the currency so rupiah grouping is read correctly.
- All statistics are pure functions in `BudgetMath` and covered by unit tests. The 50/30/20
  split (`splitIncome`) rounds down and lets savings take the remainder, so the three parts
  always sum back to the income.
- Category→bucket mapping lives on `Category.bucket`, editable from Settings → Categories.
  Built-ins are compiled in; custom rows live in the `categories` table and override a
  built-in with the same key (which is how renaming, re-bucketing and hiding work). Lookups go
  through the `Categories` object, which is hydrated once by `LedgerRepository`.
- Budget cycles are `Cycles`: a cycle is labelled by the month it mostly covers, so a start day
  of 25 puts 25 Sep – 24 Oct in "October". Everything that groups by month takes a `startDay`
  that defaults to 1 (calendar month), so callers that don't care stay unchanged.
- Budget style (`SIMPLE` / `RULE_50_30_20`) and monthly income are stored in the existing
  SharedPreferences keyspace — no database migration.
- Schema changes go through `Migrations.kt`: every version step is applied in order inside
  `SQLiteOpenHelper`'s transaction and must keep existing rows. `MigrationTest` opens a real
  v1 database, upgrades it and asserts the ledger survives.
- Budgets carry an optional `month` (`2026-10`); an empty month is the default that applies
  everywhere, and a month row overrides it for that month only.
- State is exposed from the ViewModel as `StateFlow`; writes go to `Dispatchers.IO`.
- No third-party runtime dependencies beyond AndroidX/Compose — no ads, no analytics,
  no network permission at all. The backup file uses a small hand-rolled JSON codec (`Json.kt`)
  rather than pulling in a serialisation library.

## Continuous integration

`.github/workflows/android.yml` runs the unit tests and builds both APKs on every push and
pull request (artifacts are uploaded). Pushing a `vX.Y.Z` tag builds a signed release APK and
publishes it to GitHub Releases using `docs/release-notes/vX.Y.Z.md` as the description, so a
release only needs the tag and the notes file to exist.

## Tests

107 JVM unit tests (plus Robolectric for the database) cover money parsing/formatting
(including zero-decimal currencies like JPY and whole-unit currencies like IDR), the budgeting
math (summaries, slices, budget status, projections, trends, the 50/30/20 split and bucket
aggregation), CSV import/export round-trips, the backup codec, recurring date rollover, custom
category bucketing, payday budget cycles, notification parsing (amounts, direction words, and the
cases it must refuse), and the schema migration from v1.

```bash
./gradlew :app:testDebugUnitTest
```

## Privacy

Ledgerly holds these permissions, and nothing else:

- `INTERNET` — to ask GitHub for the latest release and download its APK.
- `REQUEST_INSTALL_PACKAGES` — to hand that APK to the system installer.
- `POST_NOTIFICATIONS` — only if you turn reminders on.
- Notification access (a system switch, not a manifest permission) — only if you turn payment
  detection on.

Payment detection is the one place Ledgerly reads another app's content, so it is worth being
precise about:

- It is **off until you enable it**, and you enable it in Android's own settings screen.
- It only looks at notification text, only to find an amount next to a payment word. The raw
  text of a match is kept (up to 160 characters) so you can see why it was suggested, and it is
  discarded as soon as you add or dismiss it. Nothing else is stored, ever.
- Nothing is sent anywhere. The parse happens on the device, and no transaction is created
  without you tapping **Add**.
- Turning the switch off stops the service reading anything.

- No analytics, no telemetry, no crash reporting, no ad SDKs.
- The only request the app makes is `GET /repos/darrenphilip/ledgerly/releases/latest`, plus
the APK download if you start one. Your transactions, budgets and settings are never sent
anywhere, and the request carries no identifier.
- Turn the check off in Settings → Updates → *Check automatically* if you'd rather it never
talked to the network.
- Android only accepts an update signed with the same key as the installed app, so a tampered
download fails to install instead of replacing your ledger.

Android auto-backup is disabled (`allowBackup="false"`), so the ledger isn't copied to Google
Drive either. The backup file you create from Settings is the only copy that leaves the app,
and it goes wherever you choose to save it.

## What I'd build next

- "Apply plan to category budgets" — seed per-category limits from the 50:30:20 split
- Per-bucket pace warnings on Home
- Home-screen widget with the "left to spend" figure
- Multiple accounts/wallets
- Receipt photo attachments
- Translating the UI (strings are still English literals in Kotlin; `strings.xml` holds only
the app name and shortcut labels)

## Demo data

The four months of sample transactions and the suggested monthly budget are **debug-only**.
`SampleData.available` is `BuildConfig.DEBUG`, and release builds ship with an empty ledger:
no sample-data switch in onboarding, no "Reset with sample data" row in Settings, no
"Load sample data" link on Home, and no prefilled budget figure. A release install starts at
zero and only ever holds what you enter.

## License

MIT — see [LICENSE](LICENSE).
