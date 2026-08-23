# Satellite-Derived Bathymetry (SDB) for Shallow, Clear Water from Free Sentinel-2 Imagery

**Scope:** empirical passive-optical bathymetry for shallow (< ~15–20 m), clear water (reefs, coastal shelves, lagoons), using freely available Sentinel-2 MSI imagery. This report grounds the *why* and the *published accuracy numbers* behind a Stumpf & Holderied (2003) log-ratio pipeline. Primary sources are cited inline. Where a claim could not be verified against a primary full text, it is flagged explicitly.

**Author of report:** compiled for the balisurf SDB pipeline, 2026-08-21.

**Verification note on sources:** The three foundational papers (Stumpf & Holderied 2003; Lyzenga 1978; Lyzenga et al. 2006) are paywalled. Their DOIs, titles, authors, venues and — where available — abstracts were verified via the publisher DOI pages and the Semantic Scholar / Crossref metadata APIs. Full journal text was **not accessed** for those three; equations reproduced below are the standard published forms as restated in the *open-access* peer-reviewed literature (Caballero et al. 2019; Traganos et al. 2018), which are explicitly cited at each equation. This is called out so nothing is taken on trust.

---

## 1. Core algorithms

### 1.1 Stumpf & Holderied (2003) — log-ratio (band-ratio) method

**Citation:** Stumpf, R.P., Holderied, K., Sinclair, M. (2003). "Determination of water depth with high-resolution satellite imagery over variable bottom types." *Limnology and Oceanography* 48(1, part 2): 547–556. DOI: [10.4319/lo.2003.48.1_part_2.0547](https://doi.org/10.4319/lo.2003.48.1_part_2.0547). *(Metadata verified via DOI + Semantic Scholar; full text paywalled/Cloudflare-blocked, not accessed.)*

**Equation (relative depth index, "pSDB"):**

```
                 ln( n · R_w(λ_blue) )
pSDB  =  ------------------------------------
                 ln( n · R_w(λ_green) )
```

where `R_w` is water-leaving/subsurface reflectance, `λ_blue` ≈ B02 and `λ_green` ≈ B03, and `n` is a fixed large constant (commonly 1000) chosen so both logarithms are positive and the ratio stays stable as reflectance → small. Depth is then obtained by a **linear calibration** `Z = m₁·pSDB − m₀` (see §3). The form and the `n=1000` convention are reproduced in the open-access restatements by Caballero et al. (2019, *Remote Sensing* 11(6):645, [doi:10.3390/rs11060645](https://doi.org/10.3390/rs11060645)) and Traganos et al. (2018, *Remote Sensing* 10(6):859, [doi:10.3390/rs10060859](https://doi.org/10.3390/rs10060859)).

**Physical rationale:** light attenuates ~exponentially with water-column path length (Beer–Lambert), and the attenuation coefficient differs by wavelength — blue penetrates deepest in clear water, green less so. As depth increases, *both* band reflectances fall, but the green falls faster; the **ratio of the two logarithms therefore changes monotonically with depth**. Because both bands respond to a change in bottom brightness in the same direction, taking their ratio largely cancels the bottom-albedo term, leaving a signal dominated by depth (see §2).

**Inputs (Sentinel-2):** B02 blue (center **0.490 µm / ~490 nm**) and B03 green (center **0.560 µm / ~560 nm**), both native 10 m. *(Wavelengths verified live from the Earth Search STAC `sentinel-2-l2a` collection metadata: blue `center_wavelength: 0.49`, green `center_wavelength: 0.56`.)* NIR (B08, ~0.842 µm) is used only for masking/glint, not in the ratio.

**Stated limits:** the method was designed for **optically shallow, clear water** and returns a *relative* index that must be calibrated to metres against control depths. Stumpf et al. reported it works to greater depths than the earlier linear (Lyzenga) approach because the ratio is less sensitive to bottom type; the practical clear-water ceiling reported across the literature is ~**15–20 m** for Sentinel-2-class sensors (see §5, and Caballero et al. 2019, §1.2 numbers below).

### 1.2 Lyzenga (1978, 1985) — linear / log-linear multiband method

**Citations:**
- Lyzenga, D.R. (1978). "Passive remote sensing techniques for mapping water depth and bottom features." *Applied Optics* 17(3): 379–383. DOI: [10.1364/AO.17.000379](https://doi.org/10.1364/AO.17.000379). Verified abstract: *"Ratio processing methods are reviewed, and a new method is proposed for extracting water depth and bottom type information from passive multispectral scanner data. Limitations of each technique are discussed, and an error analysis is performed using an analytical model for the radiance over shallow water."* *(Abstract accessed; full text login-gated, not accessed.)*
- Lyzenga, D.R. (1985). "Shallow-water bathymetry using combined lidar and passive multispectral scanner data." *Int. J. Remote Sensing* 6(1): 115–125. DOI: [10.1080/01431168508948428](https://doi.org/10.1080/01431168508948428). *(Metadata verified; full text not accessed.)*

**Equation (log-linearised reflectance → depth):** Lyzenga linearises the radiative transfer by taking logs of deep-water-corrected reflectance. For each band *i*:

```
X_i = ln( R_w,i − R_∞,i )
```

where `R_∞,i` is the deep-water (optically-deep) reflectance for band *i* (the asymptote where the bottom is invisible). Because `X_i` is then approximately linear in depth *Z*, depth is estimated as a **multiband linear combination**:

```
Z = a₀ + Σ_i a_i · X_i
```

with coefficients `a_i` fit by regression against known depths. The single-band version reduces to `Z ∝ ln(R_w − R_∞)`. *(Standard form as restated in Traganos et al. 2018 and Caballero et al. 2019, both open access and cited above.)*

**Physical rationale:** subtracting the deep-water asymptote removes the atmosphere + water-surface + deep-water-scattering offset, leaving a bottom-plus-column term that is exponential in depth; the log makes it linear so ordinary least squares applies. Using two or more bands lets the fit partially separate depth from bottom-type variation.

**Inputs (Sentinel-2):** typically B02 (blue) + B03 (green), optionally B04 (red) for the shallowest water; deep-water `R_∞` estimated from an optically-deep region of the same scene.

**Stated limits:** more sensitive to **bottom-albedo variation** than the ratio method, because a single-band log term still contains the bottom-reflectance factor; performs best where bottom type is relatively uniform. This bottom-type sensitivity is precisely the weakness Stumpf & Holderied (2003) set out to reduce with the ratio.

### 1.3 Lyzenga, Malinas & Tanis (2006) — physically-based optimization

**Citation:** Lyzenga, D.R., Malinas, N.P., Tanis, F.J. (2006). "Multispectral bathymetry using a simple physically based algorithm." *IEEE Transactions on Geoscience and Remote Sensing* 44(8): 2251–2259. DOI: [10.1109/TGRS.2006.872909](https://doi.org/10.1109/TGRS.2006.872909). *(Metadata/venue verified via Semantic Scholar + IEEE Xplore; full text paywalled, not accessed.)*

**What it adds:** extends the 1978/1985 log-linear model into a **physically-based multiband estimator** that jointly solves for depth *and* bottom reflectance, still in a computationally simple (essentially linear-in-log) form. The depth estimate is again a linear combination of log-transformed, deep-water-corrected bands:

```
Z = h₀ + Σ_i h_i · ln( R_w,i − R_∞,i )
```

but the coefficients `h_i` are derived from the water's optical attenuation coefficients (a physical basis) rather than purely empirically, improving transferability across bottom types. *(Form as restated in the open-access SDB literature cited above; the exact derivation of `h_i` is in the paywalled paper and was not read.)*

**Inputs / limits:** multiple visible bands (blue, green, red); same optically-shallow, clear-water constraint. Its advantage over the plain ratio is better bottom-type discrimination when ≥3 usable bands exist.

### 1.4 Modern ML / random-forest / neural approaches (brief)

A substantial recent body of peer-reviewed work replaces the fixed linear/ratio calibration with **machine-learning regressors** — random forests, gradient boosting, support-vector regression, and shallow neural nets — trained on the same visible bands (plus derived indices) against LiDAR/sounding depths.

**Citation:** Sagawa, T., Yamashita, Y., Okumura, T., Yamanokuchi, T. (2019). "Satellite Derived Bathymetry Using Machine Learning and Multi-Temporal Satellite Images." *Remote Sensing* 11(10):1155. DOI: [10.3390/rs11101155](https://doi.org/10.3390/rs11101155). Random forest over **135 Landsat-8 images** (not Sentinel-2) across five areas in Google Earth Engine; reported **RMSE 1.41 m over 0–20 m**. *(Open access, abstract verified.)*

These methods can capture non-linearity (e.g. bottom-type mixtures) that the log-ratio ignores, but they **need more and better-distributed training depths** — Sagawa et al. use "a large amount of training bathymetry data" — risk overfitting to one scene/site, and lose the ratio method's physical interpretability. For a small, cost-conscious operation with sparse control points (the balisurf case), the Stumpf ratio + linear calibration remains the robust default; ML is an upgrade path only once dense reference depths exist. *(Training-data appetite and the RMSE figure: Sagawa et al. 2019. Overfitting/interpretability trade-offs are the common consensus across the SDB literature — flagged as a synthesis, not a single-source quote.)*

Note that Traganos et al. (2018) — cited elsewhere in this report — is **not** an ML paper: its four benchmarked algorithms are the classical empirical ones. Don't read its R²=0.90 as an ML result.

---

## 2. Why blue/green, and why the log-ratio is robust

- **Differential attenuation:** in clear (Case-1) water, the diffuse attenuation coefficient K is lowest in the blue (~445–490 nm) and rises through the green into the red. Blue light therefore reaches the bottom and returns from greater depths than green; red is extinguished within a few metres. Blue and green are the only Sentinel-2 visible bands with usefully different, *both-non-trivial* bottom returns over the 0–20 m range — hence the pairing. *(Wavelength/attenuation ordering is standard ocean-optics; the operational choice of B02/B03 for Sentinel-2 SDB is documented in Caballero et al. 2019 and Traganos et al. 2018.)*
- **Albedo robustness of the ratio:** a brighter bottom raises reflectance in *both* blue and green by roughly the same multiplicative factor. In the ratio `ln(n·R_blue)/ln(n·R_green)`, a common multiplicative change to both numerator and denominator's arguments largely cancels, so the index tracks **depth** far more than **bottom type**. Stumpf & Holderied (2003) introduced the ratio specifically to overcome the bottom-albedo sensitivity of Lyzenga's single-band linear form. *(Attributed to Stumpf & Holderied 2003; mechanism restated in Caballero et al. 2019.)*
- **Approximate linearity with depth:** because each band's log reflectance is ~linear in depth (Beer–Lambert), the ratio of the two logs is a smooth, monotonic, near-linear function of depth over the clear-water shallow range — which is why a **single linear fit** to metres works well (see §3). Linearity degrades as depth approaches the saturation ceiling (§5).

---

## 3. Calibration to metres

**Model:** `Z = m · pSDB + c` — an ordinary least-squares line from the relative index to true depth. Stumpf & Holderied's original form is `Z = m₁·pSDB − m₀`; the two are equivalent.

**Control-point sources (best first):**
1. **In-situ soundings / echo-sounder / dive-computer tracks** located *on the feature of interest* (the reef). A handful of on-reef points beats a dense but coarse grid because they constrain the model where it matters.
2. **Airborne LiDAR bathymetry** where it exists — the standard *validation* reference in the literature (used as ground truth by Caballero et al. 2019).
3. **GEBCO** as a bootstrap only — global but ~450 m grid (§7); it cannot resolve reef shape, so it yields a plausible absolute *offset/scale* but not reef detail.

**Typical reported accuracy (attributed):**

| Study (primary, peer-reviewed) | Sensor | Depth range | Reported error / fit |
|---|---|---|---|
| Caballero et al. 2019, *Remote Sensing* 11(6):645 ([doi](https://doi.org/10.3390/rs11060645)) | Sentinel-2A MSI, ratio model | 0–18 m, West Palm Beach, low turbidity | **RMSE ≈ 0.58 m** |
| Caballero et al. 2019 (same) | Sentinel-2A MSI | 0–5 m, Key West, low turbidity | **RMSE ≈ 0.22 m** |
| Traganos et al. 2018, *Remote Sensing* 10(6):859 ([doi](https://doi.org/10.3390/rs10060859)) | Sentinel-2, Aegean, to ~17 m | training / validation | training **R²=0.79, RMSE=1.39 m**; validation **R²=0.90, RMSE=1.67 m** |

These bracket the realistic expectation: **sub-metre RMSE in very clear shallow (0–5 m) water, ~0.5–1.7 m RMSE out to ~15–18 m**, degrading with turbidity and depth. *(All numbers quoted verbatim from the two open-access papers' abstracts, verified.)*

**Sample-size guidance:** the ratio model needs only **two calibration parameters** (m, c), so it is fittable from very few points — Caballero et al. (2019) stress it "only requires two calibration parameters for vertical referencing using available chart data." In practice, use **enough points to span the full depth range and multiple bottom types** and to leave an independent hold-out for validation; ML methods (§1.4) need far more. Warn when the calibration correlation is weak (a corr < ~0.6 indicates SDB is unreliable at that site). *(Two-parameter claim: Caballero et al. 2019. The "span the range + hold-out" guidance is standard regression practice, flagged as method convention, not a single citation.)*

---

## 4. Preprocessing

- **Atmospheric correction:** work from **Sentinel-2 Level-2A** surface-reflectance products (bottom-of-atmosphere), produced by ESA's **Sen2Cor** processor; the L2A product also ships the Scene Classification Layer. For water specifically, aquatic-tuned processors — notably **ACOLITE** (RBINS) — are widely used in the SDB literature because Sen2Cor is optimised for land and can leave residual glint/haze over water. *(L2A/Sen2Cor/SCL provenance: ESA Sentinel-2 product documentation, [sentiwiki.copernicus.eu/web/s2-products](https://sentiwiki.copernicus.eu/web/s2-products). ACOLITE's use for coastal/aquatic S2 is documented in the peer-reviewed SDB literature, e.g. Caballero et al. 2019.)*
- **Sunglint correction:** the standard method is **Hedley, Harborne & Mumby (2005)**, "Simple and robust removal of sun glint for mapping shallow-water benthos," *Int. J. Remote Sensing* 26(10):2107–2112, DOI [10.1080/01431160500034086](https://doi.org/10.1080/01431160500034086) — regress each visible band against a NIR band over a glinted-but-deep-water region and subtract the glint-correlated component (NIR is ~fully absorbed by water, so any NIR signal over water is surface glint). *(Metadata/title verified; full text not accessed.)*
- **Jan-2022 processing baseline −1000 radiometric offset (ESA):** From **Processing Baseline 04.00, operational since 25 January 2022**, Sentinel-2 L1C/L2A products shifted their dynamic range by a band-dependent additive constant (`BOA_ADD_OFFSET`, value −1000 for the reflectance encoding) so that negative surface reflectances over very dark surfaces can be encoded. Users must recover true reflectance as:

  ```
  L2A_SR_i = (L2A_DN_i + BOA_ADD_OFFSET_i) / QUANTIFICATION_VALUE
  ```

  with the per-band offset and quantification value in the product metadata (`General_Info/Product_Image_Characteristics`). **Consequence for SDB:** mixing pre- and post-2022-01-25 scenes without applying the offset corrupts any composite and any log-ratio. *(Directly from ESA/Copernicus documentation: [sentiwiki.copernicus.eu/web/s2-products](https://sentiwiki.copernicus.eu/web/s2-products) — "Following the introduction of PB 04.00 (25 January 2022), the dynamic range of the Level-2A products is shifted by a band-dependent constant: BOA_ADD_OFFSET"; PB 04.00 date confirmed on [sentiwiki.copernicus.eu/web/s2-processing](https://sentiwiki.copernicus.eu/web/s2-processing).)*
- **Water masking:** use the **Scene Classification Layer (SCL), class 6 = WATER** as the primary water mask. SCL is a 20 m product (upsample nearest-neighbour to the 10 m grid — never interpolate a class map). AND-gate it with a **NIR-dark test** (B08 below a small DN threshold) to drop foam, whitecaps and residual sunglint that SCL may mislabel. Avoid a blind NIR percentile threshold — it forces a fixed "water" fraction even on all-land tiles. *(SCL provenance/classes: ESA L2A documentation, [sentiwiki.copernicus.eu/web/s2-products](https://sentiwiki.copernicus.eu/web/s2-products), which lists SCL among L2A outputs; class-6=water is the ESA SCL definition. NIR-dark AND-gate is a pipeline convention, flagged as method not citation.)*
- **Cloud filtering:** query STAC by `eo:cloud_cover` and prefer the lowest-cloud scene (§6); optionally use SCL cloud classes (8/9/10) or the product cloud masks to drop residual cloud/cirrus pixels.

---

## 5. Hard limits and failure modes (where SDB is NOT appropriate)

- **Signal saturation past ~15–20 m:** in the clearest water, below roughly 15–20 m the bottom return falls to the deep-water noise floor and the index stops responding to depth — deeper values are unrecoverable. Caballero et al. (2019) validated only to the 0–18 m limit of their LiDAR; the deep zone is where per-pixel Stumpf noise dominates. *(Saturation depth is the consensus operating ceiling in the SDB literature; specific 0–18 m validation bound is Caballero et al. 2019.)*
- **Turbidity / water clarity:** suspended sediment and chlorophyll raise attenuation and shrink the usable depth, and can be mistaken for depth. Caballero et al. (2019) is dedicated to quantifying this: they use the **red-edge NIR bands (704 nm on Sentinel-2 MSI, 709 nm on Sentinel-3 OLCI)** plus a standard ocean-colour chlorophyll product to detect turbidity and cap the SDB depth limit accordingly. In turbid water SDB is unreliable at any depth. *(Caballero et al. 2019, abstract, verified.)*
- **Sunglint:** specular sun reflection off the surface swamps the bottom signal; must be corrected (Hedley 2005) or the affected pixels masked. Heavy glint can make a scene unusable.
- **Bottom-type / albedo confounds:** the ratio suppresses but does not eliminate bottom-albedo effects; very dark (seagrass, dark rubble) or very bright (white sand) bottoms bias depth. Traganos et al. (2018) report, from their spatial error maps, **over-prediction over low-reflectance *and very shallow* seabeds, and under-prediction over high-reflectance (<6 m) *and optically deep (>17 m)* bottoms** — note the bias is partly depth-driven, not purely albedo-driven. *(Traganos et al. 2018, abstract, verified verbatim.)*
- **Tide stage:** each scene is a snapshot at one tide; the retrieved depth is relative to that instantaneous water level. Blending scenes shot at different tide stages smears shallow edges, and any absolute depth needs tidal reduction to a datum. *(Tidal-datum requirement is standard hydrographic practice; flagged as method convention.)*
- **Not appropriate for:** deep water (> ~20 m), persistently turbid water, high-turbidity river mouths, heavy-glint/rough-sea scenes, or anywhere lacking ≥ a few reliable control depths for calibration.

---

## 6. Free data access (verified live, 2026-08-21)

- **Copernicus (official):** Sentinel-2 L1C/L2A are free and open via the **Copernicus Data Space Ecosystem** ([dataspace.copernicus.eu](https://dataspace.copernicus.eu)). *(Official free-and-open Copernicus data policy.)*
- **Earth Search STAC (public, no auth):** endpoint `https://earth-search.aws.element84.com/v1/`, collection **`sentinel-2-l2a`** (verified live: `id: sentinel-2-l2a`, `title: Sentinel-2 Level-2A`). Search via `POST /v1/search` with a small `bbox`, `query.eo:cloud_cover < N`, sort ascending by cloud cover.
- **Band asset names (verified live from the collection's `item_assets`):** `blue` (B02, `center_wavelength 0.49`, 10 m), `green` (B03, `center_wavelength 0.56`, 10 m), `nir` (B08, 10 m), `scl` (Scene Classification, 20 m); plus `red`, `coastal`, `rededge1/2/3`, `nir08/09`, `swir16/22`, `visual`. COG assets are the default hrefs; `*-jp2` variants also exist.
- **Public COGs:** hosted as Cloud-Optimized GeoTIFFs on the public bucket `sentinel-cogs.s3.us-west-2.amazonaws.com` — read windowed over HTTP with `/vsicurl` (GDAL/rasterio), no download of the full 100×100 km tile. *(Bucket/COG hosting verified via the STAC asset hrefs in the live `sentinel-2-l2a` collection response.)*
- **Cloud-cover query field:** `eo:cloud_cover` (percent), queryable in the STAC search body. *(Verified: standard STAC EO extension field exposed by the live Earth Search collection.)*

---

## 7. Validation / reference bathymetry sources (coverage & resolution caveats)

- **GEBCO** — global gridded bathymetry (`GEBCO_20xx` grid) on a **15 arc-second** interval (~**450 m** at the equator). Global coverage but far too coarse to resolve reef-scale shape; useful only as an absolute-scale bootstrap or a coarse sanity check. *(Resolution verified from GEBCO official: [gebco.net/data-products/gridded-bathymetry-data](https://www.gebco.net/data-products/gridded-bathymetry-data) — "15 arc-second interval grid.")*
- **EMODnet Bathymetry** — harmonised Digital Terrain Model for **European sea regions only**, on a **1/16 × 1/16 arc-minute grid (~115 m)**; returns null outside Europe (e.g. over SE Asia / Bali). *(Verified from EMODnet official: [emodnet.ec.europa.eu/en/bathymetry](https://emodnet.ec.europa.eu/en/bathymetry) — "1/16 * 1/16 arc minutes (ca. 115 metre grid)"; European coverage stated on the same page.)*
- **Allen Coral Atlas** — tropical reef mapping. **Correction to a common misconception:** the Atlas's **bathymetry** product is itself **Sentinel-2-derived at 10 m resolution** (Google Earth Engine, 12-month median composite; Landsat-8 and Planet Dove fill gaps), *not* the 3.125 m figure — the **3.125 m PlanetScope/Dove resolution applies to the habitat/benthic maps**, not the depth layer. Access is gated (login + named-user conservation terms; tile host non-public), which is exactly why open reef bathymetry is scarce enough to be a moat. Treat fetching as human-in-the-loop, not auto-scraped. *(Verified from Allen Coral Atlas methods: [allencoralatlas.org/methods](https://allencoralatlas.org/methods) — bathymetry "created at a resolution of 10 m using the Google Earth Engine (GEE) Sentinel-2 surface reflectance dataset"; PlanetScope "Pixel Size: 3.125 m" refers to the imagery for habitat mapping.)*

**Practical validation note:** the balisurf pipeline's reported correlation ~0.84 vs GEBCO (RMSE ~3.4 m, `tools/sdb/README.md`) is *consistent with GEBCO being a coarse (~450 m) reference* — it validates the broad shelf-to-deep gradient, not reef detail. On-reef control points would be the meaningful check, but the repo's only on-reef set (`tools/sdb/control_points_uluwatu.csv`) is self-declared **reef-knowledge estimates, not measured soundings**, so no on-reef accuracy figure has been earned yet. This matches the literature's practice of validating against LiDAR/soundings, not GEBCO, for reef-scale accuracy — real soundings remain the outstanding gap.

---

## Source ledger (primary-source status)

| Source | Role | Access status |
|---|---|---|
| Stumpf & Holderied 2003, *Limnol. Oceanogr.* [doi](https://doi.org/10.4319/lo.2003.48.1_part_2.0547) | Log-ratio method | Metadata verified; **full text paywalled/Cloudflare-blocked, not accessed** |
| Lyzenga 1978, *Appl. Opt.* [doi](https://doi.org/10.1364/AO.17.000379) | Linear/ratio method origin | **Abstract accessed**; full text login-gated |
| Lyzenga 1985, *IJRS* [doi](https://doi.org/10.1080/01431168508948428) | Log-linear + LiDAR | Metadata verified; full text not accessed |
| Lyzenga, Malinas & Tanis 2006, *IEEE TGRS* [doi](https://doi.org/10.1109/TGRS.2006.872909) | Physically-based optimization | Metadata/venue verified; full text paywalled, not accessed |
| Caballero, Stumpf & Meredith 2019, *Remote Sensing* 11(6):645 [doi](https://doi.org/10.3390/rs11060645) | Accuracy numbers, turbidity | **Open access (CC-BY), abstract fully accessed** |
| Traganos et al. 2018, *Remote Sensing* 10(6):859 [doi](https://doi.org/10.3390/rs10060859) | R²/RMSE, error maps (classical algorithms, **not** ML) | **Open access (CC-BY), abstract fully accessed** |
| Sagawa et al. 2019, *Remote Sensing* 11(10):1155 [doi](https://doi.org/10.3390/rs11101155) | ML/random-forest SDB (Landsat-8) | **Open access (CC-BY), abstract fully accessed** |
| Hedley, Harborne & Mumby 2005, *IJRS* [doi](https://doi.org/10.1080/01431160500034086) | Sunglint correction | Metadata/title verified; full text not accessed |
| ESA Sentinel-2 product docs ([sentiwiki s2-products](https://sentiwiki.copernicus.eu/web/s2-products), [s2-processing](https://sentiwiki.copernicus.eu/web/s2-processing)) | BOA_ADD_OFFSET, PB 04.00 date, SCL, L2A | **Official ESA docs, accessed live** |
| Earth Search STAC `sentinel-2-l2a` collection | Collection id, band assets, wavelengths, COG bucket | **Live API response, accessed 2026-08-21** |
| GEBCO ([gebco.net](https://www.gebco.net/data-products/gridded-bathymetry-data)) | 15 arc-sec grid | **Official, accessed live** |
| EMODnet ([emodnet.ec.europa.eu](https://emodnet.ec.europa.eu/en/bathymetry)) | ~115 m, Europe-only | **Official, accessed live** |
| Allen Coral Atlas ([allencoralatlas.org/methods](https://allencoralatlas.org/methods)) | 10 m S2 bathymetry / 3.125 m habitat | **Official, accessed live** |

**Explicitly unverified against primary full text:** the exact printed equations of Stumpf 2003, Lyzenga 1978/1985/2006 (reproduced here in their standard forms as restated in the two open-access papers, not read from the originals); the `n=1000` constant convention (standard in practice, in Caballero et al. 2019, but the original Stumpf text was not read to confirm the exact constant). Everything in §4's ESA offset paragraph, §6's STAC/asset details, and §7's resolution figures was read directly from the official/primary source and is not a restatement.
