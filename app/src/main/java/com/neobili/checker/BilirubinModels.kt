package com.neobili.checker

enum class BilirubinUnit { MG_DL, UMOL_L }
enum class MeasurementType { TSB, TCB }

data class RiskFactors(
    val albuminBelow3: Boolean = false,
    val isoimmuneHemolysis: Boolean = false,
    val g6pdDeficiency: Boolean = false,
    val otherHemolysis: Boolean = false,
    val sepsis: Boolean = false,
    val clinicalInstability24h: Boolean = false,
) {
    val anyAdditional: Boolean
        get() = albuminBelow3 || isoimmuneHemolysis || g6pdDeficiency ||
            otherHemolysis || sepsis || clinicalInstability24h
}

data class BiliInput(
    val gestationalAgeWeeks: Int,
    val ageHours: Int,
    val bilirubin: Double,
    val unit: BilirubinUnit,
    val measurementType: MeasurementType,
    val riskFactors: RiskFactors,
    val previousPhototherapy: Boolean,
) {
    val bilirubinMgDl: Double
        get() = when (unit) {
            BilirubinUnit.MG_DL -> bilirubin
            BilirubinUnit.UMOL_L -> bilirubin / 17.1
        }
}

data class Thresholds(
    val phototherapyMgDl: Double,
    val exchangeMgDl: Double,
) {
    val escalationMgDl: Double get() = exchangeMgDl - 2.0
}

data class ClinicalResult(
    val thresholds: Thresholds,
    val deltaToPhoto: Double,
    val deltaToEscalation: Double,
    val deltaToExchange: Double,
    val level: ResultLevel,
    val headline: String,
    val actions: List<String>,
    val cautions: List<String>,
)

enum class ResultLevel { BELOW_PHOTOTHERAPY, PHOTOTHERAPY, ESCALATION, EXCHANGE }
