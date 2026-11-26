# Merge request description for fdroiddata

This is the text to paste into the MR body. It follows `.gitlab/merge_request_templates/Default.md`
from `fdroid/fdroiddata` on purpose - the first submission did not use the template and got closed for
that reason. Do not rewrite it into prose. The MR title must be `New app: Openeye`.

Before pasting, drag the six screenshots from
`fastlane/metadata/android/en-US/images/phoneScreenshots/` into the description box, because the
template asks for screenshots in the MR itself and not only upstream.

---

## Abstract for new app inclusion

Openeye is a local-first digital wellbeing app for short-video feeds (Instagram Reels, YouTube
Shorts, TikTok). It does three things:

- it measures the feed you are actually looking at - time, clips watched, swipes - using the
  accessibility node tree plus Android's own usage stats, and shows that number on top of the feed
  while you are in it, not in a dashboard you open afterwards
- it covers the Reels / Shorts / Explore button in the apps you tell it to, and can hold the app
  behind a short interrupt screen that quotes your own goal and your own if-then plan back at you
- it puts a quiet `today 42m` in the corner of the screen and a home-screen widget with today's
  number and goal bar

Permissions: it declares no `INTERNET` permission at all, so nothing can leave the device. It needs
usage access (to read the numbers Android already keeps) and accessibility (to know which app is in
front and to draw the readout over it). No account, no ads, no analytics, no server.

Screenshots: (drag the six fastlane phone screenshots in here)

### Why I wrote it, and what it does that the existing apps in the category do not

I wrote it because I kept checking my own screen time after the fact and changing nothing. Every
existing open-source option in this space either (a) blocks whole apps via device-owner /
accessibility and so needs a scary set of privileges, (b) shows a dashboard after the session, or
(c) requires an account, a server or a Play Services dependency to read usage stats.

What is unique here:

- the measurement is per-feed, not per-app. It counts clips and swipes inside Reels/Shorts while you
  are watching, which no other app in the repo does without a server
- the intervention is at the moment of the urge and it quotes the number *you* set, in your own words
  (the if-then plan), instead of a rule the app invented
- it deliberately avoids streaks, badges and guilt copy, because a broken streak is the main reason
  people quit these apps. Nothing in the interface is red at you
- it is ~1.6 MB, has no `INTERNET` permission, and every number on screen was measured, never
  estimated or fabricated

### Policy

* [x] The app complies with the [inclusion policy](https://f-droid.org/docs/Inclusion_Policy). _Pay special attention to Quality point 6._
  Quality 1 (actively maintained, author commits to ongoing maintenance): yes, I am the sole author
  and this is my main project.
  Quality 2 (functional, implements everything the description says): every claim in the fastlane
  description is a feature that is in the 5.1.0 APK and was tested on device.
  Quality 3 (native when wrapping a website): not a web wrapper, pure Kotlin + Jetpack Compose.
  Quality 4 (useful, not a demo): it is the real thing, sideloaded and used daily.
  Quality 5 (unique value): see the section above.
  **Quality 6 (actively developed for at least 3 months): not yet met, and that is the honest
  blocker.** The source became public on 2026-11-25 and the history here is 11 commits since
  2026-11-24. None of the three exceptions apply today - the app has a handful of users (9 APK
  downloads from the GitHub release, no Play Store listing yet), I have no other app on F-Droid, and
  it is not an event-only app. I would rather say so than argue it. I am keeping the repository
  active and will re-request once the public history reaches 3 months, i.e. on or after 2027-02-24,
  with the newest release in the recipe. If a small but real user base would satisfy the first
  exception instead, tell me what evidence you want and I will post it.
* [x] The original app author has been notified (and does not oppose the inclusion). I am the author.
* [x] The upstream repo contains fastlane metadata: `fastlane/metadata/android/en-US/` with
  `title.txt`, `short_description.txt`, `full_description.txt`, `images/icon.png` and
  `images/phoneScreenshots/1.png`-`6.png`, plus `changelogs/17.txt`. The `en-US` locale is included.

### Docs

* [x] I have read CONTRIBUTING.md.
* [x] Metadata follows the templates in `templates/`.
* [x] I have read the Build Metadata Reference and the Quick Start Guide.

### Merge Request Setup

* [x] The title follows the "New app: app name" format: `New app: Openeye`.
* [x] The fork is public and the branch `add-openeye` is not protected.
* [x] No related fdroiddata or RFP issue exists for this app; this is a fresh submission.
* [x] One app in one MR.

### Metadata

* [x] Metadata is in `metadata/io.github.dharma_labs.openeye.yml`, valid YAML, LF line endings, and
  that file is the only change in the MR - no summary/description/images here, they are upstream.
* [x] Releases are tagged (`v5.1.0` on the commit above) and auto update is enabled
  (`AutoUpdateMode: Version`, `UpdateCheckMode: Tags`).
* [x] Issue tracker and author name are in the recipe; I watch that repo, so bug reports reach me.
* [x] An `AuthorName` is set. I am using my given name, which is also the name on the source repo and
  in the app's own credits screen. I have no domain of my own, hence the `io.github.dharma_labs.*` id
  rather than a domain-based one.
* [x] No external repos are used, so there are no submodules and no srclibs.
* [ ] Reproducible Builds are **not** enabled yet. Reason: I have no third-party build logs
  (BTLog) yet and I have not verified that a build from the tag reproduces byte-for-byte on a
  different machine - the release variant is R8 + resource shrinking, and I know R8 output is not
  guaranteed stable across JDK/minor-toolchain differences. Please sign with the F-Droid key for now.
  I will add `AllowedAPKSigningKeys` as soon as I have BTLogs proving reproducibility.
* [x] No ABI splits: the single APK is about 1.6 MB and contains no native code.
* [x] Only the latest version is in the metadata, and no version is disabled.
* [x] `commit` is the full hash, not the tag name.

### Pipeline

* [ ] Pipelines cannot run from this account: GitLab answers "Identity verification is required in
  order to run CI jobs", so the MR pipeline is created and fails instantly with zero jobs. Per the
  template's own instruction I am not paying GitLab for anything - please trigger the CI when the
  MR is picked up.
* [x] Reports tab: nothing to fix yet, since no job ran.

---------------------

/label ~"New App"
