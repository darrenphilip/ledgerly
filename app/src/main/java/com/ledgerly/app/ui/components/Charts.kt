package com.ledgerly.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ledgerly.app.ui.theme.ledgerColors
import com.ledgerly.app.util.BudgetMath
import com.ledgerly.app.util.DateFormats
import com.ledgerly.app.util.Money
import com.ledgerly.app.util.MonthPoint
import java.time.YearMonth

data class DonutSegment(
    val color: Color,
    val value: Long
)

@Composable
fun DonutChart(
    segments: List<DonutSegment>,
    modifier: Modifier = Modifier,
    size: Dp = 148.dp,
    strokeWidth: Dp = 18.dp,
    center: @Composable () -> Unit = {}
) {
    val progress = remember { Animatable(0f) }
    val total = segments.sumOf { it.value }.coerceAtLeast(1L)
    val trackColor = MaterialTheme.ledgerColors.chartTrack

    LaunchedEffect(segments) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 850, easing = FastOutSlowInEasing))
    }

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            val inset = stroke / 2f
            val arcSize = Size(this.size.width - stroke, this.size.height - stroke)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            var start = -90f
            val gap = if (segments.size > 1) 3.5f else 0f
            segments.forEach { segment ->
                val sweep = segment.value.toFloat() / total * 360f
                if (sweep > 0.4f) {
                    val drawSweep = (sweep - gap).coerceAtLeast(1.5f) * progress.value
                    drawArc(
                        color = segment.color,
                        startAngle = start + gap / 2f,
                        sweepAngle = drawSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
                start += sweep
            }
        }
        center()
    }
}

@Composable
fun TrendBarChart(
    points: List<MonthPoint>,
    currency: String,
    highlight: YearMonth?,
    modifier: Modifier = Modifier
) {
    val max = points.maxOfOrNull { it.spentCents }?.coerceAtLeast(1L) ?: 1L
    val primary = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        points.forEach { point ->
            val hasSpend = point.spentCents > 0L
            val fraction = point.spentCents.toFloat() / max
            val animated by animateFloatAsState(
                targetValue = if (hasSpend) fraction.coerceAtLeast(0.04f) else 0f,
                animationSpec = tween(700, easing = FastOutSlowInEasing),
                label = "bar"
            )
            val isHighlight = point.month == highlight
            val barBrush: Brush = if (isHighlight) {
                Brush.verticalGradient(listOf(primary, primary.copy(alpha = 0.55f)))
            } else {
                SolidColor(primary.copy(alpha = 0.18f))
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (hasSpend) Money.compact(point.spentCents, currency) else "—",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isHighlight) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(108.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (animated > 0.001f) Box(
                        modifier = Modifier
                            .fillMaxWidth(0.62f)
                            .height(108.dp * animated)
                            .clip(RoundedCornerShape(topStart = 9.dp, topEnd = 9.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                            .background(barBrush)
                    )
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    text = DateFormats.monthShort(point.month),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Medium,
                    color = if (isHighlight) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
