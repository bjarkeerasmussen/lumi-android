# Lumi — daily skin progress journal

A small, private Android app for tracking your skin over time. Take one photo a
day, rate how it looks, log your routine and LED light-therapy sessions, and
watch your progress with a before/after compare and a trend line. Optional AI
feedback uses the AI apps you already have (Claude, Gemini, ChatGPT) via the
share sheet — Lumi has no server of its own.

Built with the same toolchain as CareMonitor and Nightfall (Kotlin + Jetpack
Compose, APK built in the cloud by GitHub Actions — no Android Studio needed).

## What it does

- **Today** — take the day's photo with an **ID-style oval face guide** (line
  your face up inside the oval, so every day is framed the same), quick
  self-ratings (overall / redness / breakouts), a note, and a daily streak.
- **Progress** — photo timeline, a before/after cross-fade slider, and a trend
  chart of your ratings.
- **Routine** — log AM/PM products and **LED light-therapy sessions** (red /
  blue / near-infrared, minutes).
- **Learn** — plain-language skincare guidance, including how LED masks
  (Therabody, Omnilux, etc.) actually work.
- **Skin Check** — a non-medical self-assessment that names common cosmetic
  patterns (dryness, oiliness, congestion, redness, dullness…) and suggests
  general care. It does **not** diagnose conditions and routes anything
  concerning to a dermatologist.
- **Settings** — daily reminder, export/backup, privacy.

## Safety & scope (important)

Lumi is a self-care journal, **not a medical device**. It does not diagnose.
It never recommends UV light — the masks it supports are **LED** (visible
light), which is different from and far safer than UV. For anything painful,
spreading, changing (e.g. a mole), or that won't heal, Lumi tells you to see a
dermatologist.

## Privacy

Everything stays on the phone: photos in the app's private storage, the journal
as a single `journal.json`. Nothing is uploaded. A photo only leaves the device
if you tap "Ask an AI" and pick an app yourself.

## Building the APK

This repo mirrors the CareMonitor / Nightfall setup. Push it to GitHub and the
**Build APK** workflow (`.github/workflows/build-apk.yml`) builds a debug APK on
every push. Download it from the run's **Artifacts** (`lumi-debug`), then
sideload it onto the phone.

- Toolchain: AGP 8.5.2, Kotlin 1.9.24, Compose BOM 2024.06.00, compileSdk 34,
  minSdk 26.
- No Gradle wrapper is committed on purpose; CI provisions Gradle 8.9.

## Project layout

```
app/src/main/java/com/lumi/app/
  MainActivity.kt            app shell + bottom-nav
  data/                      Models, on-device store, prefs
  skincheck/SkinCheck.kt     non-medical pattern rules
  learn/LearnContent.kt      curated guidance
  share/AiShare.kt           share-sheet handoff to Claude/Gemini/ChatGPT
  reminder/                  daily notification
  ui/                        theme, components, screens
```
