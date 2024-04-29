package com.practic.usbterminal.screens.terminal

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.practic.usbterminal.settings.model.SettingsRepository
import com.practic.usbterminal.ui.theme.UsbTerminalTheme


@Composable
fun TextToXmitInputField(
    inputMode: Int,
    textToXmit: State<String>,
    textToXmitCharByChar: State<TextFieldValue>,
    onXmitCharByCharKBInput: (text: TextFieldValue) -> Unit,
    onTextChanged: (text: String) -> Unit,
    onXmitText: () -> Unit,
    mainFocusRequester: FocusRequester,
    auxFocusRequester: FocusRequester,
) {
    if (inputMode == SettingsRepository.InputMode.WHOLE_LINE) {
        WholeLineTextToXmitInputField(textToXmit, onTextChanged, onXmitText, mainFocusRequester)
    } else {
        CharByCharTextToXmitInputField(
            textToXmitCharByChar,
            onXmitCharByCharKBInput,
            mainFocusRequester,
            auxFocusRequester
        )
    }
}

@Composable
fun CharByCharTextToXmitInputField(
    textToXmit: State<TextFieldValue>,
    onTextChanged: (textFieldValue: TextFieldValue) -> Unit,
    mainFocusRequester: FocusRequester,
    auxFocusRequester: FocusRequester,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(0.dp)
    ) {
        TextField(
            value = textToXmit.value,
            onValueChange = { tfv -> onTextChanged(tfv) },
            modifier = Modifier
                .padding(0.dp)
                .height(1.dp)
                .width(44.dp)
                .focusRequester(mainFocusRequester),
            maxLines = 1,
            textStyle = TextStyle.Default.copy(
                fontSize = 1.sp,
                background = Color.Transparent,
            ),
            colors = TextFieldDefaults.textFieldColors(
                textColor = Color(0x0a000000),
                disabledTextColor = Color.Transparent,
                backgroundColor = Color.Transparent,
                cursorColor = Color.Transparent,
                errorCursorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                leadingIconColor = Color.Transparent,
                disabledLeadingIconColor = Color.Transparent,
                errorLeadingIconColor = Color.Transparent,
                trailingIconColor = Color.Transparent,
                disabledTrailingIconColor = Color.Transparent,
                errorTrailingIconColor = Color.Transparent,
                focusedLabelColor = Color.Transparent,
                unfocusedLabelColor = Color.Transparent,
                disabledLabelColor = Color.Transparent,
                errorLabelColor = Color.Transparent,
                placeholderColor = Color.Transparent,
                disabledPlaceholderColor = Color.Transparent,
            ),
        )
        TextField(
            value = "",
            onValueChange = {},
            modifier = Modifier
                .padding(0.dp)
                .height(1.dp)
                .focusRequester(auxFocusRequester),
            maxLines = 1,
            textStyle = TextStyle.Default.copy(
                fontSize = 1.sp,
                color = Color.Transparent,
                background = Color.Transparent,
            ),
            colors = TextFieldDefaults.textFieldColors(
                textColor = Color.Red,
                disabledTextColor = Color.Transparent,
                backgroundColor = Color.Transparent,
                cursorColor = Color.Transparent,
                errorCursorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent,
                leadingIconColor = Color.Transparent,
                disabledLeadingIconColor = Color.Transparent,
                errorLeadingIconColor = Color.Transparent,
                trailingIconColor = Color.Transparent,
                disabledTrailingIconColor = Color.Transparent,
                errorTrailingIconColor = Color.Transparent,
                focusedLabelColor = Color.Transparent,
                unfocusedLabelColor = Color.Transparent,
                disabledLabelColor = Color.Transparent,
                errorLabelColor = Color.Transparent,
                placeholderColor = Color.Transparent,
                disabledPlaceholderColor = Color.Transparent,
            ),
        )
    }
}

@Composable
fun WholeLineTextToXmitInputField(
    textToXmit: State<String>,
    onTextChanged: (text: String) -> Unit,
    onXmitText: () -> Unit,
    focusRequester: FocusRequester,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        BasicTextField(
            value = textToXmit.value,
            onValueChange = onTextChanged,
            textStyle = TextStyle.Default.copy(
                fontSize = 16.sp,
                color = Color.Blue,
                background = Color.LightGray,
            ),
            modifier = Modifier
                .padding(start = 0.dp, top = 2.dp)
                .horizontalScroll(rememberScrollState())
                .weight(1f)
                .focusRequester(focusRequester),
            maxLines = 2,
        ) { innerTextField ->
            Box(
                modifier = Modifier
                    .background(
                        color = UsbTerminalTheme.extendedColors.textToXmitInputFieldBackgroundColor,
                        shape = RoundedCornerShape(2.dp)
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            UsbTerminalTheme.extendedColors.textToXmitInputFieldBorderColor
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                innerTextField()
            }
        }

        IconButton(
            onClick = {
                onXmitText()
            },
        ) {
            Icon(
                Icons.Filled.Send,
                contentDescription = "Send",
                tint = MaterialTheme.colors.primary,
            )
        }
    }
}