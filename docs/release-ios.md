# iOS release runbook

App Store Connect app id **6801866165**, bundle `cx.viz.balisurf`, version 1.0.0.
All auth is the ASC API key (`AuthKey_948K3FKL2H.p8` in the repo root, gitignored)
plus `ASC_ISSUER_ID` — locally put both in `fastlane/.env`; CI reads GitHub secrets.

## Pipelines

| What | How |
|---|---|
| Build + TestFlight | `ios-testflight.yml` (macOS runner, gym + pilot) |
| Listing / submit | `ios-appstore.yml` — dispatch-only, `-f lane=release` or `-f lane=submit` (Linux, API-only) |

Workflow-file commits must be pushed over SSH (`git push git@github.com:chiliec/balisurf.git main`);
the https token lacks the `workflow` scope.

## Submission gates

deliver refuses to submit until all of these are set. Run the scripts with
`PATH=/opt/homebrew/opt/ruby/bin:$PATH bundle exec ruby scripts/<x>.rb`
(system ruby 2.6 can't load the bundle) and `ASC_ISSUER_ID` + `ASC_APP_ID` in env.

- **Age rating 4+** — `scripts/asc_age_rating.rb` (idempotent). DONE.
- **Content rights** — `USES_THIRD_PARTY_CONTENT` (Open-Meteo forecast data,
  Copernicus Sentinel-derived reef overlays). DONE.
- **Pricing: Free** — `scripts/asc_pricing_free.rb` (POSTs an appPriceSchedule with
  the USA free price point; spaceship has no support for this). DONE.
- **App Privacy: "no data collected"** — web UI only, no public ASC API. Matches
  `PRIVACY.md`: everything (session logs, forecasts) stays on device.
- **Listing metadata + screenshots** — `-f lane=release`. DONE.

`scripts/asc_state.rb` is a read-only dump of all of the above (plus the build
attached to the version) — run it before submitting.

## Submit

Once App Privacy is published:

```
gh workflow run ios-appstore.yml -f lane=submit
```

The submit lane attaches the latest valid build (build 1 is `VALID` /
`APP_STORE_ELIGIBLE`, `usesNonExemptEncryption=false`) and declares export
compliance + no-IDFA, then submits 1.0.0 for review. Release is manual.
