package com.sharefast.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharefast.data.repository.AppearancePreferencesStore
import com.sharefast.presentation.theme.AccentPresetCount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainAppearanceViewModel @Inject constructor(
    private val appearancePreferencesStore: AppearancePreferencesStore,
) : ViewModel() {

    val accentIndex = appearancePreferencesStore.accentIndex.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        0,
    )

    val greetingName = appearancePreferencesStore.greetingName.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        "",
    )

    fun cycleAccent() {
        viewModelScope.launch {
            val cur = appearancePreferencesStore.accentIndex.first()
            appearancePreferencesStore.setAccentIndex((cur + 1) % AccentPresetCount)
        }
    }

    fun setGreeting(name: String) {
        viewModelScope.launch {
            appearancePreferencesStore.setGreetingName(name)
        }
    }
}
