package com.safeshield.app.feature.scan

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safeshield.app.R
import com.safeshield.app.core.Formatters
import com.safeshield.app.domain.model.ScanState
import com.safeshield.app.ui.components.SectionCard
import com.safeshield.app.ui.theme.DangerRed
import com.safeshield.app.ui.theme.NeutralGrey

/**
 * Honest progress: real percentage, the item being checked right now, a live
 * threat counter and a genuine ETA derived from measured throughput.
 */
@Composable
fun ScanScreen(
    onFinished: (Long) -> Unit,
    onCancelled: () -> Unit,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val finishedId by viewModel.finishedScanId.collectAsStateWithLifecycle()

    LaunchedEffect(progress.state, finishedId) {
        when (progress.state) {
            ScanState.COMPLETED -> finishedId?.let(onFinished)
            ScanState.CANCELLED, ScanState.IDLE -> onCancelled()
            else -> Unit
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = progress.percent / 100f,
        animationSpec = tween(400),
        label = "progress",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(
                if (progress.state == ScanState.PAUSED) R.string.paused else R.string.scanning
            ),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(Modifier.height(12.dp))

        Text(
            text = "${progress.percent}%",
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(16.dp))

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.fillMaxWidth().height(10.dp),
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = progress.currentItem.ifBlank { "…" },
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(20.dp))

        SectionCard {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        stringResource(R.string.scanned_count, progress.scanned, progress.total),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        stringResource(R.string.threats_found, progress.threats),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (progress.threats > 0) DangerRed else NeutralGrey,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    text = if (progress.etaMillis > 0) {
                        stringResource(R.string.time_remaining, Formatters.duration(progress.etaMillis))
                    } else {
                        Formatters.duration(progress.elapsedMillis)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeutralGrey,
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (progress.state == ScanState.PAUSED) {
                Button(onClick = viewModel::resume, shape = RoundedCornerShape(24.dp)) {
                    Icon(Icons.Filled.PlayArrow, null)
                    Spacer(Modifier.height(0.dp))
                    Text(stringResource(R.string.resume))
                }
            } else {
                Button(onClick = viewModel::pause, shape = RoundedCornerShape(24.dp)) {
                    Icon(Icons.Filled.Pause, null)
                    Text(stringResource(R.string.pause))
                }
            }
            OutlinedButton(
                onClick = { viewModel.cancel(); onCancelled() },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
            ) {
                Icon(Icons.Filled.Close, null)
                Text(stringResource(R.string.cancel))
            }
        }

        progress.error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = DangerRed, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
