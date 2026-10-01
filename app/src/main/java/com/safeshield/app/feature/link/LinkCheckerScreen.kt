package com.safeshield.app.feature.link

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.safeshield.app.R
import com.safeshield.app.ui.components.SectionCard
import com.safeshield.app.ui.theme.DangerRed
import com.safeshield.app.ui.theme.SafeGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkCheckerScreen(
    prefilledUrl: String? = null,
    onPrefillConsumed: () -> Unit = {},
    onBack: () -> Unit,
    viewModel: LinkViewModel = hiltViewModel(),
) {
    val url by viewModel.url.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()

    LaunchedEffect(prefilledUrl) {
        if (!prefilledUrl.isNullOrBlank()) {
            viewModel.onUrlChange(prefilledUrl)
            viewModel.check()
            onPrefillConsumed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.link_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = url,
                onValueChange = viewModel::onUrlChange,
                label = { Text(stringResource(R.string.link_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = viewModel::check,
                enabled = !loading && url.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.link_check)) }

            if (loading) {
                CircularProgressIndicator()
                Spacer(Modifier.height(4.dp))
            }

            when (val r = result) {
                is LinkUiResult.Safe -> SectionCard {
                    Text(
                        stringResource(R.string.link_safe),
                        color = SafeGreen,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(r.verdict.url, style = MaterialTheme.typography.bodySmall)
                }
                is LinkUiResult.Unsafe -> SectionCard {
                    Text(
                        stringResource(
                            R.string.link_unsafe,
                            r.verdict.threatTypes.joinToString(", "),
                        ),
                        color = DangerRed,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(r.verdict.url, style = MaterialTheme.typography.bodySmall)
                }
                LinkUiResult.Invalid -> SectionCard { Text(stringResource(R.string.link_invalid)) }
                LinkUiResult.NeedsOnline -> SectionCard { Text(stringResource(R.string.link_needs_online)) }
                LinkUiResult.NotConfigured -> SectionCard { Text(stringResource(R.string.error_api_key)) }
                is LinkUiResult.Error -> SectionCard {
                    Text(stringResource(R.string.error_generic) + " (${r.message})")
                }
                null -> Unit
            }
        }
    }
}
