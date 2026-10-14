package com.neobili.checker

import java.io.ByteArrayInputStream
import java.util.Base64
import java.util.zip.GZIPInputStream

/**
 * Exact hour-specific AAP 2022 hyperbilirubinemia thresholds (ages 1..336 h).
 * The validated table is compressed only to keep source compact; all values are
 * bundled inside the APK and are decoded locally. No network access is used.
 */
internal object OfflineThresholdData {
    private const val PAYLOAD = "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"

    private val parsed: Map<String, DoubleArray> by lazy {
        val compressed = Base64.getDecoder().decode(PAYLOAD)
        val text = GZIPInputStream(ByteArrayInputStream(compressed)).bufferedReader().use { it.readText() }
        text.lineSequence()
            .filter { it.isNotBlank() }
            .associate { line ->
                val split = line.indexOf('=')
                require(split > 0) { "Invalid bundled threshold row" }
                val key = line.substring(0, split)
                val values = line.substring(split + 1).split(',').map(String::toDouble).toDoubleArray()
                require(values.size == 336) { "Invalid bundled threshold curve: $key" }
                key to values
            }
    }

    fun value(kind: Char, ga: Int, ageHours: Int, hasAdditionalRisk: Boolean): Double {
        require(kind == 'P' || kind == 'E') { "Threshold kind must be P or E" }
        require(ga in 35..40) { "GA must be 35..40 weeks" }
        require(ageHours in 1..336) { "Age must be 1..336 hours" }
        val risk = if (hasAdditionalRisk) "R" else "N"
        return parsed.getValue("$kind:$risk:$ga")[ageHours - 1]
    }
}
