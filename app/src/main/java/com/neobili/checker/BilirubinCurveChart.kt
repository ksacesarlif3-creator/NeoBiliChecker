package com.neobili.checker

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min

private data class CurvePoint(
    val ageHours: Int,
    val phototherapy: Double,
    val escalation: Double,
    val exchange: Double,
)

private val PhotoColor = Color(0xFFE09F00)
private val EscalationColor = Color(0xFFEF6C00)
private val ExchangeColor = Color(0xFFC62828)
private val PatientColor = Color(0xFF1D3557)
private val GridColor = Color(0xFFD7DCE3)

@Composable
fun BilirubinCurveChart(
    state: UiState,
    bilirubinMgDl: Double,
) {
    val age = state.ageHours.toIntOrNull() ?: return
    val risk = state.riskFactors.anyAdditional
    var showFullCurve by remember(state.ga, age, risk) { mutableStateOf(false) }

    val allPoints = remember(state.ga, risk) {
        (1..336).map { hour ->
            val photo = OfflineThresholdData.value('P', state.ga, hour, risk)
            val exchange = OfflineThresholdData.value('E', state.ga, hour, risk)
            CurvePoint(
                ageHours = hour,
                phototherapy = photo,
                escalation = exchange - 2.0,
                exchange = exchange,
            )
        }
    }

    val focusedStart = max(1, age - 48)
    val focusedEnd = min(336, age + 48)
    val visiblePoints = if (showFullCurve) allPoints else allPoints.filter { it.ageHours in focusedStart..focusedEnd }
    if (visiblePoints.isEmpty()) return

    val xMin = visiblePoints.first().ageHours.toDouble()
    val xMax = visiblePoints.last().ageHours.toDouble()
    val curveMin = visiblePoints.minOf { it.phototherapy }
    val curveMax = visiblePoints.maxOf { it.exchange }
    val yMin = max(0.0, min(curveMin, bilirubinMgDl) - 2.0)
    val yMax = max(max(curveMax, bilirubinMgDl) + 2.0, yMin + 5.0)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Bilirubin plotted against treatment curves", fontWeight = FontWeight.Bold)
        Text(
            "GA ${if (state.ga == 40) "40+" else state.ga} weeks • " +
                if (risk) "additional neurotoxicity risk factor(s)" else "no additional neurotoxicity risk factors",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !showFullCurve,
                onClick = { showFullCurve = false },
                label = { Text("Patient-focused") },
            )
            FilterChip(
                selected = showFullCurve,
                onClick = { showFullCurve = true },
                label = { Text("Full 1–336 h") },
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (showFullCurve) 280.dp else 250.dp)
        ) {
            val left = 48.dp.toPx()
            val right = size.width - 10.dp.toPx()
            val top = 10.dp.toPx()
            val bottom = size.height - 34.dp.toPx()
            val plotWidth = right - left
            val plotHeight = bottom - top

            fun x(hour: Double): Float {
                val span = (xMax - xMin).coerceAtLeast(1.0)
                return left + ((hour - xMin) / span * plotWidth).toFloat()
            }

            fun y(value: Double): Float {
                val span = (yMax - yMin).coerceAtLeast(1.0)
                return bottom - ((value - yMin) / span * plotHeight).toFloat()
            }

            val labelPaint = Paint().apply {
                isAntiAlias = true
                color = Color(0xFF5F6670).toArgb()
                textSize = 10.dp.toPx()
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val boldPaint = Paint().apply {
                isAntiAlias = true
                color = PatientColor.toArgb()
                textSize = 10.dp.toPx()
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            repeat(5) { i ->
                val fraction = i / 4.0
                val yy = top + (plotHeight * fraction).toFloat()
                val value = yMax - ((yMax - yMin) * fraction)
                drawLine(GridColor, Offset(left, yy), Offset(right, yy), strokeWidth = 1.dp.toPx())
                drawContext.canvas.nativeCanvas.drawText(
                    String.format("%.0f", value),
                    3.dp.toPx(),
                    yy + 3.dp.toPx(),
                    labelPaint,
                )
            }

            repeat(5) { i ->
                val fraction = i / 4.0
                val xx = left + (plotWidth * fraction).toFloat()
                val hour = xMin + ((xMax - xMin) * fraction)
                drawLine(GridColor, Offset(xx, top), Offset(xx, bottom), strokeWidth = 1.dp.toPx())
                labelPaint.textAlign = Paint.Align.CENTER
                drawContext.canvas.nativeCanvas.drawText(
                    String.format("%.0f h", hour),
                    xx,
                    bottom + 18.dp.toPx(),
                    labelPaint,
                )
                labelPaint.textAlign = Paint.Align.LEFT
            }

            fun drawCurve(selector: (CurvePoint) -> Double, color: Color) {
                val path = Path()
                visiblePoints.forEachIndexed { index, point ->
                    val px = x(point.ageHours.toDouble())
                    val py = y(selector(point))
                    if (index == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }

            drawCurve({ it.phototherapy }, PhotoColor)
            drawCurve({ it.escalation }, EscalationColor)
            drawCurve({ it.exchange }, ExchangeColor)

            if (age.toDouble() in xMin..xMax) {
                val patientX = x(age.toDouble())
                val patientY = y(bilirubinMgDl)
                drawLine(
                    color = PatientColor.copy(alpha = 0.45f),
                    start = Offset(patientX, top),
                    end = Offset(patientX, bottom),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx())),
                )
                drawCircle(Color.White, radius = 7.dp.toPx(), center = Offset(patientX, patientY))
                drawCircle(PatientColor, radius = 5.dp.toPx(), center = Offset(patientX, patientY))

                val measurement = if (state.measurementType == MeasurementType.TSB) "TSB" else "TcB"
                val label = "$measurement ${String.format("%.1f", bilirubinMgDl)}"
                val labelX = if (patientX > right - 80.dp.toPx()) patientX - 6.dp.toPx() else patientX + 6.dp.toPx()
                boldPaint.textAlign = if (patientX > right - 80.dp.toPx()) Paint.Align.RIGHT else Paint.Align.LEFT
                val labelY = (patientY - 8.dp.toPx()).coerceAtLeast(top + 11.dp.toPx())
                drawContext.canvas.nativeCanvas.drawText(label, labelX, labelY, boldPaint)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LegendItem(PhotoColor, "Phototherapy")
            LegendItem(EscalationColor, "Escalation")
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LegendItem(ExchangeColor, "Exchange")
            LegendItem(PatientColor, if (state.measurementType == MeasurementType.TSB) "Patient TSB" else "Patient TcB")
        }
        Text(
            if (showFullCurve) "Full AAP 2022 curve, 1–336 hours" else "Focused window: ±48 hours around the measured bilirubin",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).background(color, CircleShape))
        Spacer(Modifier.width(5.dp))
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}
