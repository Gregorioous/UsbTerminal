package com.practic.usbterminal.screens.logfiles

import android.app.Application
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.AlertDialog
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ShareCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.practic.usbterminal.R
import com.practic.usbterminal.main.MainViewModel
import com.practic.usbterminal.main.UsbTerminalScreenAttributes
import com.practic.usbterminal.ui.theme.UsbTerminalTheme
import com.practic.usbterminal.utill.collectAsStateLifecycleAware
import com.practic.usbterminal.utill.getActivity
import timber.log.Timber

object LogFilesListScreenAttributes : UsbTerminalScreenAttributes(
    isTopInBackStack = false,
    route = "LogFiles",
) {
    override fun getTopAppBarActions(
        mainViewModel: MainViewModel,
        isTopBarInContextualMode: Boolean
    ): @Composable RowScope.() -> Unit =
        { LogFilesListTopAppBarActions(mainViewModel, isTopBarInContextualMode) }
}

@Composable
fun LogFilesListTopAppBarActions(mainViewModel: MainViewModel, isTopBarInContextualMode: Boolean) {
    val logFilesListViewModel: LogFilesListViewModel = viewModel(
        factory = LogFilesListViewModel.Factory(
            application = LocalContext.current.applicationContext as Application,
            mainViewModel = mainViewModel,
        )
    )
    val nSelected by logFilesListViewModel.nSelected.collectAsStateLifecycleAware()

    if (isTopBarInContextualMode) {
        IconButton(
            onClick = { logFilesListViewModel.onDeleteButtonClick() }
        ) {
            Icon(
                modifier = Modifier.padding(start = 0.dp, end = 0.dp),
                painter = painterResource(id = R.drawable.ic_baseline_delete_24),
                contentDescription = stringResource(R.string.delete),
                tint = UsbTerminalTheme.extendedColors.contextualAppBarOnBackground,
            )
        }
        IconButton(
            onClick = { logFilesListViewModel.onShareButtonClick() }
        ) {
            Icon(
                modifier = Modifier.padding(start = 0.dp, end = 0.dp),
                painter = painterResource(id = R.drawable.ic_baseline_share_24),
                contentDescription = stringResource(R.string.share),
                tint = UsbTerminalTheme.extendedColors.contextualAppBarOnBackground,
            )
        }
        if (nSelected == 1) {
            IconButton(
                onClick = { logFilesListViewModel.onPreviewFileButtonClick() }
            ) {
                Icon(
                    modifier = Modifier.padding(start = 0.dp, end = 0.dp),
                    painter = painterResource(id = R.drawable.ic_baseline_preview_24),
                    contentDescription = stringResource(R.string.preview),
                    tint = UsbTerminalTheme.extendedColors.contextualAppBarOnBackground,
                )
            }
        }
    } else {
        IconButton(
            onClick = { logFilesListViewModel.onToggleSortOrderClick() }
        ) {
            Icon(
                modifier = Modifier.padding(start = 0.dp, end = 0.dp),
                painter = painterResource(id = R.drawable.ic_baseline_height_24),
                contentDescription = stringResource(R.string.toggle_sort_order),
            )
        }
    }
}


@Composable
fun LogFilesListScreen(
    mainViewModel: MainViewModel
) {
    val viewModel: LogFilesListViewModel = viewModel(
        factory = LogFilesListViewModel.Factory(
            application = LocalContext.current.applicationContext as Application,
            mainViewModel = mainViewModel,
        )
    )

    val fileNamesList by viewModel.filesList.collectAsStateLifecycleAware()
    val nSelected by viewModel.nSelected.collectAsStateLifecycleAware()
    val shouldDisplayDeleteConfirmationDialog = viewModel.shouldDisplayDeleteConfirmationDialog

    DisposableEffect(true) {
        viewModel.onScreenVisible()
        onDispose {
            viewModel.onScreenHidden()
        }
    }

    BackHandler(
        enabled = nSelected > 0,
        onBack = {
            Timber.d("LogFilesListScreen#BackHandler.onBack called")
            viewModel.clearAllSelections()
        }
    )

    val context = LocalContext.current

    LaunchedEffect(context) {
        viewModel.shouldViewFile.collect { uri ->
            val activity = context.getActivity()
            if (!Uri.EMPTY.equals(uri)) {
                if (activity != null) {
                    val myIntent = Intent(Intent.ACTION_VIEW)
                    myIntent.setDataAndType(uri, "text/plain")
                    myIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION /*or Intent.FLAG_GRANT_WRITE_URI_PERMISSION*/)
                    try {
                        startActivity(activity, myIntent, null)
                    } catch (e: ActivityNotFoundException) {
                        Timber.e("No app that can display text/plain")
                    } catch (e: Exception) {
                        Timber.e("Can't start activity to display:'${uri}'. Exception: ${e.message}")
                    }
                }
                viewModel.fileViewed()
            }
        }
    }

    LaunchedEffect(context) {
        val activity = context.getActivity()
        viewModel.shouldShareFile.collect { fileShareInfo ->
            if (fileShareInfo.uriList != null) {
                if (activity != null) {
                    val emailAddresses =
                        mainViewModel.settingsRepository.settingsStateFlow.value.emailAddressForSharing.split(
                            ","
                        )
                            .toTypedArray()
                    val intentBuilder = ShareCompat.IntentBuilder(activity)
                        .setType(fileShareInfo.mimeType)
                        .setSubject(context.getString(fileShareInfo.subjectLine))
                        .setEmailTo(emailAddresses)
                        .setChooserTitle(fileShareInfo.chooserTitle)
                    fileShareInfo.uriList.forEach { uri ->
                        intentBuilder.addStream(uri)
                    }
                    val intent = intentBuilder.intent
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                    val chooserIntent =
                        Intent.createChooser(intent, context.getString(fileShareInfo.chooserTitle))
                    try {
                        startActivity(activity, chooserIntent, null)
                    } catch (e: Exception) {
                        Timber.e("Can't share URI:'${fileShareInfo.uriList.firstOrNull() ?: "null"}'. Exception: ${e.message}")
                    }
                }

                viewModel.fileShareHandled()
            }
        }
    }
}

@Composable
private fun LogFilesList(
    fileNamesList: List<LogFilesListViewModel.LogFileListItemModel>,
    onItemClick: (itemId: Int) -> Unit,
    shouldDisplayDeleteConfirmationDialog: Boolean,
    nSelected: Int,
    onDeleteConfirmationDialogDismissed: () -> Unit,
    onDeleteConfirmed: () -> Unit,
) {
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
    ) {
        items(count = fileNamesList.size) { itemIndex ->
            LogFileListItem(
                logFileItemModel = fileNamesList[itemIndex],
                itemIndex = itemIndex,
                onItemClick = { onItemClick(itemIndex) },
            )
        }
    }

    if (shouldDisplayDeleteConfirmationDialog) {
        DeleteFilesConfirmationDialog(
            nSelected = nSelected,
            onDialogDismiss = onDeleteConfirmationDialogDismissed,
            onConfirm = onDeleteConfirmed
        )
    }
}

@Composable
private fun LogFileListItem(
    logFileItemModel: LogFilesListViewModel.LogFileListItemModel,
    itemIndex: Int,
    onItemClick: (itemIndex: Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick(itemIndex) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = logFileItemModel.fileName,
                fontSize = 16.sp
            )
            Text(
                text = stringResource(R.string.size, logFileItemModel.fileSize),
                fontSize = 14.sp
            )
        }
        Checkbox(
            checked = logFileItemModel.isSelected,
            onCheckedChange = null,
        )
    }
}

@Composable
fun DeleteFilesConfirmationDialog(
    nSelected: Int,
    onDialogDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            Timber.d("onDismissRequest")
            onDialogDismiss()
        },
        title = { Text(text = stringResource(R.string.delete_files)) },
        text = {
            Column {
                Text(text = stringResource(R.string.delete_files_warning, nSelected))
                Text(
                    text = stringResource(R.string.delete_files_are_you_sure),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDialogDismiss()
            }) {
                Text(stringResource(R.string.yes_all_caps))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDialogDismiss() }) {
                Text(stringResource(R.string.cancel_all_caps))
            }
        }
    )
}


@Preview
@Composable
private fun LogFileListItemPreview() {
    Column {
        LogFileListItem(
            logFileItemModel = LogFilesListViewModel.LogFileListItemModel(
                "UsbTerminal_20220224_0832.log",
                777,
                false
            ),
            itemIndex = 0,
            onItemClick = {},
        )
        LogFileListItem(
            logFileItemModel = LogFilesListViewModel.LogFileListItemModel(
                "UsbTerminal_20220224_0832.log",
                12345678,
                true
            ),
            itemIndex = 0,
            onItemClick = {},
        )
    }
}


@Composable
private fun EmptyLogFilesListMessage() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.no_log_file),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.h6
        )
        Spacer(modifier = Modifier.height(30.dp))
        Text(
            text = stringResource(R.string.log_files_how_to),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.body1
        )
    }
}
