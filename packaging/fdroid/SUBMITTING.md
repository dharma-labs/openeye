# Submitting Openeye to F-Droid

The recipe in this folder is the file that goes into `fdroiddata`. It is kept here so the app
repository carries its own packaging notes; the copy that gets reviewed lives in the merge request.

## Steps

1. Sign in at [gitlab.com](https://gitlab.com) and fork
   [fdroid/fdroiddata](https://gitlab.com/fdroid/fdroiddata).
2. In your fork, add one file: `metadata/io.github.dharma_labs.openeye.yml`, with the exact contents
   of [io.github.dharma_labs.openeye.yml](io.github.dharma_labs.openeye.yml).
3. Commit it on its own branch, e.g. `add-openeye`.
4. Open a merge request against `master` of `fdroid/fdroiddata` using the text below.
5. Wait for the CI build. If it fails, the log says which step; paste it into the MR and the
   maintainers will pick it up. Do not force-push over a failing build - add a commit.

If `fdroid server` tooling is installed locally, this checks the recipe before the MR:

```bash
fdroid readme --metadata --config config.yml   # renders the store page
fdroid build io.github.dharma_labs.openeye     # runs the build in docker
```

## Merge request text

> **Add Openeye (io.github.dharma_labs.openeye)**
>
> Source: https://github.com/dharma-labs/openeye (GPL-3.0-only, licence file at the repo root)
> Upstream release tag: `v5.1.0`, versionCode 17
>
> A local-first digital wellbeing app for short-video feeds. It counts feed time and estimated clips
> from usage events and the accessibility node tree, covers the Reels/Shorts/Explore buttons, and
> shows the number on the home screen and in the corner of the display.
>
> - No `INTERNET` permission in the manifest, so no anti-feature is needed. No backend, no analytics
>   SDK, no Firebase, no push.
> - All dependencies are AndroidX / JetBrains artifacts under Apache-2.0 from Google Maven and Maven
>   Central. Nothing non-free.
> - `fastlane` metadata, including `icon.png` and six device screenshots, is in the upstream repo at
>   `fastlane/metadata/android/en-US/`, so `AntiFeatureText` and friends are not needed.
> - The build is the R8 release variant, which is the variant that has been run on a device end to
>   end. The output is `app/build/outputs/apk/release/app-release-unsigned.apk`.
> - Reproducible builds are not enabled yet; the recipe is a plain gradle build. Happy to move to
>   `gradle: reproduce` once the signing config is in the build file rather than applied out of
>   band.
>
> I am the author and the maintainer of the upstream repository.

## After it is accepted

F-Droid re-signs every APK with its own key, so the store build and the GitHub APK in
[releases](https://github.com/dharma-labs/openeye/releases) cannot update each other. The install
page and the changelog both say so; keep saying it, because the failure is a silent
`INSTALL_FAILED_UPDATE_INCOMPATIBLE` and people read that as the app breaking.
