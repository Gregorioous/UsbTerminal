package com.practic.usbterminal.settings.ui.lib.internal

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.practic.usbterminal.R

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SingleChoiceWithFreeInputFieldDialog(
    title: @Composable () -> Unit,
    choices: List<String>,
    freeInputFieldValue: String,
    freeInputFieldLabel: String,
    freeInputFieldIsValid: Boolean,
    initiallySelectedIndex: Int,
    bottomBlockContent: (@Composable ColumnScope.() -> Unit)?,
    onFreeInputFieldChange: ((String) -> Unit)?,
    onCancel: () -> Unit,
    keyboardOptions: KeyboardOptions,
    onSelection: (index: Int, freeInputFieldValue: String) -> Unit,
) {
    var freeInputFieldValueLocal by remember { mutableStateOf(freeInputFieldValue) }
    var selectedIndex by remember { mutableStateOf(initiallySelectedIndex) }
    val keyboardController = LocalSoftwareKeyboardController.current

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

                RadioButtonGroupWithFreeInputField(
                    labels = choices,
                    freeInputFieldValue = if (onFreeInputFieldChange != null) freeInputFieldValue else freeInputFieldValueLocal,
                    freeInputFieldLabel = freeInputFieldLabel,
                    selectedIndex = selectedIndex,
                    bottomBlockContent = bottomBlockContent,
                    onFreeInputFieldChange = { newText ->
                        if (onFreeInputFieldChange != null) {
                            onFreeInputFieldChange(newText)
                        } else {
                            freeInputFieldValueLocal = newText
                        }
                    },
                    onSelection = { index ->
                        if (index == choices.size) {
                            selectedIndex = index
                        } else {
                            keyboardController?.hide()
                            onSelection(index, choices[index])
                        }
                    },
                    keyboardOptions = keyboardOptions,
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                        onSelection(
                            choices.size,
                            if (onFreeInputFieldChange == null) freeInputFieldValueLocal else freeInputFieldValue
                        )
                    }),
                )

                Spacer(modifier = Modifier.height(8.dp))
                // Cancel button
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onCancel) {
                        Text(text = stringResource(R.string.cancel_all_caps))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        enabled = freeInputFieldIsValid,
                        onClick = {
                            keyboardController?.hide()
                            onSelection(
                                choices.size,
                                if (onFreeInputFieldChange == null) freeInputFieldValueLocal else freeInputFieldValue
                            )
                        }
                    ) {
                        Text(text = stringResource(R.string.ok))
                    }
                }
            }
        }
    }
}

@ExperimentalComposeUiApi
@Composable
fun RadioButtonGroupWithFreeInputField(
    labels: List<String>,
    freeInputFieldLabel: String,
    freeInputFieldValue: String,
    selectedIndex: Int,
    bottomBlockContent: (@Composable ColumnScope.() -> Unit)?,
    onFreeInputFieldChange: (newText: String) -> Unit,
    onSelection: (index: Int) -> Unit,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
) {
    Column(
        modifier = Modifier
            .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
    ) {
        labels.forEachIndexed { index, label ->
            RadioButtonWithLabel(
                label = label,
                isSelected = index == selectedIndex,
                onClick = { onSelection(index) },
            )
        }
        RadioButtonWithFreeInputField(
            label = freeInputFieldLabel,
            freeInputFieldValue = freeInputFieldValue,
            isSelected = selectedIndex == labels.size,
            onTextChange = onFreeInputFieldChange,
            onSelect = { onSelection(labels.size) },
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
        )
        if (bottomBlockContent != null) {
            Spacer(modifier = Modifier.height(16.dp))
            bottomBlockContent()
        }
    }
}

@ExperimentalComposeUiApi
@Preview
@Composable
fun RadioButtonGroupWithFreeInputFieldPreview() {
    val labels = listOf("Label 1","Label 2", "Label 3")
    RadioButtonGroupWithFreeInputField(
        labels,
        freeInputFieldValue = "FREE",
        freeInputFieldLabel = "Any baud rate",
        selectedIndex = 1,
        bottomBlockContent = {
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(color = Color.Black),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = "Text color - Green",
                    color = Color(0xffff6c00),
                    modifier = Modifier
                        .padding(start = 8.dp),
                )
            }
        },
        onFreeInputFieldChange = {},
        onSelection = {},
        keyboardOptions = KeyboardOptions(),
        keyboardActions = KeyboardActions(),
    )
}

@Composable
fun RadioButtonWithLabel(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(true, onClick = { onClick() })
            .padding(top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            modifier = Modifier
                .align(Alignment.CenterVertically),
        )
        Text(
            text = label,
            modifier = Modifier
                .padding(start = 8.dp)
                .align(Alignment.CenterVertically),
        )
    }
}

@Composable
fun RadioButtonWithFreeInputField(
    label: String,
    freeInputFieldValue: String,
    isSelected: Boolean,
    onTextChange: (newText: String) -> Unit,
    onSelect: () -> Unit,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
) {

    val focusRequester = FocusRequester()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(true, onClick = onSelect)
            .padding(top = 6.dp, bottom = 6.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = isSelected,
            onClick = {
                onSelect()
                focusRequester.requestFocus()
            },
            modifier = Modifier
                .align(Alignment.CenterVertically),
        )

        OutlinedTextField(
            value = freeInputFieldValue,
            onValueChange = { onTextChange(it) },
            label = { Text(label) },
            modifier = Modifier
                .padding(start = 8.dp, top = 2.dp)
                .align(Alignment.CenterVertically)
                .weight(1f)
                .horizontalScroll(rememberScrollState())
                .focusRequester(focusRequester)
                .onFocusChanged {
                    if (it.isFocused) onSelect()
                },
            maxLines = 1,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
        )
    }
}