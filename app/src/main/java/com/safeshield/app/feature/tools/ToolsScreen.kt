package com.safeshield.app.feature.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.safeshield.app.R
import com.safeshield.app.ui.components.ToolRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    onOpenWifi: () -> Unit,
    onOpenAppLock: () -> Unit,
    onOpenJunk: () -> Unit,
    onOpenLink: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.tools_title)) }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ToolRow(Icons.Filled.Wifi, stringResource(R.string.tool_wifi), null, onOpenWifi)
            ToolRow(Icons.Filled.Lock, stringResource(R.string.tool_applock), null, onOpenAppLock)
            ToolRow(Icons.Filled.CleaningServices, stringResource(R.string.tool_junk), null, onOpenJunk)
            ToolRow(Icons.Filled.Link, stringResource(R.string.tool_link), null, onOpenLink)
            ToolRow(Icons.Filled.History, stringResource(R.string.tool_history), null, onOpenHistory)
            ToolRow(Icons.Filled.PrivacyTip, stringResource(R.string.tool_privacy), null, onOpenPrivacy)
        }
    }
}
