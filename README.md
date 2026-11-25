# Openeye

**Your attention, back on your side.**

Openeye is a local-first Android digital wellbeing app for short-video feeds. It hides the surfaces
built to be endless, counts what you actually watched, and interrupts in a way that is meant to
change behaviour rather than make you feel bad about it.

No account. No server. No `INTERNET` permission. Everything stays on your phone.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" height="72" alt="Get it on F-Droid">](https://f-droid.org/packages/io.github.dharma_labs.openeye/)

The signed APK is also attached to each
[GitHub release](https://github.com/dharma-labs/openeye/releases). F-Droid re-signs every build, so
do not switch between the two sources without uninstalling first.

## What it does

| | |
| --- | --- |
| **Blocks the endless surfaces** | Instagram Reels and Explore, YouTube Shorts, Facebook Reels, TikTok For You, Snapchat Spotlight. Two modes: a breathing pause you can decline, or a hard stop. |
| **Hides the buttons** | The Reels / Explore / Shorts tabs can be covered in-app, so the shortcut is gone rather than resisted. |
| **Counts what you watched** | Feed minutes, estimated clips encountered, sessions, swipes, taps, longest session. A live counter sits inside the feed. |
| **Puts the number where you see it** | A home-screen widget, and an optional corner timer drawn over whatever app is open. |
| **Interventions, not reports** | A daily stop rule you sign while calm and get quoted back when you are over it, if-then plans, feeling check-ins, and a lapse card that does not scold you. |
| **Schedules and focus** | Weekly quiet windows, study/work/sleep presets, focus sessions, per-app daily budgets. |

Every figure comes from records measured on your device. Nothing is extrapolated from a sample and
no screen in the app contains made-up data.

## Privacy

Openeye asks the user for two things and uses both in ways you can verify:

- **Usage access** — how long each app was in the foreground. This is how the time is measured.
- **Accessibility** — which screen is showing, so a feed can be counted and interrupted.

The manifest declares those two plus `FOREGROUND_SERVICE` (the focus timer), its
`FOREGROUND_SERVICE_SPECIAL_USE` type, and `POST_NOTIFICATIONS` (Android 13+ requires it to show
that timer). That is the complete list. There is no `INTERNET` permission at all, so the app cannot
upload anything even if it wanted to, and there is no `SYSTEM_ALERT_WINDOW` — the in-feed overlays
come from the accessibility service you grant. No analytics SDK, no Firebase, no backend, no push.
Backup is disabled and data lives in an app-private store; `clear all local data` in Settings deletes
it.

The accessibility service does not read, store, or forward anything you type. It inspects the view
tree to identify feed surfaces and nothing else.

## Build it

Requires JDK 17 or newer and an Android SDK with API 37.

```bash
./gradlew :app:assembleDebug      # test build
./gradlew :app:testDebugUnitTest  # unit tests, no device needed
./gradlew :app:assembleRelease    # R8-shrunk release APK (unsigned)
```

Sign the release APK with your own key before installing it:

```bash
apksigner sign --ks your-release-key.jks \
  app/build/outputs/apk/release/app-release-unsigned.apk
```

Kotlin, Jetpack Compose (Material 3), minSdk 26, target and compile SDK 37. Dependencies are
AndroidX, Jetbrains Kotlin and kotlinx artifacts under Apache-2.0 — no proprietary or non-free
libraries.

## How it works

```
data/     settings store, usage stats, screen-time meter
domain/   rule engine, attention stats, copy pools — pure Kotlin, fully unit tested
service/  accessibility service, hiding overlays, watch tracker, HUD, corner timer, focus timer
widget/   home-screen widget (RemoteViews, no extra dependency)
ui/       Compose app: Today, Shields, Focus, Numbers, Settings, onboarding, block sheet
```

`domain/` never touches Android, which is why the counting, the translation of minutes into real
things you could have done instead, and every sentence shown to the user are covered by tests.

The longer technical notes — surface detection, the feed session state machine, multi-signal clip
counting, and the five limits that stop an overlay from ever making a phone feel broken — are in
[docs/NOTES.md](docs/NOTES.md).

## Version 5.1.0

The F-Droid release. The package id is now `io.github.dharma_labs.openeye`, the project is
GPL-3.0-only, and onboarding and the block sheet were re-laid out to fit one screen: you should
never have to scroll to find your way out of an interruption.

**If you installed an earlier Openeye from GitHub, uninstall it first.** Android sees a different
app, so the old settings will not carry over.

## Licence

GPL-3.0-only. See [LICENSE](LICENSE).

Built by Dharmendra.
