package com.safeshield.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safeshield.app.domain.model.RiskLevel
import com.safeshield.app.domain.model.Severity
import com.safeshield.app.ui.theme.CriticalRed
import com.safeshield.app.ui.theme.DangerRed
import com.safeshield.app.ui.theme.NeutralGrey
import com.safeshield.app.ui.theme.SafeGreen
import com.safeshield.app.ui.theme.WarnAmber

fun severityColor(severity: Severity): Color = when (severity) {
    Severity.CRITICAL -> CriticalRed
    Severity.HIGH -> DangerRed
    Severity.MEDIUM -> WarnAmber
    Severity.LOW -> NeutralGrey
    Severity.CLEAN -> SafeGreen
}

fun riskColor(level: RiskLevel): Color = when (level) {
    RiskLevel.HIGH -> DangerRed
    RiskLevel.MEDIUM -> WarnAmber
    RiskLevel.LOW -> SafeGreen
}

fun scoreColor(score: Int): Color = when {
    score >= 80 -> SafeGreen
    score >= 50 -> WarnAmber
    else -> DangerRed
}

/** Animated ring used for the security score and the WiFi rating. */
@Composable
fun ScoreRing(
    score: Int,
    modifier: Modifier = Modifier,
    label: String? = null,
    strokeWidth: Float = 26f,
    content: @Composable (() -> Unit)? = null,
) {
    val animated by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(900),
        label = "score",
    )
    val color by animateColorAsState(scoreColor(score), label = "scoreColor")
    val track = MaterialTheme.colorScheme.surfaceVariant

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxWidth().height(0.dp)) { }
        Canvas(modifier = Modifier.matchParentSize()) {
            drawArc(
                color = track,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(color.copy(alpha = 0.65f), color)),
                startAngle = 135f,
                sweepAngle = 270f * animated,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (content != null) {
                content()
            } else {
                Text(
                    text = "$score",
                    fontSize = 46.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
                label?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = NeutralGrey)
                }
            }
        }
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
fun ToolRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
) {
    SectionCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp).size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                subtitle?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = NeutralGrey)
                }
            }
        }
    }
}

@Composable
fun SeverityChip(severity: Severity, text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = severityColor(severity).copy(alpha = 0.14f),
    ) {
        Text(
            text = text,
            color = severityColor(severity),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun RiskChip(level: RiskLevel, text: String) {
    Surface(shape = RoundedCornerShape(50), color = riskColor(level).copy(alpha = 0.14f)) {
        Text(
            text = text,
            color = riskColor(level),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = NeutralGrey,
        )
    }
}
