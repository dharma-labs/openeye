# Openeye

**Your attention, back on your side.**

Openeye is a local-first Android digital wellbeing app. It hides the addictive surfaces inside
short-video apps, counts time and estimated items seen in Reels/Shorts, puts that number on the home
screen where it can be seen without opening anything, and talks to you in a way that is meant to
change behaviour rather than make you feel bad about it. The point is fewer minutes on a feed, not a
neat report about them - so the blocking, the counting, the reminders and the widget read from one
set of measured records and aim at the same outcome.

Version 4.0.0 - the public-launch cut: a redrawn mark, four tabs instead of five, the per-app
details folded behind `add apps`, permissions that ask themselves at the right moment, and every
screen driven on an emulator before it shipped.
4.0.1 - 4.0.3 are the counting-correctness cuts: the overlay counts on a feed that emits no events,
volume keys no longer restart a session, an interruption is resumed instead of reset, and a session
closes when the clip feed is provably gone. See Verification.
4.1.0 is the "do not break the phone, and do not hide the number" cut: the intrusive parts are now
physically bounded and can opt themselves out, Today / Shields / Numbers were cut back to their
numbers, there is a home-screen widget, and Settings says who built it. See Verification.
Forked from Openeye 2.3.0 at
`C:\Users\kumar\.openclaw-autoclaw\workspace\openmind` (that folder is untouched).
Package id intentionally unchanged: `com.example.openmind`. **Because the id is shared, installing
this build replaces an installed 2.x on the same device. The DataStore schema is additive
(`ignoreUnknownKeys`), so old settings load into the new app; downgrading will drop the new fields.**

## Why this version reads differently

Every screen, HUD line and interruption was rewritten around what the short-form-video research
actually supports, and four mechanics were added because the copy alone would have been decoration:

| Finding | What it changed here |
| --- | --- |
| Variable-ratio reinforcement; the slot machine is the design, not a side effect (Clark & Zack 2023) | Onboarding step 0 explains the pull before asking for anything |
| Wanting is not liking - cue-triggered dopamine prediction error survives a flat, numb session (Berridge & Robinson) | Feeling check-ins; after 3 entries the app shows the pattern, e.g. "M landed on flat or numb. that's wanting, not liking." |
| No natural stopping cue | Daily stop-rule signed in the contract while calm, quoted back when over (`Coach.blockLine`) |
| Repeated warnings habituate (Maynard 2014; Stawarz CHI 2015) | `promptCursor` rotates every interruption; `VoiceAndTimeTest.rotatingPoolsNeverRepeatConsecutively` guards it |
| Implementation intentions, d about 0.65 (Gollwitzer & Sheeran 2006) | Plans screen: when/then pairs, armed by default, generated from your own data on Insights |
| Friction beats guilt; one sec and the Max Planck nudge cut consumption around 57% without moralising | 8-second breathing gate, "your call", "nah, go back" always visible |
| Affect labeling dampens the response (Lieberman 2007) | The block screen asks you to name a feeling in one word and what you were hoping for |
| Self-compassion after a lapse outperforms self-criticism (Breines & Chen 2012) | Lapse card: "look at it" / "drop it", curiosity questions, 10-minute re-entry. No verdicts |
| Gain framing beats loss framing (Rothman & Salovey) | "put an hour back", "take 10 minutes back" - never "you wasted" |
| Intervene at a natural breakpoint (Iqbal & Horvitz CHI 2007) | Interruptions land on surface entry, not mid-clip |
| Monitoring alone rarely changes use (Lukoff et al. CHI 2022) | Minutes become forgone things: films, episodes, walks, whole workdays (`TimeTranslation`) |
| Peer-misperception stats are effective but must be true | Not invented. Insights compares you with your own tracked average and says so (`Voice.noPeerStats`) |

Deliberately absent: streaks, badges, points, levels, leaderboards, confetti, red as a shame
colour, push praise, and forced slang. `VoiceTest.poolsAvoidShameClinicalAndHypeLanguage` fails the
build if banned vocabulary ("addict", "detox", "clean", "sober", "discipline", "failure",
"put the phone down", "you'll thank yourself", "unlock/level up/achievement") enters a copy pool.

## What works

| Area | State |
| --- | --- |
| Instagram Reels / Explore blocking | Working (accessibility heuristics) |
| YouTube Shorts blocking | Working |
| Hide Reels / Explore / Shorts navigation items | Working (opaque accessibility overlay) |
| Hide YouTube Shorts shelves on Home | Best-effort |
| Facebook Reels | Catalog-driven, on by default, selectors unverified |
| TikTok For You, Snapchat Spotlight | Best-effort, off by default |
| Gentle mode + intentional access + breathing step | Working; rotating copy, quotes your own stop rule when over |
| Strict mode | Working |
| Attention contract (daily stop rule, signed while calm) | Working, onboarding step 1 + Plans screen |
| If-then plans (when/then, armed/off, max 140 chars each) | Working, Plans screen; Insights offers a generated draft |
| Feeling check-ins and the wanting/liking reveal | Working, today + block screen |
| Lapse flow (name it / drop it / 10-minute re-entry) | Working, once per day |
| Time translated into forgone things + hedged yearly projection | Working, today and Insights |
| Weekly schedule with overnight windows | Working |
| Study / Work / Sleep routines | Presets that fill the schedule |
| Focus sessions (15/25/45/90 + custom 5-180 min) | Working |
| Live Reels/Shorts counter HUD | Working; lowercase, wording escalates as the session grows, shows "over your Nm" past your number |
| In-feed reminder popups | Working; every 5 min of feed time by default (off/3/5/10), non-touchable overlay that fades on its own |
| Whole-app time in the reminder | Optional "long videos too" switch; real UsageStats minutes, never a feed estimate |
| Coverage | Instagram (+ Lite), YouTube, Facebook (+ Lite), TikTok (both package ids), Snapchat |
| Home-screen widget | Working. `Openeye today` in the launcher's widget picker: feed minutes as the number, screen time in the line above, estimated clips and visits below, and the stop rule when one is signed. Same records the Today screen reads; tap opens the app. |
| Hide the Reels / Explore / Shorts / Spotlight button | Working, catalog-driven; TikTok's For You is Home, so it is counted and never covered |
| Strava-style summary card | Day/Week toggle; PNG rendered on-device and shared via the system sheet (WhatsApp and others) |
| Today dashboard | Coach hero message, forgone-time translation, lapse card, feeling reveal |
| Breathe screen | Intentional block screen with stop-rule echo, active plan, feeling chips and free text |
| Insights | Today / 7 days / 30 days, own-baseline comparison, feeling reveal, plan draft CTA |
| App-level daily budgets | Settings in Shields; 80% warning + block when spent (needs usage access) |
| Dark / Light / System theme | Working; token-based palette |
| Local analytics and data clearing | Working |
| About in Settings | Working: name, version and build number read from `BuildConfig`, who built it, what the accessibility service is for, and the widget's place in the launcher picker |

### What is measured

- **Feed time** - real foreground time inside a detected Reels/Shorts/Explore/For You surface.
- **Estimated videos encountered** - first item on entry plus each detected paging transition.
  Auto-advance players that emit no accessibility scroll are not counted, so this under-reports.
- **Feed transitions** - exposures minus sessions. Never claimed to be unique videos.
- **Sessions** - one per entry into a surface. **Longest** - max session time, not a sum.
- **Detected taps** - accessibility click events while a feed is active; system-level raw touches
  are unavailable to a non-root accessibility service, so taps are a lower bound.
- **Average per item** - feed time divided by estimated exposures, labelled avg/item.
- **Current item** - live seconds since the last detected paging transition.
- **Screen time** - UsageStats foreground minutes for the five catalogued apps (main package id
  only, so Instagram Lite / Facebook Lite time is not summed in), read on demand and cached 60s.

Today and Insights both call `AttentionStats`, so their totals agree by construction.

## Performance

The service runs inside someone else's app while they scroll, so the cost is bounded by design:

- One window pass per second. The ticker asks the window manager who owns the screen once and hands
  the answer to the surface check, the counter tick, the HUD and the reminder. Nothing on that path
  reads the tree twice for the same tick.
- The tree is only re-read during an event drought (`VERIFY_AFTER_QUIET_MS`). While the feed is
  talking to us, its own events already measure the screen several times a second, so a second
  opinion would be work for nothing.
- The ticker is stopped whenever no catalogued app is foreground. On an idle phone the process
  samples at 0% CPU; that is the whole background cost.
- Node-tree evaluation is throttled to every 300 ms, and every window-state change bypasses the
  throttle so a cover does not lag the screen it belongs to.
- A tree walk stops at 400 nodes (`MAX_NODES`); the traversal and the catalog match are one pass.
- `HidingOverlayController.sync` diffs targets by key and calls `updateViewLayout` for moved
  bounds, so scrolling a feed moves a window instead of tearing down and re-adding it. A failed
  add or move is not cached, so the next event retries it.
- `WatchHudController` keeps `lastText` and returns early when the line is unchanged, which is the
  common case between the per-second ticks.
- Nudges reuse the HUD's already-computed minutes and count - no extra counters - and the whole-app
  UsageStats query behind "long videos too" is cached per platform for 60 s.
- Settings come from an in-memory `StateFlow` backed by Preferences DataStore; the event path never
  hits disk, and the only serialized reads are additive JSON fields.
- The widget is redrawn on a flush, on app open, and on the system's own 30-minute cadence - never
  on a timer of its own - and does nothing at all when no widget is placed.
- Splash is a minimum of 1.1 s and is skipped as soon as state is ready; its enter animation is
  420 ms with a fast-out/slow-in curve, so the mark settles instead of snapping. The window is brand
  time, not load time - the data is already there. The sweep is driven from the layout phase
  (`Modifier.offset { }`), not by recomposing the screen.
- Release builds run R8 with `isShrinkResources`: 1.5 MB against 13.5 MB debug. The signed output has
  been run on the emulator end to end - launch, onboarding, contract persistence, block screen, a
  counting session - so the shrunk artifact is not unproven, but it has not had a full day on a
  physical phone.

### What the app will not do to your phone

An accessibility overlay can make a device feel broken without crashing it: a window that covers
the status bar, a back press that goes somewhere you did not mean, a burst of covers because the
view-tree guess is wrong. Four limits exist for that reason, and all four were written after the
"overlay makes system failure" reports:

- **Bounded covers.** A hiding window must be an on-screen, clickable node inside the app, at most
  18% of the screen area, 30% of its height and 95% of its width, and there are at most four of
  them. Anything bigger is clamped, anything else is refused. `FLAG_LAYOUT_NO_LIMITS` is gone, so no
  cover can reach the status or navigation bar.
- **Backoff on a hostile window manager.** Three consecutive `addView`/`updateViewLayout` failures
  stop the hiding engine for 20 s instead of retrying every event.
- **A circuit breaker on intrusion.** Six covers or block screens inside two minutes means the
  guess is wrong, so the service puts its hands off the system for ten minutes - counting, the HUD
  and the reminders carry on, nothing is drawn over another app and no back press is sent.
- **Navigation only after a landing.** The block screen is started first and the back press that
  walks you out of the feed is checked 700 ms later, only if the feed still owns the window. A back
  press into our own block screen would dismiss it and drop you back into what we meant to block,
  which reads as a hang.

---

## Architecture

```
data/       Models, AppRepository (DataStore), UsageStatsRepository
domain/     RuleEngine, BypassStore, AttentionStats,
            Voice (copy pools), Coach (what to say), TimeTranslation (minutes -> real life)
service/    GuardAccessibilityService, HidingOverlayController,
            WatchSessionTracker, WatchHudController, WatchNudgeController, FocusTimerService
widget/     OpeneyeWidget - RemoteViews, no Glance, no new dependency
ui/         AppRoot, AppViewModel, BlockActivity, components, screens
            (screens: Today, Plan, Shields, Focus, Insights, Schedule, Settings, Onboarding)
brand/      openeye-icon-1024.png - master launcher artwork
```

`Voice` holds the sentences, `Coach` decides which one is true right now, and `TimeTranslation`
does arithmetic on measured minutes. Nothing in `domain/` touches Android, so all three are unit
tested; screens never contain a hard-coded motivational string.

- DataStore (not Room) holds settings, the 30-day block log and 120 days of watch records. The
  dataset is small and bounded; adding Room would add a migration surface without a query need.
- All writes are serialized in `AppRepository`; one `Settings` object and one `WatchRecord` list
  are the single source of truth. No sample or fabricated data is ever inserted.
- Kotlin 2.3.20, Compose BOM 2026.03.01, AGP 9.0.1, minSdk 26, target/compile 37.
- No INTERNET permission, no backend, no Firebase, no analytics SDK.
- The 4.0.0 state lives inside the existing `Settings` JSON rather than in new DataStore keys:
  `contract` (stop rule, signed timestamp, why, usual feeling), `plans` (when/then pairs),
  `feelings` (check-ins, capped at 200), `promptCursor` (which interruption line is next),
  `lapseHandledDay`, `planAskedInOnboarding`. Nesting keeps the single-source-of-truth invariant and
  needs no migration - `ignoreUnknownKeys` handles both directions.

### Voice rules

`domain/Voice.kt` is the only place user-facing sentences are written. Lowercase, twelve words or
fewer per line, one question per screen, always an exit. Interruption copy is a rotating pool keyed
on `promptCursor`, snapshotted in `BlockActivity.onCreate` *before* it advances, so two
consecutive block screens can never show the same line - that is the anti-habituation requirement
expressed as an ordering constraint, and a test guards the rotation.

Screen chrome outside those pools is on the same budget: 4.0.0 cut roughly 370 words of explainer
prose out of Today, Plan, Insights, Focus, Schedule, Settings, Onboarding and the block screen down
to one short line per idea. Nothing mechanical was removed - every control, number, chip and
disclosure survived, only the sentences around them shrank.
4.1.0 did the same to the *layout* of Today, Shields and Numbers: one figure that is the answer,
the settings that matter as chips, and everything else behind `today in detail` or `advanced`. The
parameter lists of the old and new screens were diffed to prove no control was lost with the prose.

### Multi-signal view counting (2.3.0)

Counting no longer depends on a single accessibility signal, because Instagram/YouTube often
emit scroll events with zero deltas and -1 indices.

1. **First view** is counted after 2 s on screen, so an immediately blocked entry is not counted.
2. **Any pager scroll** counts a transition (1.5 s gate). Comment-sheet scrolls are rejected by
   source height and the presence of an editable field.
3. **Content signature**: visible captions/usernames (numbers and UI labels removed) are compared;
   a 35% Jaccard change counts a transition (3 s gate).
4. **Collection row index**: if the visible collection row changes, it counts a transition.
5. **Taps** come from accessibility click and long-click events.

All signals share one last-count timestamp, so a swipe cannot be counted three times. If the app
still shows a mismatch, `adb logcat -s OpeneyeCount OpeneyeHiding` prints the signal that counted
(`source=scroll|content|index`), the surface and the running session count.

### Feed session state machine

Entry -> tracker starts -> 2 s dwell counts first view -> scroll/transition -> 60 s flush -> exit/grace
(3 s) -> final flush. Durations use `SystemClock.elapsedRealtime()` (monotonic, immune to clock
changes) with deltas capped at 5 s; wall-clock dates group records. Screen off, app switch,
service restart and rotation flush or clear state. Midnight attribution uses the flush time, so
a session crossing midnight is credited to the day it ends.

### Surface detection

Activity class cannot be trusted: Compose windows report `android.widget.FrameLayout`, not the activity
name. Detection therefore uses several independent signals, in order:

1. Activity class, when the vendor reports one the surface declares in `classHints`.
2. Content, for a clip pager with no bottom bar: a header caption in `titleTokens`, a view id in
   `idHints`, or an audio caption ("original audio" / "original sound").
3. A selected bottom-tab label matching `navTokens`.

**Every one of those signals is data, not code.** `PlatformCatalog` holds one `Surface` per entry
point with its `classHints`, `idHints`, `navIdHints`, `titleTokens`, `navTokens` and a `hideNav`
flag, and the accessibility service iterates the catalog. Adding another short-video app is a
catalog entry and nothing else; `PlatformCatalog.packages` maps every install variant (including
lite builds) to its platform id, and `PlatformCatalogTest` fails the build if a surface carries an
empty or capitalised hint, belongs to no package, or claims to be hideable with nothing to match.
TikTok is the exception that proves the flag is needed: its For You feed *is* the home tab, so it is
detected and counted but `hideNav = false` keeps the cover off it - a covered home button is an
uninstall.

The fake-Instagram harness used for the 2.3.0 release confirms the content path: no activity class, no
bottom bar, top title `Reels` → surface detected, HUD live, swipes counted.

### In-feed reminders

`WatchNudgeController` draws the same `TYPE_ACCESSIBILITY_OVERLAY` window class as the counter, so
no `SYSTEM_ALERT_WINDOW` permission is involved, and adds `FLAG_NOT_TOUCHABLE` so it can never eat
a scroll. It is timed on **feed minutes, not wall clock**: the first arrives after two minutes of
watching, then every `nudgeMinutes` (off / 3 / 5 / 10, default 5). Copy comes from `Voice.nudges`,
which carries exactly two slots - today's feed time and clips watched - and switches to
`Voice.nudgesOver`, quoting the number the person signed, once today is past it. `VoiceTest` asserts
the slot count, because a raw `%s` printed over someone's video is the one bug this surface cannot
have. Strict blocks, the block screen and leaving the app all tear it down immediately.

### Hiding engine

- Active root selected from `AccessibilityService.windows` by target package, never our overlays.
- Labels matched without requiring `isVisibleToUser`, plus resource-id hints and clickable
  container preference.
- `getBoundsInScreen()` -> `TYPE_ACCESSIBILITY_OVERLAY`, `PixelFormat.OPAQUE`, touch-consuming.
- Re-measured on content/scroll events; cleared on rotation, app exit and block screen.
- Cover colour follows explicit app theme (light/dark) or system theme in System mode.

---

## Simple setup

Onboarding is four steps: why the feed feels like a slot machine, your stop rule and usual feeling
(the precommitment - asked while you are calm and nothing is blocking you, before any permission),
screen access, then a recap that reads your own answers back to you. Screen time (usage access) is
a separate optional card that can be skipped. Notifications are no longer part of first-run setup;
they are requested only when a focus session needs them, from Settings. The accessibility
description in system settings stays accurate, but the in-app wording avoids Android jargon.

The stop rule is the only number the app holds you against, and it is yours. There is no target
inherited from an article or a study.

## Logo

`brand/openeye-icon-1024.png` (plus a 512 export) is the master mark: one solid lime lens on ink
with the swipe arrow knocked out of it as negative space. Nothing else. The earlier version carried a
stroked outline, a pupil and decorative circles, and all three vanished at launcher size. The adaptive
layers are `res/drawable/ic_launcher_foreground.xml` (the same single even-odd path, inside the safe
zone) and `ic_launcher_background.xml` (a flat rect). `EyeMark` in `ui/components/Components.kt` and
`drawEye` in `ui/SummaryShare.kt` draw identical geometry, so the splash, the nav mark, the block
screen and the shared card match the icon on the home screen.
Legacy `mipmap-*/ic_launcher*.webp` files are untouched and unused - minSdk 26 guarantees the
adaptive icons win on every supported device.

## Sharing the summary

The Day/Week summary card renders a 1080x1350 PNG locally, writes it to the app cache, exposes it through a
FileProvider and opens the Android share sheet. WhatsApp, Telegram, Messages and any installed
image target appear; no storage permission and no network permission are needed. Nothing is
uploaded by Openeye itself.

## Privacy

- Accessibility is used only for the four configured packages. Window changes from other apps are
  read only to remove overlays when the target app loses the foreground; no content is inspected.
- Editable text, passwords, messages and page content are never read or stored.
- Screen bounds are used in memory for overlays and are never persisted.
- UsageStats is queried on demand for Insights and at most once a minute for app budgets.
- `allowBackup=false`, data-extraction rules exclude everything, history is capped and clearable.

### Google Play

`android:isAccessibilityTool="false"` is correct, and `flagIncludeNotImportantViews` is used only to reach tab items. AccessibilityService use requires a declaration
and manual review; digital-wellbeing blockers are frequently rejected under the accessibility
policy. Submit with a disclosure video and expect review risk; direct APK or enterprise
distribution is the fallback. Usage access needs the separate declaration.

---

## Build

```powershell
$env:JAVA_HOME='C:\Users\kumar\.jdks\jdk-21.0.12+8'
cd C:\Users\kumar\.openclaw-autoclaw\workspace\openeye
.\gradlew.bat :app:assembleDebug --console=plain
.\gradlew.bat :app:testDebugUnitTest --console=plain
.\gradlew.bat :app:assembleRelease --console=plain
```

Artifacts:

- `app/build/outputs/apk/debug/app-debug.apk` (signed with the debug key, installable)
- `app/build/outputs/apk/release/app-release-unsigned.apk` (R8 minified; sign before distribution)
- `C:\Users\kumar\Downloads\openeye-4.1.0-debug.apk` - the copy handed over for install testing
- `C:\Users\kumar\Downloads\openeye-4.1.0-release-unsigned.apk` - for the Galaxy Store upload after signing
- `C:\Users\kumar\Downloads\openeye-4.1.0-release.apk` - zipaligned + signed (v2, v3), the public download
- `C:\Users\kumar\.openclaw-autoclaw\workspace\openeye-signing\` - `openeye-release.jks` and
  `keystore-password.txt`, outside the repo. This key is the only way to ship an update over an
  installed release build; if it is lost, every user has to uninstall and lose their data.
- Public release: `https://github.com/dharma-labs/openeye-releases` (this repo holds only the signed
  apk and the install page; the source stays private here)

## Verification

What was checked for 4.0.0, in this environment only:

- `:app:assembleDebug`, `:app:assembleRelease` -> BUILD SUCCESSFUL.
- `:app:testDebugUnitTest` -> 54 tests, 0 failures, 0 errors: 14 `RuleEngineTest`, 11 `CoachTest`,
  10 `PlatformCatalogTest`, 6 `AttentionStatsTest`, 6 `VoiceTest`, 5 `TimeTranslationTest`,
  2 legacy template tests.
- `:app:lintDebug` -> 0 errors.
- Manifest: label Openeye, version 4.0.0 (code 10), minSdk 26, target 37, no INTERNET, no
  SYSTEM_ALERT_WINDOW. Accessibility config: window/content/scroll events, no package allowlist.
- **Driven on an API 36 emulator (`medium_phone`, x86_64, headless) with the real APK installed.**
  First run from a clean `pm clear`: splash with the credit, onboarding, the accessibility deep
  link, the system toggle, back into the app, which re-read the grant and flipped the card to `on`
  without a restart. Notification permission is requested at launch on 13+.
  Every tab and overlay was tapped: today, shields (3 core apps, `add apps` reveals TikTok and
  Snapchat, row expansion, per-surface strict/gentle/off, daily-limit chips, reminder chips,
  long-videos switch), focus (start, live countdown, stop, custom +/- picker), numbers (day/week/
  30-day ranges, the screen-time request that lands on the real system page), plans (add, arm,
  park), quiet hours (routine presets, day toggles, time picker, strict/gentle mode), settings
  (theme chips, disclosure, share sheet, all three permissions reading `allowed`), in light and dark.
- Against a real third-party app: YouTube launched, Openeye's HUD pill drew over it, the Shorts tab
  was covered and its taps swallowed, and the strict block screen fired with the stop rule and the
  armed plan quoted back. `close it` dropped out of YouTube. Screen time then appeared on Today and
  in Numbers with real UsageStats minutes.
- Two defects only a running app could show, both fixed here: `POST_NOTIFICATIONS` was never
  requested, and the feed HUD stayed painted over Openeye's own screens when you came back from
  YouTube.

### Counting correctness (4.0.1 - 4.0.3)

Reported from devices: the overlay counted nothing on Reels/Shorts, volume keys restarted the
counter, and continuous same-app watching was not continuous. Each was reproduced on the API 36
emulator and fixed, then re-checked on the installed binary with screenshots and `logcat`:

- `:app:assembleDebug`, `:app:assembleRelease` -> BUILD SUCCESSFUL; `:app:testDebugUnitTest` ->
  60 tests, 0 failures, 0 errors. Manifest: version 4.0.3 (code 13), still no INTERNET and no
  SYSTEM_ALERT_WINDOW.
- **Counting through an event drought.** A clip that plays without scrolling emits no accessibility
  events, so the session is now re-measured by a 1 s ticker that asks the window manager who owns
  the screen. Reproduced: a Reels feed left untouched for 25 s kept counting (1:04 continuous).
- **Volume keys.** Two causes, both fixed: the system volume panel (`com.android.systemui`, not an
  application window) was being read as the foreground app, and `onInterrupt()` tore the session
  down when a key burst backed up the event queue. `foregroundWindowPackage()` now only accepts
  `TYPE_APPLICATION` windows and `onInterrupt()` is a no-op. Reproduced and re-checked: five volume
  presses leave the session continuous (0:28 with `active=true` in the log).
- **Continuous same-app watching.** An interruption now closes the session but keeps it resumable
  for 8 s, and the borrowed seconds are not counted. Reproduced: 0:28 before a 5 s home-screen trip,
  0:58 after it - one session, gap excluded.
- **The in-feed time popup.** It did not fire because reminders were set to `off` in the stored
  settings, not because of a code defect. With a 3-minute cadence the popup was caught on screen
  inside a real clip feed, quoting measured feed minutes and estimated clips.
- **No invented sessions.** Two honesty regressions surfaced during this work and are fixed: the
  structural pager rule claimed the Instagram *home* feed as Reels (Compose renders the home feed as
  a full-screen pager too), and a ticker auto-resume resurrected a Reels session on package match
  alone. The pager rule now refuses any screen still showing the app's tab bar, the auto-resume is
  gone, and the surface is re-read from the window tree on every tick (`verifySurface`), so a session
  ends within ~2 s of the clip feed being gone even when the app emits nothing afterwards.
  Reproduced: `leave surface (tick)` in `logcat`, and the home feed showing only
  `today 35m · 25 clips` with no session lines.
- Clip and swipe counters move with real swipes (`source=content surface=reels count=3`, HUD
  `2 swipes`, `this one` following the page to Reel 3). Time is measured; counts stay labelled
  estimates.

### Safety, cost and the widget (4.1.0)

Written after reports that the overlay caused a system failure on some devices. Re-checked on the
same API 36 emulator with the installed 4.1.0 binary:

- `:app:assembleDebug`, `:app:assembleRelease`, `:app:testDebugUnitTest`, `:app:lintDebug` -> all
  BUILD SUCCESSFUL. Manifest: version 4.1.0 (code 14), still no INTERNET and no SYSTEM_ALERT_WINDOW.
- **Nothing was bricked, and nothing of ours failed.** `dumpsys dropbox` holds no entry for
  `com.example.openmind` at any point; the last system ANR on the image is 08:44, two hours before
  this test window. No "System UI isn't responding" dialog appeared during any run below.
- **The block screen survives its own back press.** Before the fix, the back press that walks you
  out of the feed could land on our block screen and dismiss it, leaving the user inside the app we
  meant to block. With the delayed check, the block screen is up and stays up until answered.
- **Idle cost is zero.** `dumpsys cpuinfo` reports `+0% com.example.openmind` over a five-minute
  window with no feed open, and `top` samples 0.0% while one is. The service is not polling a
  timer while the phone is doing nothing.
- **Counting still works after all of that.** A real session through the block screen: five volume
  presses in the middle of it left `still here 1:27 · this one 1:25 · today 4m · 12 clips` - the
  counter never restarted, and `0 swipes` stayed honest because no swipe happened. Pressing home
  took the readout with it: no HUD on the launcher, and the widget jumped to the flushed number.
- **The widget is a widget.** It appears in the launcher's picker as `Openeye today` with its
  description and preview, drags onto the home screen, and renders
  `openeye · 43m on screen / 4m / ~12 clips · 9 visits` from the same records the Today screen
  reads - it agreed with the app to the clip. Tapping it opens the app. Under a minute of watching
  it says `under a minute · ~2 clips` rather than `0m` next to a clip count, which is the one thing
  an honest number should never look like.
- Today, Shields and Numbers were re-cut around their numbers: one figure per screen, the rest
  behind `today in detail` and `advanced`. Every callback the old screens had is still there -
  checked by diffing the parameter lists - and no screen invents a figure it has not measured.

Not verified in 4.1.0: the cover-area clamp and the ten-minute hands-off breaker have never fired
on a device, because the burst of wrong guesses they exist to survive could not be reproduced here.
They are reasoned-about guards, not observed ones. The widget has only run on the Pixel launcher;
One UI's picker, its resize behaviour and Android 12+'s pin-confirmation dialog are untested.

### The signed R8 release binary (4.1.0)

The 1.5 MB minified output was installed on the same API 36 image - after uninstalling the debug
build, because the signatures differ - and run through the path a new user walks:

- launched with no `AndroidRuntime` error in logcat; splash, onboarding and all three tabs rendered.
- accessibility enabled from system settings; the app read it back as `screen access | on`, so the
  service class survived shrinking and is still discoverable by the system.
- signed the contract at 30 min / "a scroll leaves you flat" and the recap screen showed both
  values after a restart, which is the DataStore round-trip holding under R8.
- a live Reels session, past the block screen: `today 1m · 3 clips / still here 1:14 · 2 swipes /
  this one 0:23` over reel 3. Counting, the HUD and the block flow all work in the release binary.

What this does not prove: the release build was not put through the multi-hour idle, volume-key and
widget-drag passes above - those ran against the debug binary of the same commit.

Not verified: no physical device, so OEM accessibility implementations (Xiaomi/Samsung/Huawei kill
background overlays and rename the accessibility entry) are still unproven - the Samsung/One UI
behaviour behind the Galaxy Store submission is the open risk. Facebook, Facebook Lite, TikTok and
Snapchat were never installed here, so their selectors remain catalog guesses. Production Instagram,
YouTube and Facebook could not be opened on this emulator image (YouTube refuses to run until
updated), so all feed testing above ran against a local harness app that mimics their window,
activity and view structure; the catalog class names for the real apps are still unconfirmed here.

## Known limitations

- Detected taps under-count literal finger touches; the service sees accessibility click events only.
- Reel/Short counts are conservative estimates; auto-advance without accessibility scroll is not
  counted. Time is measured, counts are labelled as estimates.
- Tab labels and resource ids differ between app versions and OEM accessibility implementations;
  when the nav item is not found, the existing block screen remains the fallback.
- Hiding a YouTube shelf leaves blank space because the host app is not reflowed.
- Facebook, Facebook Lite, TikTok and Snapchat selectors are catalog guesses written without a
  device. They can miss, and in the worst case cover the wrong button. Off by default except
  Facebook, which is on but equally unverified.
- The reminder popup is a `FLAG_NOT_TOUCHABLE` accessibility overlay six seconds long. Aggressive
  OEM battery managers can drop the host service mid-session, which would take the popup with it.
- App budgets depend on UsageStats and accessibility; enforcement happens when the target app is
  foreground, not as a system-level app lock.
- Hourly buckets use the flush hour; sessions crossing an hour boundary are attributed there.
- Focus sessions do not yet support pause/resume or a stored history.
- The widget is one fixed size. A launcher that cannot give it 2x2 clips the numbers rather than
  shrinking them, and a resized widget keeps the same content.
- Every 4.0.0 mechanic that beats plain monitoring - the quoted stop rule, "over your Nm" in the
  HUD, the plan echo on the block screen - is inert until you sign a contract. Without one the app
  falls back to rotating reminders, which is the weakest thing it can do.
- Feeling and lapse state are per-day heuristics from local check-ins, not a clinical signal. The
  wording deliberately stops at "that's wanting, not liking" and never suggests a diagnosis.
- Plan text is free-form and local. The app cannot tell whether a when/then pair is realistic, so
  Insights only drafts one from your own measured peak hour and heaviest surface.
- Internal class names and the package id still use "openmind" to preserve the installed update
  path. Rename only with a deliberate migration and signing plan.
- Template demo files (`Navigation.kt`, `ui/main/`) remain unreachable; removal needs file-delete
  approval.
- Real-device validation on Instagram and YouTube is still required.

## Roadmap

1. Device-verified selectors for current Instagram/YouTube builds.
2. Pause/resume focus and focus history.
3. Weekly report, and a second widget size for a week rather than a day. (The daily widget shipped
   in 4.1.0.)
4. Optional encrypted export and biometric lock.
5. Permanent production application id with a documented migration.
6. Install the 4.0.0 APK on a real phone and read the new copy out loud - twelve-word lines that
   work on paper are the ones that need cutting, and the rotation only proves itself over a week.

Built with Care, Openeye by Dharmendra.