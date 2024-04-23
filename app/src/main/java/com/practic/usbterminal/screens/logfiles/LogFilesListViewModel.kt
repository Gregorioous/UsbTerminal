package com.practic.usbterminal.screens.logfiles

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.practic.usbterminal.R
import com.practic.usbterminal.UsbTerminalApplication
import com.practic.usbterminal.main.MainViewModel
import com.practic.usbterminal.settings.model.SettingsRepository
import com.practic.usbterminal.usbcommservice.LogFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


class LogFilesListViewModel(
    application: Application,
    private val mainViewModel: MainViewModel,
) : AndroidViewModel(application) {

    data class LogFileListItemModel(
        val fileName: String,
        val fileSize: Long,
        val isSelected: Boolean = false
    )

    private val _filesList = MutableStateFlow<List<LogFileListItemModel>>(emptyList())
    val filesList = _filesList.asStateFlow()
    val nSelected = _filesList.transform { list ->
        emit(list.count { it.isSelected })
    }.onEach { count ->
        if (count > 0) {
            mainViewModel.setIsTopBarInContextualMode(true)
            mainViewModel.setTopBarTitle(R.string.log_files_screen_top_appbar_cab_title, count)
        } else {
            mainViewModel.setIsTopBarInContextualMode(false)
            mainViewModel.setTopBarTitle(R.string.log_files_screen_top_appbar_normal_title)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
        initialValue = 0
    )

    var shouldDisplayDeleteConfirmationDialog by mutableStateOf(false)
        private set

    fun onDeleteConfirmationDialogDismissed() {
        shouldDisplayDeleteConfirmationDialog = false
    }

    fun onDeleteConfirmed() {
        deleteSelectedFiles()
        shouldDisplayDeleteConfirmationDialog = false
    }

    private val _listIsRefreshing = mutableStateOf(false)
    val listIsRefreshing: State<Boolean> = _listIsRefreshing

    private val _shouldViewFile = MutableStateFlow(Uri.EMPTY)
    val shouldViewFile = _shouldViewFile.asStateFlow()
    fun fileViewed() {
        _shouldViewFile.value = Uri.EMPTY
    }

    data class FileSharingInfo(
        val uriList: List<Uri>? = null,
        val mimeType: String? = null,
        @StringRes val subjectLine: Int = -1,
        @StringRes val chooserTitle: Int = -1,
    )

    private val _shouldShareFile = MutableStateFlow(FileSharingInfo())
    val shouldShareFile = _shouldShareFile.asStateFlow()
    fun fileShareHandled() {
        _shouldShareFile.value = FileSharingInfo()
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _filesList.value = getFilesListFromDisk()
        }

        mainViewModel.topBarClearButtonClicked.onEach {
            if (it) {
                clearAllSelections()
                mainViewModel.topBarClearButtonHandled()
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = false)
    }

    inner class LogFileNameComparator(private val sortOrder: Int?) :
        Comparator<LogFileListItemModel> {
        override fun compare(o1: LogFileListItemModel, o2: LogFileListItemModel): Int {
            return if (sortOrder == SettingsRepository.LogFilesListSortingOrder.DESCENDING)
                o1.fileName.compareTo(o2.fileName)
            else
                o2.fileName.compareTo(o1.fileName)
        }
    }

    private fun getFilesListFromDisk(): List<LogFileListItemModel> {
        val dir = LogFile.getLogFilesDir(getApplication())
        return dir?.listFiles()
            ?.map { file -> LogFileListItemModel(file.name, file.length()) }
            ?.sortedWith(LogFileNameComparator(mainViewModel.settingsRepository.settingsStateFlow.value.logFilesListSortingOrder))
            ?: emptyList()
    }

    fun onToggleSortOrderClick() {
        Timber.d("onToggleSortOrderClick()")
        val currentSortOrder =
            mainViewModel.settingsRepository.settingsStateFlow.value.logFilesListSortingOrder
        val newSortOrder =
            if (currentSortOrder == SettingsRepository.LogFilesListSortingOrder.ASCENDING)
                SettingsRepository.LogFilesListSortingOrder.DESCENDING
            else
                SettingsRepository.LogFilesListSortingOrder.ASCENDING
        mainViewModel.settingsRepository.setLogFilesSortingOrder(newSortOrder)
        _filesList.value = _filesList.value.sortedWith(LogFileNameComparator(newSortOrder))
    }

    fun onPreviewFileButtonClick() {
        val selectedFile = _filesList.value.firstOrNull { it.isSelected }
        if (selectedFile == null) {
            Timber.e("onPreviewFileButtonClick(): selectedFile=null")
            return
        }

        val logFilesDir = LogFile.getLogFilesDir(getApplication())
        if (logFilesDir == null) {
            Timber.e("onPreviewFileButtonClick(): logFilesDir=null")
            return
        }

        val file = File(logFilesDir, selectedFile.fileName)
        val context = getApplication<Application>()
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        _shouldViewFile.value = uri
    }

    fun onShareButtonClick() {
        viewModelScope.launch(Dispatchers.IO) {
            Timber.d("onShareButtonClick()")

            val fileNamesList = _filesList.value.filter { it.isSelected }
                .map { logFileListItemModel -> logFileListItemModel.fileName }

            if (mainViewModel.settingsRepository.settingsStateFlow.value.zipLogFilesWhenSharing) {
                val zipFile = zipFiles(fileNamesList) ?: run {
                    Timber.e("Error when trying to zip log files")
                    return@launch
                }
                val context = getApplication<Application>()
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    zipFile
                )

                _shouldShareFile.value = FileSharingInfo(
                    uriList = listOf(uri),
                    mimeType = "application/zip",
                    subjectLine = R.string.log_files_sharing_subject_line,
                    chooserTitle = R.string.log_files_sharing_chooser_title
                )
            } else {
                val logFilesDir = LogFile.getLogFilesDir(getApplication()) ?: run {
                    Timber.e("Error when trying to get log file directory")
                    return@launch
                }
                val uriList = fileNamesList.map { fileName ->
                    val file = File(logFilesDir, fileName)
                    val context = getApplication<Application>()
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                }

                _shouldShareFile.value = FileSharingInfo(
                    uriList = uriList,
                    mimeType = "text/plain",
                    subjectLine = R.string.log_files_sharing_subject_line,
                    chooserTitle = R.string.log_files_sharing_chooser_title
                )
            }
        }
    }

    fun onRefreshRequested() {
        Timber.d("onRefreshButtonClick()")
        _listIsRefreshing.value = true
        _filesList.value = getFilesListFromDisk()
        _listIsRefreshing.value = false
    }

    fun onDeleteButtonClick() {
        Timber.d("onDeleteButtonClick()")
        shouldDisplayDeleteConfirmationDialog = true
    }

    fun onLogFilesListItemClick(itemIndex: Int) {
        _filesList.value = _filesList.value
            .mapIndexed { index, item ->
                LogFileListItemModel(
                    fileName = item.fileName,
                    fileSize = item.fileSize,
                    isSelected = if (index == itemIndex) !item.isSelected else item.isSelected
                )
            }
    }

    fun clearAllSelections() {
        _filesList.value = _filesList.value
            .map { item -> item.copy(isSelected = false) }
    }

    private fun deleteSelectedFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val dir = LogFile.getLogFilesDir(getApplication()) ?: return@launch
            _filesList.value.filter { it.isSelected }.forEach { logFileListItemModel ->
                Timber.d("Deleting ${logFileListItemModel.fileName}")
                val file = File(dir, logFileListItemModel.fileName)
                if (!file.delete()) {
                    Timber.e("Can't delete file: '${file.name}'")
                }
            }
            _filesList.value = getFilesListFromDisk()
        }
    }

    private fun zipFiles(fileNamesList: List<String>): File? {
        val cacheDir = getApplication<UsbTerminalApplication>().cacheDir
        val outputFileName = LogFile.generateFileName() + ".zip"
        val outputFile = File(cacheDir, outputFileName)
        val zos = ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile)))
        val logFilesDir = LogFile.getLogFilesDir(getApplication()) ?: return null
        fileNamesList.forEach { fileName ->
            zos.putNextEntry(ZipEntry(fileName))
            val bis = BufferedInputStream(FileInputStream(File(logFilesDir, fileName)))
            bis.copyTo(zos)
            bis.close()
        }
        zos.close()
        return outputFile
    }

    fun onScreenVisible() {
        _filesList.value = getFilesListFromDisk()
    }

    fun onScreenHidden() {
        // Timber.d("onScreenHidden()")
    }

    class Factory(
        private val application: Application,
        private val mainViewModel: MainViewModel,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LogFilesListViewModel(application, mainViewModel) as T
        }
    }
}