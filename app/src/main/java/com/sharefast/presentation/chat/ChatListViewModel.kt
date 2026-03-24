package com.sharefast.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sharefast.data.repository.ChatRepository
import com.sharefast.data.repository.ChatThread
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    chatRepository: ChatRepository,
) : ViewModel() {
    val threads: StateFlow<List<ChatThread>> =
        chatRepository.threads().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

