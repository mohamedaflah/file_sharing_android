package com.sharefast.presentation.navigation

import androidx.lifecycle.ViewModel
import com.sharefast.services.transfer.IncomingTransferApprovalCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class IncomingTransferRequestViewModel @Inject constructor(
    private val coordinator: IncomingTransferApprovalCoordinator,
) : ViewModel() {
    val request = coordinator.request

    fun accept(id: Long) = coordinator.accept(id)

    fun decline(id: Long) = coordinator.decline(id)
}

