# Submitting Openeye to F-Droid

The recipe in this folder is the file that goes into `fdroiddata`. It is kept here so the app
repository carries its own packaging notes; the copy that gets reviewed lives in the merge request.

## Outcome of the first submission

MR [fdroiddata!59168](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/59168) was closed by
Licaon_Kter on 2026-11-26: *"template not used ... app is not ready, see Policy Quality point 6, go
and correct the things and again request."* Both parts were right:

- the body was my own prose instead of `Default.md` from `fdroiddata`, so none of the checklist could
  be reviewed. The required title is also `New app: Openeye`, not `Add Openeye ...`.
- the recipe itself had two defects that checklist calls out: `commit: v5.1.0` (must be a full hash)
  and no `AuthorName`. `Categories: Internet` was also wrong - the schema has a
  `Digital Wellbeing` category. All three are fixed here.
- **Quality point 6 is a real block, not paperwork**: *"the application should have been actively
  developed for at least 3 months"*, unless it has proven users, the author already maintains apps
  on F-Droid, or it is event-only. None of those is true today (public history started 2026-11-24).
  Re-request on or after **2027-02-24**, with the then-current release.

## Steps

1. Sign in at [gitlab.com](https://gitlab.com) and fork
   [fdroid/fdroiddata](https://gitlab.com/fdroid/fdroiddata). Make sure the **fork is public** and the
   branch is **not protected** (Settings → Repository → Protected branches).
2. In your fork, add exactly one file: `metadata/io.github.dharma_labs.openeye.yml`, with the contents
   of [io.github.dharma_labs.openeye.yml](io.github.dharma_labs.openeye.yml), **LF** line endings.
   Nothing else in the MR - descriptions, icons and screenshots belong upstream in `fastlane/`.
3. Commit it on its own branch, e.g. `add-openeye`.
4. Open a merge request against `master` of `fdroid/fdroiddata` titled `New app: Openeye`, using
   [MR-DESCRIPTION.md](MR-DESCRIPTION.md) verbatim and dragging in the six phone screenshots.
5. Wait for CI. If it fails, the log says which step; paste it into the MR. Do not force-push over a
   failing build - add a commit.

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

It lives in [MR-DESCRIPTION.md](MR-DESCRIPTION.md) because it has to match the `Default.md` template
in `fdroiddata`, checkboxes and all. Do not compress it back into a paragraph - that is what got the
first MR closed.

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
