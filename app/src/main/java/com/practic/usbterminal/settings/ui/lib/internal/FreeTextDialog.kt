package com.practic.usbterminal.settings.ui.lib.internal

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.practic.usbterminal.R

@Composable
fun FreeTextDialog(
    title: @Composable () -> Unit,
    label: @Composable () -> Unit,
    previousText: String,
    maxLines: Int = 1,
    onTextChange: (String) -> Unit,
    onOk: () -> Unit,
    onCancel: () -> Unit,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
) {
    var value by remember {
        mutableStateOf(TextFieldValue(previousText, TextRange(previousText.length)))
    }
    val focusRequester = remember { FocusRequester() }

    Dialog(onDismissRequest = onCancel) {
        Card(
            elevation = 8.dp,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Box(modifier = Modifier.padding(start = 10.dp, top = 8.dp)) {
                    ProvideTextStyle(value = MaterialTheme.typography.h6) {
                        title()
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it; onTextChange(it.text) },
                    label = label,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 2.dp)
                        .horizontalScroll(rememberScrollState())
                        .focusRequester(focusRequester),
                    maxLines = maxLines,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                )

                Spacer(modifier = Modifier.height(8.dp))
                // OK & Cancel buttons
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onCancel) {
                        Text(text = stringResource(R.string.cancel_all_caps))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(onClick = { onOk() }) {
                        Text(text = stringResource(R.string.ok))
                    }
                }
            }
        }
    }
    LaunchedEffect(true) {
        focusRequester.requestFocus()
    }
}

@Preview
@Composable
fun FreeTextDialogPreview() {
    var value by remember { mutableStateOf(TextFieldValue("previous text")) }

    Card(
        elevation = 8.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Box(modifier = Modifier.padding(start = 10.dp, top = 8.dp)) {
                ProvideTextStyle(value = MaterialTheme.typography.h6) {
                    Text("Title")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Email address") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 2.dp)
                    .horizontalScroll(rememberScrollState()),
                maxLines = 3,
            )

            Spacer(modifier = Modifier.height(8.dp))
            // OK & Cancel buttons
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = {}) {
                    Text(text = "CANCEL")
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = { }) {
                    Text(text = "OK")
                }
            }
        }
    }
}