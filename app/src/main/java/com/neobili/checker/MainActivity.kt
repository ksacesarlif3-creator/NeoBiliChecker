package com.neobili.checker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.max

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NeoBiliApp() }
    }
}

@Composable
fun NeoBiliApp(vm: MainViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF8A6500),
            secondary = Color(0xFF006A6A),
            surface = Color(0xFFF8FAFC),
            background = Color(0xFFF8FAFC),
        )
    ) {
        Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("NeoBili Checker", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(
                    "AAP 2022 bilirubin threshold checker • ≥35 weeks",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                InputCard(state, vm)

                Button(
                    onClick = vm::calculate,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    enabled = !state.loading,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (state.loading) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Checking verified thresholds…")
                    } else {
                        Text("Check bilirubin", fontWeight = FontWeight.SemiBold)
                    }
                }

                state.error?.let { ErrorCard(it) }
                state.result?.let { ResultCard(it, state) }

                SafetyCard()
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun InputCard(state: UiState, vm: MainViewModel) {
    ElevatedCard(shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Patient data", fontWeight = FontWeight.Bold, fontSize = 18.sp)

            Text("Gestational age at birth")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (35..40).forEach { ga ->
                    FilterChip(
                        selected = state.ga == ga,
                        onClick = { vm.setGa(ga) },
                        label = { Text(if (ga == 40) "40+" else ga.toString()) }
                    )
                }
            }

            OutlinedTextField(
                value = state.ageHours,
                onValueChange = vm::setAge,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Postnatal age (hours)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            OutlinedTextField(
                value = state.bilirubin,
                onValueChange = vm::setBilirubin,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Bilirubin") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                trailingIcon = { Text(if (state.unit == BilirubinUnit.MG_DL) "mg/dL" else "µmol/L") }
            )

            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.unit == BilirubinUnit.MG_DL,
                    onClick = { vm.setUnit(BilirubinUnit.MG_DL) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("mg/dL") }
                SegmentedButton(
                    selected = state.unit == BilirubinUnit.UMOL_L,
                    onClick = { vm.setUnit(BilirubinUnit.UMOL_L) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("µmol/L") }
            }

            Text("Measurement")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.measurementType == MeasurementType.TSB,
                    onClick = { vm.setMeasurementType(MeasurementType.TSB) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("TSB") }
                SegmentedButton(
                    selected = state.measurementType == MeasurementType.TCB,
                    onClick = { vm.setMeasurementType(MeasurementType.TCB) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("TcB") }
            }

            RiskFactorSection(state.riskFactors, vm::setRiskFactors)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = state.previousPhototherapy,
                    onCheckedChange = vm::setPreviousPhototherapy
                )
                Text("Infant has already received phototherapy")
            }
        }
    }
}

@Composable
private fun RiskFactorSection(r: RiskFactors, onChange: (RiskFactors) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text("Additional neurotoxicity risk factors", fontWeight = FontWeight.SemiBold)
                Text(
                    if (r.anyAdditional) "At least one additional factor selected" else "None selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide" else "Select") }
        }
        if (expanded) {
            RiskCheck("Albumin <3.0 g/dL", r.albuminBelow3) { onChange(r.copy(albuminBelow3 = it)) }
            RiskCheck("Isoimmune hemolytic disease", r.isoimmuneHemolysis) { onChange(r.copy(isoimmuneHemolysis = it)) }
            RiskCheck("G6PD deficiency", r.g6pdDeficiency) { onChange(r.copy(g6pdDeficiency = it)) }
            RiskCheck("Other hemolytic condition", r.otherHemolysis) { onChange(r.copy(otherHemolysis = it)) }
            RiskCheck("Sepsis", r.sepsis) { onChange(r.copy(sepsis = it)) }
            RiskCheck("Significant clinical instability in previous 24 h", r.clinicalInstability24h) { onChange(r.copy(clinicalInstability24h = it)) }
            Text(
                "Prematurity is already accounted for by the gestational-age curve; do not select it again as an additional factor.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RiskCheck(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChecked)
        Text(label, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ResultCard(result: ClinicalResult, state: UiState) {
    val biliMg = state.bilirubin.toDoubleOrNull()?.let {
        if (state.unit == BilirubinUnit.MG_DL) it else it / 17.1
    } ?: return

    val accent = when (result.level) {
        ResultLevel.BELOW_PHOTOTHERAPY -> Color(0xFF2E7D32)
        ResultLevel.PHOTOTHERAPY -> Color(0xFFE09F00)
        ResultLevel.ESCALATION -> Color(0xFFEF6C00)
        ResultLevel.EXCHANGE -> Color(0xFFC62828)
    }

    ElevatedCard(shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(result.headline, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = accent)
            Text(
                "${String.format("%.1f", biliMg)} mg/dL  •  ${String.format("%.0f", BilirubinRules.mgDlToUmol(biliMg))} µmol/L",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            ThresholdGauge(biliMg, result.thresholds)

            HorizontalDivider()
            BilirubinCurveChart(state = state, bilirubinMgDl = biliMg)
            HorizontalDivider()

            ThresholdRow("Phototherapy", result.thresholds.phototherapyMgDl, result.deltaToPhoto)
            ThresholdRow("Escalation", result.thresholds.escalationMgDl, result.deltaToEscalation)
            ThresholdRow("Exchange", result.thresholds.exchangeMgDl, result.deltaToExchange)

            HorizontalDivider()
            Text("Recommended next step", fontWeight = FontWeight.Bold)
            result.actions.forEach { Bullet(it) }

            if (result.cautions.isNotEmpty()) {
                HorizontalDivider()
                Text("Important checks", fontWeight = FontWeight.Bold)
                result.cautions.forEach { Bullet(it) }
            }
        }
    }
}

@Composable
private fun ThresholdGauge(bili: Double, t: Thresholds) {
    val maxValue = max(t.exchangeMgDl + 2.0, bili + 2.0)
    Canvas(Modifier.fillMaxWidth().height(54.dp)) {
        val y = size.height / 2f
        drawLine(Color(0xFFD5DBE3), Offset(8f, y), Offset(size.width - 8f, y), strokeWidth = 12f, cap = StrokeCap.Round)
        fun x(v: Double): Float = (8f + ((size.width - 16f) * (v / maxValue)).toFloat())
        drawCircle(Color(0xFFE09F00), 9f, Offset(x(t.phototherapyMgDl), y))
        drawCircle(Color(0xFFEF6C00), 9f, Offset(x(t.escalationMgDl), y))
        drawCircle(Color(0xFFC62828), 9f, Offset(x(t.exchangeMgDl), y))
        drawCircle(Color(0xFF1D3557), 11f, Offset(x(bili), y))
    }
    Text("● bilirubin   ● phototherapy   ● escalation   ● exchange", style = MaterialTheme.typography.labelSmall)
}

@Composable
private fun ThresholdRow(name: String, threshold: Double, delta: Double) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(name)
        Column(horizontalAlignment = Alignment.End) {
            Text("${String.format("%.1f", threshold)} mg/dL", fontWeight = FontWeight.SemiBold)
            Text(BilirubinRules.signedDeltaText(delta), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        Text("•", fontWeight = FontWeight.Bold)
        Text(text, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Text(message, Modifier.padding(14.dp), color = MaterialTheme.colorScheme.onErrorContainer)
    }
}

@Composable
private fun SafetyCard() {
    OutlinedCard(shape = RoundedCornerShape(16.dp)) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.Info, contentDescription = null)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Clinical reference only", fontWeight = FontWeight.Bold)
                Text(
                    "Based on the AAP 2022 guideline for newborns ≥35 weeks. Treatment thresholds and curves are bundled inside the app and work fully offline; no patient data are transmitted. Verify critical treatment decisions against the current guideline and local policy.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
