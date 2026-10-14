package com.neobili.checker

import org.junit.Assert.assertEquals
import org.junit.Test

class PediToolsParserTest {
    private val repo = PediToolsThresholdRepository()

    @Test
    fun parsesNoRiskRow() {
        val html = """
            <html><body>
            <table><tr><th>Phototherapy threshold</th><th>Exchange threshold</th></tr>
            <tr><td>No neurotoxicity risk factors</td><td>17 mg/dL</td><td>24 mg/dL</td></tr></table>
            </body></html>
        """.trimIndent()
        val t = repo.parseThresholds(html, false)
        assertEquals(17.0, t.phototherapyMgDl, 0.001)
        assertEquals(24.0, t.exchangeMgDl, 0.001)
    }

    @Test
    fun parsesRiskRow() {
        val html = """
            <html><body>
            <tr><td>ANY neurotoxicity risk factors</td><td>14 mg/dL</td><td>20.1 mg/dL</td></tr>
            </body></html>
        """.trimIndent()
        val t = repo.parseThresholds(html, true)
        assertEquals(14.0, t.phototherapyMgDl, 0.001)
        assertEquals(20.1, t.exchangeMgDl, 0.001)
    }
}
