# Releasing BaliSurf to Google Play (Android)

This pipeline is ported from `chiliec/slovo`. It builds a signed AAB and uploads
it to Play via fastlane `supply`, degrading gracefully: no signing secret →
unsigned artifact only; no Play key → build-only, no upload. iOS is wired
separately: `iosApp/` + the `platform :ios` lanes in the Fastfile (see its header
for the signing model) + `.github/workflows/ios-testflight.yml`.

**Verified locally on 2026-08-20**: upload keystore generated, `bundleRelease`
produces an AAB that `jarsigner -verify` accepts under
`CN=BaliSurf, O=viz.cx, C=US` (SHA1 `06:4C:25:F8:99:0F:DD:EF:4B:AD:53:4E:BC:27:91:03:7D:6D:C0:16`),
`play_stage` maps the real store assets. Everything below the Play Console line
is still untested — there is no app record yet.

## What only a human can do (blockers)

1. **Google Play Console account** — $25 one-time, identity verification (can take
   days). Required before anything reaches the store.
2. **Play service account JSON** (Release-manager role) — created in Play Console
   *after* the account exists (§A below).
3. **Upload keystore** — you generate it locally and keep it safe (§B).

Until those exist you can still verify the pipeline: `play_stage` and `bundle`
need no credentials, and CI builds the AAB artifact on every run.

## Verify with no account (do this first)

```bash
bundle install
bundle exec fastlane android play_stage   # proves store-assets -> supply mapping
bundle exec fastlane android bundle        # builds an (unsigned) AAB on a Mac
```
Or in GitHub: **Actions → Android Play → Run workflow → track: none** — builds the
AAB and uploads it as an artifact, no secrets needed.

## A. Play service account key (for upload lanes / CI)

The upload lanes (`play_internal`, `play_closed`, `play_listing`, `play_promote`)
and CI authenticate with a **service account JSON**, never a password. Create once,
AFTER the Play Console account and the `cx.viz.balisurf` app record exist:

1. Play Console → **Setup → API access** → link/create a Google Cloud project, then
   **Create new service account** (opens Google Cloud IAM).
2. Google Cloud → the new service account → **Keys → Add key → JSON** → download.
3. Play Console → **API access → grant access** with the **Release manager** role.
4. Install it:
   - Local: save as `play-service-account.json` at the repo root (gitignored).
   - CI: base64 into the `PLAY_JSON_KEY` secret (§C).

```bash
mv ~/Downloads/<downloaded>.json ./play-service-account.json
bundle exec fastlane run validate_play_store_json_key json_key:play-service-account.json
```
Permission propagation is not instant — a fresh account can 401 for a few minutes.

## B. Upload keystore

```bash
keytool -genkeypair -v -keystore balisurf-upload.jks -alias balisurf \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -dname "CN=BaliSurf, O=viz.cx, C=US"

cp keystore.properties.example keystore.properties   # then edit in your passwords
```
With Play App Signing enabled this is only the UPLOAD key — resettable via the Play
Console if lost. Still: **back up the .jks and passwords.** Both files are
gitignored.

## C. GitHub Actions secrets (for CI upload)

Wire from a checkout that has the keystore + JSON on hand:

```bash
gh secret set ANDROID_KEYSTORE_BASE64   --repo chiliec/balisurf < <(base64 -w0 balisurf-upload.jks)
gh secret set ANDROID_KEYSTORE_PASSWORD --repo chiliec/balisurf --body '<store password>'
gh secret set ANDROID_KEY_ALIAS         --repo chiliec/balisurf --body 'balisurf'
gh secret set ANDROID_KEY_PASSWORD      --repo chiliec/balisurf --body '<key password>'
gh secret set PLAY_JSON_KEY             --repo chiliec/balisurf < <(base64 -w0 play-service-account.json)
gh secret list --repo chiliec/balisurf
```
`base64 -w0` is GNU/Linux; macOS/BSD uses `base64 -i <file>`. The workflow rewrites
`keystore.properties` with `storeFile=$RUNNER_TEMP/balisurf-upload.jks` — the
decoded filename must match, which it does.

## First upload (partly manual)

`supply` cannot create the app record or enrol Play App Signing — do those once in
the console. Then:

```bash
# dry run: Play validates and discards, nothing published
PLAY_VALIDATE_ONLY=1 bundle exec fastlane android play_internal
# real upload to the internal track as a draft
bundle exec fastlane android play_internal
```
Review the draft release in the console and publish it to testers.

## Production access note

Personal Play accounts created after 2023-11-13 need **12 testers opted in to a
CLOSED track for 14 consecutive days** before production is unlocked. `play_closed`
starts that clock — but a `draft` release is invisible to testers, so publish it
(or pass `PLAY_RELEASE_STATUS=completed`) before the days count.

## Store art

`tools/gen-art.swift` renders every raster asset — the adaptive launcher
foreground + legacy/round icons at all five densities, the Play 512 icon and the
1024×500 feature graphic — from the Tropic Clean palette using CoreGraphics
(this machine has no ImageMagick/PIL and needs none):

```bash
swift tools/gen-art.swift
```

The mark is code, not a binary to hand-edit; re-run it after a palette change and
keep `ic_launcher_background` in `androidMain/res/values/colors.xml` in step.

Screenshots are captured on the `slovo_qa` AVD (Pixel 6, 1080×2400) headless:

```bash
emulator -avd slovo_qa -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect &
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
adb shell am start -n cx.viz.balisurf/.MainActivity
adb exec-out screencap -p > shot.png          # navigate with `adb shell input tap/swipe`
sips -c 2160 1080 --cropOffset 150 0 shot.png --out 01-name.png
```

That last crop is not cosmetic: Play rejects a side ratio above 2:1 and 1080×2400
is 2.22:1. Cropping 150px off the top also drops the status bar. `supply` picks
the files up in sorted filename order, so keep the `NN-` prefixes.

## Pre-upload checklist

- [ ] Play Console account created, `cx.viz.balisurf` app record created, Play App
      Signing enrolled. **← the only blocker left**
- [ ] Service account JSON created (Release manager), `play-service-account.json`
      in place — §A.
- [x] Upload keystore generated + `keystore.properties` filled — §B.
- [ ] (CI only) `ANDROID_KEYSTORE_*` + `PLAY_JSON_KEY` GitHub secrets set — §C.
- [x] Real store images + text in `store-assets/`.
- [ ] `PLAY_VALIDATE_ONLY=1` dry run passed.
