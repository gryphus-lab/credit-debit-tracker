# Credit · Debit Tracker

A simple, offline-first Android app for tracking money you lend and borrow,
per person. Inspired by the design of
[ariyha/CreDebt](https://github.com/ariyha/CreDebt), rebuilt with a modern
Material 3 / Jetpack Compose stack and extended with **Swiss Franc (CHF)**
support (CHF is the default currency).

## Features

- **Dashboard** – two summary cards: *Owed to you* (total credit) and *You owe*
  (total debit), computed across everyone.
- **People** – add the people you track money with; each row shows their net
  outstanding balance, green when they owe you, red when you owe them.
- **Per-person ledger** – record entries with an amount, an optional note, and a
  simple **They owe you / You owe them** toggle.
- **Settle** – tap an entry to mark it settled (or reopen it); settled entries
  stop counting toward balances.
- **Delete** – remove an entry; deleting a person removes their entries too.
- **Multi-currency** – CHF (default), EUR, USD, GBP, INR, JPY, AUD, CAD. The
  symbol in the top bar opens the picker.
- **Appearance** – Light / Dark / System (follows the device), chosen from the
  brightness icon in the top bar and remembered across launches.
- **Offline & private** – all data is stored locally with Room; no network, no
  accounts.

## Design

High-contrast, flat Material 3 dark theme in the spirit of the companion
CallSimulator app: near-black surfaces, a confident green for credit and a clear
red for debit, pill FABs, generous spacing. Interactions are deliberately
minimal — a couple of dialogs and tap/FAB actions, nothing to learn.

## Tech stack

| Area | Choice |
| --- | --- |
| Language | Kotlin 2.4.21 |
| UI | Jetpack Compose + Material 3 (Compose BOM 2026.09.00) |
| Navigation | Navigation-Compose 2.10.2 |
| Persistence | Room 2.8.5 (KSP 2.3.12) |
| Build | Android Gradle Plugin 9.4.1, Gradle 9.6.0 |
| Min / target / compile SDK | 26 / 37 / 37 |

> **Toolchain note:** the app needs Room (KSP) + the Compose compiler on one
> Kotlin version. It runs on **AGP 9.4.1 with built-in Kotlin** (Kotlin 2.4.21);
> Room's annotation processing uses **KSP 2.3.12** (the KSP2 line, which is
> compatible with AGP 9's built-in Kotlin). The current AndroidX libraries
> require AGP 9.1+ and `compileSdk 37`.

## Build & run

A [mise](https://mise.jdx.dev/) toolchain file pins the build JDK (Temurin 21)
and provides tasks:

```bash
mise install              # provision the JDK (first time)
mise run assemble-debug   # build app/build/outputs/apk/debug/app-debug.apk
mise run install-debug    # build + install on a connected device
mise run test             # unit tests
mise run check            # lint + checks
```

Or straight Gradle: `./gradlew assembleDebug`.

The Android SDK location is read from `local.properties` (`sdk.dir=...`).

## Data model

- `Person(id, name)`
- `Entry(id, personId, note, amountCents, isCredit, settled, createdAt)` —
  amounts are stored as integer **cents** to avoid floating-point rounding; a
  person's balance is the sum of their unsettled entries (credits positive,
  debits negative). Money parsing/formatting lives in the pure, unit-tested
  `Money` helper.
