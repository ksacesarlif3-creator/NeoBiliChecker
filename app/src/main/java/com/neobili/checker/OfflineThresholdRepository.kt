package com.neobili.checker

/**
 * Reads AAP 2022 thresholds bundled directly in the APK.
 *
 * The data object is generated from the official AAP supplemental Tables 1-4
 * during the build and contains every integer postnatal hour from 1 to 336.
 */
class OfflineThresholdRepository {
    fun getThresholds(ga: Int, ageHours: Int, hasAdditionalRisk: Boolean): Thresholds {
        return Thresholds(
            phototherapyMgDl = OfflineThresholdData.value(
                kind = 'P',
                ga = ga,
                ageHours = ageHours,
                hasAdditionalRisk = hasAdditionalRisk,
            ),
            exchangeMgDl = OfflineThresholdData.value(
                kind = 'E',
                ga = ga,
                ageHours = ageHours,
                hasAdditionalRisk = hasAdditionalRisk,
            ),
        )
    }
}
