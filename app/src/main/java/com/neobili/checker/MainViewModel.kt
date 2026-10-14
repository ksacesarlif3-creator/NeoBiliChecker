package com.neobili.checker

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UiState(
    val ga: Int = 40,
    val ageHours: String = "48",
    val bilirubin: String = "10",
    val unit: BilirubinUnit = BilirubinUnit.MG_DL,
    val measurementType: MeasurementType = MeasurementType.TSB,
    val riskFactors: RiskFactors = RiskFactors(),
    val previousPhototherapy: Boolean = false,
    val loading: Boolean = false,
    val result: ClinicalResult? = null,
    val error: String? = null,
)

class MainViewModel(
    private val repository: OfflineThresholdRepository = OfflineThresholdRepository()
) : ViewModel() {
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun setGa(value: Int) { _state.value = _state.value.copy(ga = value, result = null, error = null) }
    fun setAge(value: String) { _state.value = _state.value.copy(ageHours = value.filter { it.isDigit() }, result = null, error = null) }
    fun setBilirubin(value: String) {
        val cleaned = value.filter { it.isDigit() || it == '.' }.let { s ->
            val firstDot = s.indexOf('.')
            if (firstDot < 0) s else s.substring(0, firstDot + 1) + s.substring(firstDot + 1).replace(".", "")
        }
        _state.value = _state.value.copy(bilirubin = cleaned, result = null, error = null)
    }
    fun setUnit(value: BilirubinUnit) { _state.value = _state.value.copy(unit = value, result = null, error = null) }
    fun setMeasurementType(value: MeasurementType) { _state.value = _state.value.copy(measurementType = value, result = null, error = null) }
    fun setPreviousPhototherapy(value: Boolean) { _state.value = _state.value.copy(previousPhototherapy = value, result = null, error = null) }
    fun setRiskFactors(value: RiskFactors) { _state.value = _state.value.copy(riskFactors = value, result = null, error = null) }

    fun calculate() {
        val s = _state.value
        val age = s.ageHours.toIntOrNull()
        val bili = s.bilirubin.toDoubleOrNull()
        if (age == null || bili == null) {
            _state.value = s.copy(error = "Enter age in hours and bilirubin value.")
            return
        }
        val input = BiliInput(
            gestationalAgeWeeks = s.ga,
            ageHours = age,
            bilirubin = bili,
            unit = s.unit,
            measurementType = s.measurementType,
            riskFactors = s.riskFactors,
            previousPhototherapy = s.previousPhototherapy,
        )
        BilirubinRules.validate(input)?.let {
            _state.value = s.copy(error = it)
            return
        }

        runCatching {
            repository.getThresholds(
                ga = input.gestationalAgeWeeks,
                ageHours = input.ageHours,
                hasAdditionalRisk = input.riskFactors.anyAdditional,
            )
        }.onSuccess { thresholds ->
            _state.value = _state.value.copy(
                loading = false,
                result = BilirubinRules.interpret(input, thresholds),
                error = null,
            )
        }.onFailure { err ->
            _state.value = _state.value.copy(
                loading = false,
                error = "Unable to read the bundled AAP 2022 threshold table. (${err.message ?: "unknown error"})"
            )
        }
    }
}
