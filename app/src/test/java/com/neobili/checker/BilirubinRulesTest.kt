package com.neobili.checker

import org.junit.Assert.*
import org.junit.Test

class BilirubinRulesTest {
    @Test
    fun unitConversion_isCorrect() {
        assertEquals(171.0, BilirubinRules.mgDlToUmol(10.0), 0.001)
    }

    @Test
    fun escalationThreshold_isTwoBelowExchange() {
        val t = Thresholds(17.0, 24.0)
        assertEquals(22.0, t.escalationMgDl, 0.001)
    }

    @Test
    fun knownPediToolsExample_40w48hNoRisk() {
        val input = BiliInput(40, 48, 10.0, BilirubinUnit.MG_DL, MeasurementType.TSB, RiskFactors(), false)
        val result = BilirubinRules.interpret(input, Thresholds(17.0, 24.0))
        assertEquals(ResultLevel.BELOW_PHOTOTHERAPY, result.level)
        assertEquals(7.0, result.deltaToPhoto, 0.001)
        assertEquals(12.0, result.deltaToEscalation, 0.001)
        assertEquals(14.0, result.deltaToExchange, 0.001)
    }

    @Test
    fun tcbNearThreshold_requiresSerumConfirmation() {
        val input = BiliInput(40, 48, 14.5, BilirubinUnit.MG_DL, MeasurementType.TCB, RiskFactors(), false)
        val result = BilirubinRules.interpret(input, Thresholds(17.0, 24.0))
        assertTrue(result.cautions.any { it.contains("obtain a TSB") })
    }

    @Test
    fun gaPrematurity_notCountedAsAdditionalRisk() {
        val r = RiskFactors()
        assertFalse(r.anyAdditional)
    }
}
