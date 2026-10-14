# NeoBili Checker (Android)

Native Android/Jetpack Compose clinical-reference prototype for checking neonatal bilirubin treatment thresholds.

## Clinical scope

- AAP 2022 hyperbilirubinemia guideline
- Newborns **≥35 completed weeks** gestation
- Age **1–336 hours** in this prototype
- Phototherapy threshold
- Escalation-of-care threshold (= 2 mg/dL below exchange threshold)
- Exchange transfusion threshold
- Neurotoxicity risk factors separated from gestational age
- TcB → TSB confirmation rule
- Birth-hospitalization discharge follow-up logic when applicable
- mg/dL and µmol/L input/output

## Threshold source

The app requests the AAP-2022-derived threshold pair from the free PediTools API:

`https://peditools.org/bili2022/api/`

Only gestational age, postnatal age, and risk category are sent. The app does **not** collect patient names, MRNs, dates of birth, or other patient identifiers.

The app intentionally has no approximate/offline fallback. If the remote threshold cannot be verified, the app displays an error instead of estimating a treatment threshold.

## Build

Current project configuration (October 2026):

- Android Gradle Plugin 9.4.0
- compileSdk / targetSdk 37
- Kotlin 2.4.10 (explicit plugin; AGP built-in Kotlin disabled for this project)
- Jetpack Compose BOM 2026.09.00
- JDK 17 recommended

Open the folder in a current Android Studio, allow Gradle sync, then run on an Android device/emulator (minSdk 26).

## Important clinical limitations

This is a **clinical reference prototype**, not a certified medical device and not a substitute for clinician judgment or local hospital policy.

Key AAP points implemented/displayed:

1. Use TSB treatment thresholds according to gestational age, postnatal age, and **additional** neurotoxicity risk factors.
2. Gestational age <38 weeks is already handled by separate curves and should not automatically be selected again as an additional risk factor.
3. Obtain TSB when TcB is ≥15 mg/dL or within 3 mg/dL of the phototherapy threshold.
4. Escalation of care begins at 2 mg/dL below the exchange transfusion threshold.
5. Do not subtract direct/conjugated bilirubin from TSB when applying treatment thresholds.
6. A bilirubin at/above the phototherapy threshold before 24 hours should raise concern for hemolysis.
7. AAP Figure 7 discharge follow-up logic applies to infants ≥12 hours who have not received phototherapy.

## Suggested production upgrades

Before real clinical deployment, consider:

- Embed a validated, version-locked threshold dataset for fully offline use.
- Independent unit tests against every hourly AAP supplemental-table value.
- Add trend/rate-of-rise with multiple bilirubin measurements.
- Add date/time-based age calculator.
- Add rebound-phototherapy decision support.
- Add hospital-configurable local policy overlay.
- Add audit/version screen showing guideline + dataset version.
- Formal clinical validation, usability testing, privacy/security review, and regulatory review as applicable.
