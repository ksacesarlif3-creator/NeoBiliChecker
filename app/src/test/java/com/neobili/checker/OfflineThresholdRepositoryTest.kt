package com.neobili.checker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineThresholdRepositoryTest {
    private val repository = OfflineThresholdRepository()

    @Test
    fun knownOfficialThresholdPointsMatch() {
        repository.getThresholds(40, 24, false).also {
            assertEquals(13.3, it.phototherapyMgDl, 0.01)
            assertEquals(21.4, it.exchangeMgDl, 0.01)
        }
        repository.getThresholds(40, 48, true).also {
            assertEquals(14.0, it.phototherapyMgDl, 0.01)
            assertEquals(20.1, it.exchangeMgDl, 0.01)
        }
        repository.getThresholds(35, 24, false).also {
            assertEquals(10.6, it.phototherapyMgDl, 0.01)
            assertEquals(17.9, it.exchangeMgDl, 0.01)
        }
        repository.getThresholds(35, 72, false).also {
            assertEquals(16.8, it.phototherapyMgDl, 0.01)
            assertEquals(22.9, it.exchangeMgDl, 0.01)
        }
    }

    @Test
    fun allBundledCurvesAreClinicallyOrderedAndNondecreasing() {
        for (ga in 35..40) {
            for (hasAdditionalRisk in listOf(false, true)) {
                var previousPhoto = Double.NEGATIVE_INFINITY
                var previousExchange = Double.NEGATIVE_INFINITY
                for (age in 1..336) {
                    val thresholds = repository.getThresholds(ga, age, hasAdditionalRisk)
                    assertTrue("Phototherapy threshold must be below exchange", thresholds.phototherapyMgDl < thresholds.exchangeMgDl)
                    assertEquals(thresholds.exchangeMgDl - 2.0, thresholds.escalationMgDl, 1e-9)
                    assertTrue("Phototherapy curve must not decrease", thresholds.phototherapyMgDl >= previousPhoto)
                    assertTrue("Exchange curve must not decrease", thresholds.exchangeMgDl >= previousExchange)
                    previousPhoto = thresholds.phototherapyMgDl
                    previousExchange = thresholds.exchangeMgDl
                }
            }
        }
    }
}
