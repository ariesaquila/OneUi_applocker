package com.oneui.applocker.ui.permissions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.oneui.applocker.core.permission.PermissionHelper
import com.oneui.applocker.core.permission.PermissionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PermissionItemState(
    val type: PermissionType,
    val isGranted: Boolean
)

data class PermissionsUiState(
    val permissions: List<PermissionItemState> = emptyList(),
    val canProceed: Boolean = false
)

class PermissionsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PermissionsUiState())
    val uiState: StateFlow<PermissionsUiState> = _uiState.asStateFlow()

    init {
        refreshPermissionStates()
    }

    fun refreshPermissionStates() {
        val context = getApplication<Application>()
        val items = PermissionType.entries.map { type ->
            PermissionItemState(
                type = type,
                isGranted = PermissionHelper.isPermissionGranted(context, type)
            )
        }
        val mandatoryGranted = PermissionHelper.hasAllMandatoryPermissions(context)

        _uiState.value = PermissionsUiState(
            permissions = items,
            canProceed = mandatoryGranted
        )
    }

    fun requestPermission(type: PermissionType) {
        PermissionHelper.requestPermission(getApplication(), type)
    }
}
