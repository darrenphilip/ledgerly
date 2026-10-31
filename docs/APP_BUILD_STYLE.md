# Ledgerly — app build style

A portable description of *how* Ledgerly was built. Use it two ways:

1. **Repo agents** — `AGENTS.md` (repo root) is the operative version for this codebase.
   Point any coding agent at it.
2. **Any other AI, any other project** — paste the prompt in
   [§2](#2-copy-paste-prompt) into a fresh chat and replace the bracketed parts.

---

## 1. The style in one paragraph

Own the whole thing, end to end. Pick a native stack that fits the platform, set up the
toolchain yourself, and build a small app that feels finished rather than a demo: a real
design system, real empty states, real error paths, local persistence, no accounts. Keep the
money/domain logic pure and unit-tested, keep the UI dumb. Verify by *running* it — build it,
install it, look at it, fix what's wrong — and say honestly what you couldn't verify. Ship
artifacts (signed APK, screenshots, README) and publish to a private repo with CI. Never
commit secrets or build output. Be concise in prose, exhaustive in verification.

## 2. Copy-paste prompt

```text
Build me a [PLATFORM: Android / iOS / web / desktop] app for [IDEA].

How I want you to work:

SCOPE & TASTE
- Treat this as a shippable v1, not a prototype. Small surface, finished feel.
- Improvise features that make the core experience smoother (empty states, undo,
  onboarding, sensible defaults, keyboard/gesture behaviour) — but keep the scope tight
  enough to finish in one session.
- Native stack that fits the platform, current stable versions, no heavy frameworks
  unless they earn their place.

DESIGN
- Define a small design system first: color tokens (light + dark), type scale, spacing
  rhythm, corner radii. Then build screens only from tokens — no ad-hoc hex values.
- One accent gradient per app; numbers are the visual hero; labels are small and muted.
- Every list has an empty state. Every destructive action has undo or a confirmation.
- Support light and dark mode from day one. Handle plurals and long text properly.
- Subtle motion only (200–700 ms).

DATA & CORRECTNESS
- Money = integer minor units (cents). Never floats.
- Domain/statistics logic = pure functions in their own files, with unit tests.
- Persist locally by default. No account, no cloud, no analytics, no tracking.
  If a feature needs a permission or network access, ask me before adding it.
- Offline-first. The app must be fully usable in airplane mode.

VERIFICATION (this is the important part)
- Actually build it and run it. If an emulator/simulator/device is available, install the
  app, click through every screen, and look at screenshots — then fix what looks wrong
  (spacing, truncation, contrast, grammar, empty states).
- Run the test suite. Add tests for any non-trivial logic you write.
- Tell me exactly what you verified and what you could not. Do not claim it works if you
  didn't run it.

DELIVERY
- Produce a signed/installable build artifact and tell me where it is and how to install it.
- README with: one-line pitch, screenshots, feature list, install steps, build steps,
  architecture sketch, test instructions.
- Keep secrets (keystores, signing passwords, local.properties) and build outputs out of
  git. Add a .gitignore that enforces this.
- Add CI that runs tests + builds the artifact on every push.
- Publish to a PRIVATE repo and attach the build to a versioned release.

COMMUNICATION
- Be concise. Lead with what you did and where the files are.
- State decisions and trade-offs in a line or two; don't ask permission for reversible steps.
- Flag anything surprising: new permissions, data migrations, breaking changes.
```

## 3. The habits that made the difference

| Habit | Why it matters |
|---|---|
| Set up the toolchain before writing app code | You find out immediately whether you can verify anything at all. |
| Design tokens before screens | Prevents 40 slightly-different greys and makes dark mode nearly free. |
| Pure `BudgetMath`/`Money` + tests | The UI can be wrong-looking; the numbers must not be wrong. |
| Build → install → screenshot loop | Catches truncation, contrast, and grammar bugs no test will. |
| Empty, warning, and over-limit states | The difference between a demo and something you'd use. |
| Undo instead of "are you sure?" | Faster and friendlier for frequent actions (deleting a transaction). |
| `dist/` + README + CI | The work is reproducible by someone else, including future you. |
| Keep secrets out of git from commit #1 | Rewriting history later is annoying and easy to get wrong. |
| Say what wasn't verified | Trust is built by accurate reporting, not confident claims. |

## 4. Stack used here (reference, not a requirement)

- Kotlin 2.0, Jetpack Compose + Material 3, Compose BOM 2024.09
- Single `ComponentActivity`, no navigation library — tab state + `Crossfade`
- `SQLiteOpenHelper` (no Room) for a two-table schema; `SharedPreferences` for settings
- `StateFlow` + `AndroidViewModel`; writes on `Dispatchers.IO`
- AGP 8.6, Gradle 8.9, JDK 17, minSdk 26 / targetSdk 35
- 21 JVM unit tests (JUnit 4), no instrumentation tests needed for the core logic
- GitHub Actions: tests + debug/release APKs; manual signed releases

## 5. Applying this to a different app

Swap the domain, keep the shape:

1. Write the 5–8 pure functions that define your domain (the "math" of the app) and their
   tests first. For a habit tracker that's streaks and completion rates; for a recipe app
   it's scaling and shopping-list merging.
2. Define tokens (colors/type/spacing) before the first screen.
3. Build the shell: navigation, theme switching, persistence, empty states.
4. Build the core loop end to end (create → view → edit → delete → undo).
5. Add the one "delight" screen (insights, stats, summary).
6. Run it, screenshot it, fix the ugly parts.
7. Package, document, publish.
