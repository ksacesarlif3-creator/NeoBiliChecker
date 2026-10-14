package com.neobili.checker

import kotlin.math.abs

object BilirubinRules {
    fun validate(input: BiliInput): String? {
        if (input.gestationalAgeWeeks !in 35..40) return "AAP 2022 charts in this app are for gestational age 35–40+ completed weeks."
        if (input.ageHours !in 1..336) return "Postnatal age must be 1–336 hours."
        if (!input.bilirubin.isFinite() || input.bilirubin <= 0.0) return "Enter a valid bilirubin value."
        val mg = input.bilirubinMgDl
        if (mg > 60) return "Bilirubin value appears implausibly high. Check the unit and value."
        return null
    }

    fun interpret(input: BiliInput, thresholds: Thresholds): ClinicalResult {
        val bili = input.bilirubinMgDl
        val photoDelta = thresholds.phototherapyMgDl - bili
        val escalationDelta = thresholds.escalationMgDl - bili
        val exchangeDelta = thresholds.exchangeMgDl - bili

        val level = when {
            bili >= thresholds.exchangeMgDl -> ResultLevel.EXCHANGE
            bili >= thresholds.escalationMgDl -> ResultLevel.ESCALATION
            bili >= thresholds.phototherapyMgDl -> ResultLevel.PHOTOTHERAPY
            else -> ResultLevel.BELOW_PHOTOTHERAPY
        }

        val actions = mutableListOf<String>()
        val cautions = mutableListOf<String>()

        when (level) {
            ResultLevel.EXCHANGE -> {
                actions += "At or above the exchange transfusion threshold: urgent escalation and preparation for exchange transfusion per AAP pathway."
                actions += "Continue intensive phototherapy while arranging escalation/exchange care."
            }
            ResultLevel.ESCALATION -> {
                actions += "At or above the escalation-of-care threshold (2 mg/dL below exchange): escalate care urgently per AAP pathway."
                actions += "Use intensive phototherapy and obtain the escalation-of-care evaluation/monitoring required by the guideline."
            }
            ResultLevel.PHOTOTHERAPY -> {
                if (input.measurementType == MeasurementType.TCB) {
                    actions += "Confirm with total serum bilirubin (TSB) before using a TcB value to make the treatment decision."
                } else {
                    actions += "At or above the phototherapy threshold: intensive phototherapy is indicated by the AAP 2022 threshold chart."
                }
                val abovePhoto = bili - thresholds.phototherapyMgDl
                if (!input.previousPhototherapy && input.gestationalAgeWeeks >= 38 && input.ageHours >= 48 &&
                    !input.riskFactors.anyAdditional && abovePhoto <= 1.0
                ) {
                    actions += "Home LED phototherapy may be an option only if ALL AAP home-phototherapy criteria are satisfied."
                }
            }
            ResultLevel.BELOW_PHOTOTHERAPY -> {
                actions += followUpAdvice(input, photoDelta)
            }
        }

        if (input.measurementType == MeasurementType.TCB &&
            (bili >= 15.0 || photoDelta <= 3.0)
        ) {
            cautions += "AAP KAS 6: obtain a TSB because TcB is ≥15 mg/dL or within 3 mg/dL of the phototherapy threshold."
        }

        if (input.ageHours < 24 && bili >= thresholds.phototherapyMgDl) {
            cautions += "TSB at/above the phototherapy threshold before 24 hours is likely to reflect hemolysis; evaluate for hemolytic disease."
        }

        cautions += "Use total bilirubin; do not subtract the direct/conjugated fraction from TSB when applying these thresholds."
        cautions += "If direct/conjugated bilirubin is >50% of TSB, seek specialist guidance rather than relying on this chart alone."

        val headline = when (level) {
            ResultLevel.EXCHANGE -> "Exchange threshold reached"
            ResultLevel.ESCALATION -> "Escalation-of-care threshold reached"
            ResultLevel.PHOTOTHERAPY -> "Phototherapy threshold reached"
            ResultLevel.BELOW_PHOTOTHERAPY -> "Below phototherapy threshold"
        }

        return ClinicalResult(
            thresholds = thresholds,
            deltaToPhoto = photoDelta,
            deltaToEscalation = escalationDelta,
            deltaToExchange = exchangeDelta,
            level = level,
            headline = headline,
            actions = actions,
            cautions = cautions,
        )
    }

    private fun followUpAdvice(input: BiliInput, delta: Double): String {
        if (input.previousPhototherapy) {
            return "The birth-hospitalization discharge follow-up table is not applicable after prior phototherapy; use rebound/follow-up guidance and clinical judgment."
        }
        if (input.ageHours < 12) {
            return "AAP Figure 7 discharge follow-up guidance applies from 12 hours of age; use clinical judgment and repeat bilirubin as clinically indicated."
        }
        return when {
            delta < 0.1 -> "Very close to the phototherapy threshold: reassess promptly and use clinical judgment."
            delta <= 1.9 && input.ageHours < 24 -> "Delay discharge, consider phototherapy, and measure TSB in 4–8 hours."
            delta <= 1.9 -> "Measure TSB in 4–24 hours; options include delayed discharge/phototherapy, qualifying home phototherapy, or close follow-up."
            delta <= 3.4 -> "Repeat TSB or TcB in 4–24 hours."
            delta <= 5.4 -> "Repeat TSB or TcB in 1–2 days."
            delta <= 6.9 && input.ageHours < 72 -> "Follow-up within 2 days; TcB or TSB according to clinical judgment."
            delta <= 6.9 -> "Follow-up timing according to clinical judgment."
            delta >= 7.0 && input.ageHours < 72 -> "Follow-up within 3 days; TcB or TSB according to clinical judgment."
            else -> "Follow-up timing according to clinical judgment."
        }
    }

    fun mgDlToUmol(value: Double): Double = value * 17.1
    fun signedDeltaText(delta: Double): String = when {
        abs(delta) < 0.05 -> "at threshold"
        delta > 0 -> String.format("%.1f below", delta)
        else -> String.format("%.1f above", -delta)
    }
}
