package com.practic.usbterminal.screens.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.practic.usbterminal.R
import com.practic.usbterminal.main.MainViewModel
import com.practic.usbterminal.ui.theme.UsbTerminalTheme

@Composable
fun CtrlButtonsRow(
    mainViewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val buttonModifier = Modifier
        .widthIn(min = 40.dp)
        .height(36.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(color = UsbTerminalTheme.extendedColors.ctrlButtonsLineBackgroundColor)
            .padding(vertical = 1.dp, horizontal = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ToggleButton(
            isSelected = mainViewModel.userInputHandler.ctrlButtonIsSelected.value,
            modifier = buttonModifier,
            onClick = { mainViewModel.userInputHandler.onCtrlKeyButtonClick() }
        ) {
            Text(
                text = stringResource(R.string.ctrl),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colors.onBackground,
            )
        }
        OutlinedButton(
            modifier = buttonModifier,
            onClick = { mainViewModel.userInputHandler.onTabButtonClick() }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_baseline_keyboard_tab_24),
                tint = MaterialTheme.colors.onBackground,
                contentDescription = stringResource(R.string.tab)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        OutlinedButton(
            modifier = buttonModifier,
            onClick = { mainViewModel.userInputHandler.onLeftButtonClick() }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_baseline_arrow_back_24),
                tint = MaterialTheme.colors.onBackground,
                contentDescription = stringResource(R.string.left)
            )
        }
        OutlinedButton(
            modifier = buttonModifier,
            onClick = { mainViewModel.userInputHandler.onDownButtonClick() }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_baseline_arrow_downward_24),
                tint = MaterialTheme.colors.onBackground,
                contentDescription = stringResource(R.string.down)
            )
        }
        OutlinedButton(
            modifier = buttonModifier,
            onClick = { mainViewModel.userInputHandler.onUpButtonClick() }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_baseline_arrow_upward_24),
                tint = MaterialTheme.colors.onBackground,
                contentDescription = stringResource(R.string.up)
            )
        }
        OutlinedButton(
            modifier = buttonModifier,
            onClick = { mainViewModel.userInputHandler.onRightButtonClick() }
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_baseline_arrow_forward_24),
                tint = MaterialTheme.colors.onBackground,
                contentDescription = stringResource(R.string.right)
            )
        }
    }
}