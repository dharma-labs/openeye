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

If the MR shows a red pipeline with **no jobs at all**, that is not the recipe. A GitLab account that
was just created cannot run shared-runner CI ("Identity verification is required in order to run CI
jobs"), so fdroiddata's pipeline is created and dies instantly. The MR still reaches the maintainers,
who run the build themselves.

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
> - The APK attached to the `v5.1.0` tag is that clean `assembleRelease` output, zipaligned and
>   signed, so comparing against it should pass. Reproducible builds are not switched on in this
>   recipe yet - `AllowedAPKSigningKeys` can go in on the next release.
>
> I am the author and the maintainer of the upstream repository.

## Checked before submitting

`git clone --branch v5.1.0` into a clean directory with only `ANDROID_HOME` and `JAVA_HOME` set, then
`./gradlew :app:assembleRelease`, builds and passes `lintVitalRelease` - that is the same shape of
environment the F-Droid builder uses, and it has no `local.properties` in it.

The APK published in both release repos is the output of that clean build, zipaligned and signed
with the release key (cert SHA-256
`e6e867f255baed04d6b35289b5533035765d72d8fae007626218b81f6922d8c5`). Building the tag twice from
scratch gives the same unsigned bytes, so the only difference between the F-Droid APK and the
GitHub one is the signature.

That is worth saying in the MR, but the recipe above does not claim reproducible builds yet: doing
that needs `AllowedAPKSigningKeys` with the fingerprint of the release certificate, and it is a
second change to a file a maintainer has to review. Add it once 5.1.0 is in the archive.

One caveat for anyone rebuilding: AGP writes `META-INF/version-control-info.textproto` into the APK
with the commit it saw. Building from a dirty working tree changes that file, so compare against a
clean checkout of the tag, not against a build of your own branch.

## After it is accepted

F-Droid re-signs every APK with its own key, so the store build and the GitHub APK in
[releases](https://github.com/dharma-labs/openeye/releases) cannot update each other. The install
page and the changelog both say so; keep saying it, because the failure is a silent
`INSTALL_FAILED_UPDATE_INCOMPATIBLE` and people read that as the app breaking.
