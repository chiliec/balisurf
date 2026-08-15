# store-assets

Single source of truth for the Play listing. `fastlane/Fastfile`'s
`stage_play_metadata` copies these into the `supply` `<lang>/` layout at release
time (into a gitignored `fastlane/play-metadata/`).

```
metadata/android/en-US/
  title.txt                 app name (<= 30 chars)
  short_description.txt      <= 80 chars
  full_description.txt       <= 4000 chars
  changelogs/default.txt     fallback release notes
  changelogs/<code>.txt      per-versionCode release notes
icons/play-icon-512.png      512x512  (PLACEHOLDER)
feature-graphic/play-feature-1024x500.png  1024x500  (PLACEHOLDER)
screenshots/android/*.png    phone screenshots, <=2:1 side ratio  (PLACEHOLDERS)
```

The images are generated placeholders (ocean-blue, labelled) so the pipeline is
runnable end to end before real captures exist. **Replace all of them and confirm
the text before a public launch** — see `docs/release-android.md`.
