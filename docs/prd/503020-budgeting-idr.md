# Ledgerly 50:30:20 rule + Indonesian Rupiah — PRD

## Summary

Add two capabilities to Ledgerly 1.0.0, an offline-first Android budgeting app: (1) an opt-in 50/30/20 budgeting mode that splits a monthly income into needs/wants/savings targets with cent-exact math, a plan view, and a savings category; (2) first-class Indonesian Rupiah support — currency selection, `Rp 1.234.567` formatting, rupiah-aware amount entry, and `rb`/`jt` compact labels. Existing simple-budget behavior and all existing currency formatting remain untouched.

## Problem

1. **No 50/30/20 support.** Ledgerly 1.0.0 supports only free-form per-category monthly limits plus one overall monthly budget. Users following the 50/30/20 rule must compute their three bucket targets by hand and have no per-bucket on-track view.
2. **No IDR and wrong formatting assumptions.** The currency list is a fixed 18 codes with no IDR. Money entry and formatting assume a two-decimal currency with `1,234.56` grouping (`Money.format` uses `NumberFormat.getCurrencyInstance(Locale.US)`; `Money.compact` produces `$1.2k` labels). Indonesian users write `Rp 1.234.567`, enter amounts with no minor unit, and get no help with large numbers. The entry of `50000` risks being stored as Rp 500 if treated as cents.

## Goals

- 50/30/20 mode: income in, three cent-exact targets out, per-bucket plan view with progress and a plain-language verdict.
- IDR as a selectable currency with correct formatting, entry parsing, and compact labels.
- Savings made measurable by recording savings as expense transactions in a new savings category, so income − expenses stays true.
- Zero regression: existing simple-budget mode, USD/EUR/JPY formatting, and the 21 existing unit tests stay intact.

## Non-goals

- No savings goals/accounts, transfers, or multiple wallets.
- No cloud sync, accounts, analytics, or new permissions.
- No locale-wide redesign: UI copy stays English; only money formatting becomes currency-aware.
- No rework of the existing simple budget mode.

## Users and use cases

Single user, the phone owner.

- **Existing user on simple budget:** has data in local SQLite and SharedPreferences; behavior must not change unless they opt in to 50:30:20.
- **Indonesian user, new install:** goes through two-step onboarding (name, currency, optional monthly budget, optional sample data); wants IDR by default and to enter `50000` / `50.000` / `50,000` interchangeably.
- **Rule follower:** sets monthly income in Settings, logs savings into a savings category, checks the plan card on Budgets to see remaining or over per bucket.

## User stories

- As an Indonesian user installing Ledgerly, I want IDR selected by default so I never convert from USD.
- As a user typing rupiah amounts, I want `50000`, `50.000`, and `50,000` to all mean Rp 50.000, with a live preview of what will be saved.
- As a 50/30/20 follower, I want to enter a monthly income and see needs/wants/savings targets that sum exactly back to my income.
- As a rule-mode user, I want per-bucket target, spent, remaining/over, and a verdict like "Rp 120.000 over on wants".
- As a saver, I want to log savings as an expense in a savings category so the 20% bucket is measurable.
- As a chart reader using IDR, I want `rb`/`jt` labels and no `$` leaking into non-USD formats.
- As a long-time simple-budget user, I want identical behavior if I never enable the rule.

## Functional requirements

**Currency support**

- **FR1.** `Currencies.all` grows from 18 to 19 codes by adding `IDR`; IDR is selectable in the onboarding currency step and in Settings.
- **FR2.** A fresh install (no stored currency) defaults to IDR; an install with a stored `AppSettings.currency` value keeps it unchanged.
- **FR3.** `Money.format(cents, "IDR")` renders `Rp 1.234.567` style: dot grouping, comma as the decimal separator position, zero minor-unit digits. Test: `format(5_000_000, "IDR") == "Rp 50.000"`.
- **FR4.** `Money.format` output for USD, EUR, JPY, and all other pre-existing currencies is byte-identical to 1.0.0.
- **FR5.** `parseToCents` understands IDR conventions: `50000`, `50.000`, and `50,000` each parse to the value that formats as Rp 50.000 (stored as 5,000,000 cents). The amount-entry field shows a live formatted preview of the value that will be saved.
- **FR6.** `Money.compact` is currency-aware: for IDR it uses `rb`/`jt` suffixes; the `$` symbol never appears for non-USD currencies.

**50/30/20 mode**

- **FR7.** Settings gains a "Budget style: Simple / 50:30:20" row and a monthly income field; both persist in the existing SharedPreferences keyspace. Default style is Simple; the mode is opt-in per install.
- **FR8.** `util/BudgetMath.kt` gains a pure split function: given `incomeCents`, it returns needs/wants/savings targets at 50/30/20 where the three targets sum exactly to `incomeCents` and each is within one cent of its exact share.
- **FR9.** Every expense category (the existing 12 plus the new savings category) is assigned to exactly one bucket: needs, wants, or savings. The savings category is a new entry in `Categories` with key, label, emoji, and color, and appears in expense category pickers like other expense categories. Assignment is fixed defaults; no per-category remapping UI in this version.
- **FR10.** Bucket "spent" equals the sum of current-month expense transactions in that bucket's categories; savings bucket spent equals transactions in the savings category.
- **FR11.** The Budgets tab shows a plan card above the per-category list with three bucket rows, each showing target, spent, remaining or over, and progress, plus a per-bucket plain-language verdict ("On track", "Rp 120.000 over on wants") formatted in the active currency.
- **FR12.** Savings are recorded as ordinary expense transactions in the savings category; no new transaction type, and income − expenses remains true.
- **FR13.** With style = Simple, the app behaves identically to 1.0.0: overall budget card, per-category budget cards, Home hero, alerts, and all aggregation outputs unchanged.
- **FR14.** The Home hero continues to show money left for the month; in rule mode, when income is set, left-to-spend derives from income minus the month's expenses (including savings-as-expense). When no income is set, rule-mode surfaces show an explanatory empty state; with no spending yet, plan rows show zeros, not errors.

## Non-functional requirements

- **NFR1.** Money stays `Long` cents everywhere; all new math is pure functions in `util/BudgetMath.kt` covered by JVM unit tests.
- **NFR2.** No new runtime dependencies, no new permissions, no network code; the app remains fully offline.
- **NFR3.** No database schema migration; new state (budget style, monthly income) lives in the existing SharedPreferences keyspace.
- **NFR4.** Light and dark themes both correct; all colors come from theme tokens — no hard-coded hex.
- **NFR5.** Stack unchanged: Kotlin + Jetpack Compose (Material 3), minSdk 26, targetSdk 35, JDK 17.
- **NFR6.** The 21 existing JVM unit tests (`MoneyTest`, `BudgetMathTest`) pass unmodified.
- **NFR7.** Definition of done per AGENTS.md: unit tests, debug + release APK builds, exercised on an emulator, light and dark theme checked, README updated.

## Scope

**MVP**

- **M1.** IDR currency support and defaults (FR1–FR2).
- **M2.** IDR formatting, parsing, entry preview, and compact labels (FR3–FR6).
- **M3.** Budget style setting and monthly income field (FR7).
- **M4.** Pure split and bucket aggregation functions with unit tests (FR8, FR10).
- **M5.** Savings category and savings-as-expense transactions (FR9, FR12).
- **M6.** Plan card on Budgets, Home hero behavior, empty states, and untouched Simple mode (FR11, FR13, FR14).

**Later**

- **L1.** Per-category bucket overrides (e.g. move `travel` from wants to needs), stored per category key.
- **L2.** "Apply plan to category budgets": seed per-category limits from the 50/30/20 split.
- **L3.** Copy-plan-to-clipboard or a shareable plan summary.
- **L4.** Per-bucket pace warnings on Home (e.g. wants spent faster than days elapsed), using `daysLeftInMonth`.

## UX notes

- Plan card lives on the Budgets tab above the per-category list: three bucket rows, one accent color each (from theme tokens), numbers as the hero, labels small and muted. Keep the card compact to avoid crowding the existing per-category budgets.
- Enabling the rule is a Settings row ("Budget style: Simple / 50:30:20") plus a monthly income field. The Home hero keeps showing money left for the month.
- Empty states: no income set → explain what to enter; no spending yet → zeros, not errors. A bucket the user never funds (savings) will look permanently behind — copy must be honest about this rather than implying an error.
- Amount entry shows a live formatted preview so rupiah users trust what will be saved.
- Compact chart labels: `rb`/`jt` for rupiah; no `$` symbol in non-USD contexts.

## Technical considerations

- **Cents vs. zero-decimal currency:** storage stays integer cents; for IDR the major unit is cents/100, so entry must interpret typed digits as major units (`50000` → 5,000,000 cents), never as cents.
- **`Money.format`** currently uses `Locale.US` `NumberFormat` with the currency's default fraction digits. The IDR path (dot grouping, comma decimal, zero decimals) must be added without changing output for existing currencies — an IDR-specific branch is safer than a general locale rework, since FR4 requires byte-identical existing output.
- **`Money.compact`** currently hard-codes `$1.2k` style; it must become currency-aware per FR6.
- **Rounding:** the split function must be cent-exact and sum back to income (e.g. largest-remainder style); pinned by unit tests.
- **Persistence:** new SharedPreferences keys for budget style and monthly income cents; the `budgets` table (category → limit map) is untouched, so no schema migration. The split needs no new table.
- **Categories:** adding the savings category extends the fixed list in `data/Models.kt`; transactions continue to reference category keys, so existing tables and screens pick it up.
- **Composition:** plan-card state derives from existing `BudgetMath` aggregation plus the new pure functions; `ui/App.kt` tab wiring is unchanged apart from the new card.

## Risks

- **Cents-based storage vs. zero-decimal currency:** entering 50000 must not become Rp 500. Mitigation: FR5 parsing tests pinning `50000`/`50.000`/`50,000` → Rp 50.000.
- **Re-install surprise:** changing the new-install default to IDR can surprise users who re-install (they previously got USD). Mitigation: currency remains an explicit onboarding step; existing installs keep their stored value.
- **Unfunded savings bucket looks permanently behind:** honest empty/behind copy rather than alarm styling.
- **Budgets screen crowding:** plan card adds surface above per-category budgets. Mitigation: compact rows, hero numbers.
- **Format regression:** touching `Money.format`/`compact` risks changing USD/EUR/JPY output and breaking the 21 tests. Mitigation: golden tests on existing outputs; pure functions; Simple mode untouched.

## Success metrics

- All 21 existing unit tests pass unmodified, plus new `MoneyTest`/`BudgetMathTest` cases for IDR parsing/formatting and the cent-exact split.
- Golden assertions: `format(_, "IDR")` outputs match `Rp 1.234.567` / `Rp 50.000`; USD/EUR/JPY outputs byte-identical to 1.0.0.
- Emulator exercise in light and dark themes, debug + release APKs, and README update per the AGENTS.md definition of done.
- Adoption of the mode or IDR cannot be measured: analytics is a non-goal and the app has no network access, so success is verified by tests and manual checks only.

## Open questions

1. Should the income baseline be a number typed in Settings, the sum of income transactions logged this month, or settings with a fallback to logged income?
2. Should enabling 50:30:20 replace the existing overall monthly budget figure, or coexist with it (affects FR14 hero computation and the Budgets overall card)?
3. Do we need per-category bucket overrides in the first version, or is the fixed default assignment sufficient (currently MVP: fixed)?
4. Beyond the three specified forms, how should IDR entry treat typed decimals (e.g. `1,5` with comma as decimal separator)?
5. Should onboarding expose the budget style and income setup, or only the currency (current plan: Settings only)?
6. What default per-category limit, if any, does the new savings category get in Simple mode, and does it appear in the per-category budget cards?
7. Which specific theme tokens map to the three bucket accent colors?
8. Given the no-analytics non-goal, is any local-only signal wanted to gauge 50:30:20 or IDR adoption, or do we accept tests/DoD as the only success evidence?
