# Versioning

This app uses [Semantic Versioning](https://semver.org): **MAJOR.MINOR.PATCH**,
baselined at **1.0.0** with the initial release.

The version is a single source of truth: `appVersionName` in
[`app/build.gradle.kts`](app/build.gradle.kts). `versionCode` is derived from it
(`MAJOR*10000 + MINOR*100 + PATCH`), and the built APK is named
`CreditDebitTracker-<version>-<buildType>.apk`
(e.g. `CreditDebitTracker-1.5.3-release.apk`).

## When to bump

Bump `appVersionName` on every change merged to `main`, by the highest-impact
change in the PR:

| Change | Bump | Example |
| -------- | ------ | --------- |
| Breaking change — removed/renamed user-facing behaviour, data-format break, raised `minSdk` | **MAJOR** | `feat!:` / a commit with `BREAKING CHANGE:` |
| New backward-compatible feature | **MINOR** | new screen, backup/restore, add-from-contact |
| Bug fix, build/CI/config fix, refactor, docs | **PATCH** | `fix(sonar): …`, `fix(build): …` |

### Dependabot updates

Classify by what the dependency change affects, not merely by its own version jump:

- **Build-tooling / test-only bump** (AGP, Gradle wrapper, Kotlin, KSP, lint,
  CI actions) — no change to what ships on-device → **PATCH**.
- **Security fix** (any Dependabot security alert) → at least **PATCH**.
- **Shipped-runtime library bump that adds app capability** (a minor/feature
  bump of a dependency whose code runs in the APK) → **MINOR**.
- **Shipped-runtime library major bump that changes/removes behaviour the app
  relies on** → **MAJOR**.

Rule of thumb: if it only affects how the app is *built or tested*, it's a
PATCH; if it changes what the app *does on a user's device*, treat it like a
feature (MINOR) or a breaking change (MAJOR).

## How the current version was derived

Starting from `1.0.0` (the initial release, PR #1), each subsequent PR merged to
`main`:

| PR | Change | Type | Version |
| ---- | -------- | ------ | --------- |
| #1 | Initial Credit · Debit Tracker | baseline | **1.0.0** |
| #2 | Light / dark / system appearance mode | feat → MINOR | 1.1.0 |
| #3 | Bump Kotlin 2.2.21 → 2.4.21 | dep (build tool) → PATCH | 1.1.1 |
| #4 | Bump KSP 2.2.21-2.0.5 → 2.3.12 | dep (build tool) → PATCH | 1.1.2 |
| #9 | AGP 9.4.1 / Gradle 9.6 / compileSdk 37 + AndroidX | feat → MINOR | 1.2.0 |
| #10 | Add person from a contact (picker) | feat → MINOR | 1.3.0 |
| #11 | Bump gradle-wrapper 9.6.0 → 9.8.2 | dep (build tool) → PATCH | 1.3.1 |
| #12 | Confirmed delete button for entries | feat → MINOR | 1.4.0 |
| #13 | Backup & restore (JSON file + Drive) | feat → MINOR | 1.5.0 |
| #14 | Resolve SonarCloud main issues | fix → PATCH | 1.5.1 |
| #15 | Clear Dependabot build-classpath alerts | fix (security) → PATCH | 1.5.2 |
| #16 | Sync JVM bytecode target 17 → 21 | fix → PATCH | 1.5.3 |

**Current version: `1.5.3`** (`versionCode 10503`).
