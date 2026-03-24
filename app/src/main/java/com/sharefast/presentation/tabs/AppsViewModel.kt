package com.sharefast.presentation.tabs

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharefast.data.repository.SendQueueStore
import com.sharefast.domain.model.InstalledApp
import com.sharefast.domain.model.ShareableFile
import com.sharefast.domain.repository.AppsRepository
import com.sharefast.utils.Feedback
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val appsRepository: AppsRepository,
    private val sendQueueStore: SendQueueStore,
) : ViewModel() {

    private val _apps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val apps: StateFlow<List<InstalledApp>> = _apps.asStateFlow()

    private val _selected = MutableStateFlow<Set<String>>(emptySet())
    val selected: StateFlow<Set<String>> = _selected.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _apps.value = runCatching { appsRepository.loadInstalledApps() }.getOrElse { emptyList() }
        }
    }

    fun toggle(packageName: String) {
        val cur = _selected.value.toMutableSet()
        if (!cur.add(packageName)) cur.remove(packageName)
        _selected.value = cur
    }

    fun clearSelection() {
        _selected.value = emptySet()
    }

    fun addSelectedToQueue() {
        val map = _apps.value.associateBy { it.packageName }
        val files = _selected.value.mapNotNull { pkg ->
            map[pkg]?.let { app ->
                ShareableFile(
                    id = app.packageName,
                    displayName = "${app.label}.apk",
                    sizeBytes = app.sizeBytes,
                    uri = android.net.Uri.EMPTY,
                    localPath = app.apkPath,
                )
            }
        }
        sendQueueStore.addAll(files)
        if (files.isNotEmpty()) Feedback.vibrateQueueAdd(appContext)
        clearSelection()
    }
}
