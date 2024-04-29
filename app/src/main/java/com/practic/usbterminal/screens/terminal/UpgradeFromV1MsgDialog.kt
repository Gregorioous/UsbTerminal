package com.practic.usbterminal.screens.terminal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.practic.usbterminal.R
import com.practic.usbterminal.ui.util.ComposeWebView

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun UpgradeFromV1MsgDialog(
    onOk: () -> Unit,
    onDecline: () -> Unit,
) {
    Dialog(
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDecline,
    ) {
        Card(
            elevation = 8.dp,
            backgroundColor = MaterialTheme.colors.primary,
            modifier = Modifier.width(LocalConfiguration.current.screenWidthDp.dp),
        ) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxSize()
            ) {
                val mainPageUrl = "https://appassets.androidplatform.net/assets/upgrade_msg.html"
                ComposeWebView(
                    url = mainPageUrl,
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    onPageLoaded = null,
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = onOk,
                    ) {
                        Text(
                            text = stringResource(R.string.ok),
                            color = MaterialTheme.colors.onPrimary,
                        )
                    }
                }
            }
        }
    }
}