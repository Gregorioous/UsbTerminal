package com.practic.usbterminal.screens.devicelist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.practic.usbterminal.R

@Composable
fun SelectDeviceTypeAndConnectDialog(
    choices: List<String>,
    selectedIndex: Int,
    onSelection: (deviceTypeIndex: Int) -> Unit,
    onConnect: () -> Unit,
    onCancel: () -> Unit,
) {
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
                Text(
                    text = "Select Type and Connect",
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier.padding(start = 10.dp, top = 8.dp),
                )
                Spacer(modifier = Modifier.height(8.dp))

                RadioButtonGroup(
                    labels = choices,
                    selectedIndex = selectedIndex,
                    onClick = onSelection,
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(
                        onClick = onCancel,
                    ) {
                        Text(text = stringResource(R.string.cancel_all_caps))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = onConnect,
                        enabled = selectedIndex != -1
                    ) {
                        Text(text = stringResource(R.string.connect_all_caps))
                    }
                }
            }
        }
    }
}

@Composable
fun RadioButtonGroup(
    labels: List<String>,
    selectedIndex: Int,
    onClick: (index: Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 10.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
    ) {
        labels.forEachIndexed { index, label ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClick(index) }
                    .padding(top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = index == selectedIndex,
                    onClick = { onClick(index) },
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
    }
}