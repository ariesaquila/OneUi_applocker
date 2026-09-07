package com.oneui.applocker.ui.permissions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.oneui.applocker.core.permission.PermissionHelper
import com.oneui.applocker.core.permission.PermissionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import androidx.compose.runtime.Immutable

@Immutable
data class PermissionItemState(
    val type: PermissionType,
    val isGranted: Boolean
)

@Immutable
data class PermissionsUiState(
    val permissions: List<PermissionItemState> = emptyList(),
    val canProceed: Boolean = false,
    val isAccessibilityGranted: Boolean = false
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
        val accessibilityGranted = PermissionHelper.isAccessibilityServiceEnabled(context)

        _uiState.value = PermissionsUiState(
            permissions = items,
            canProceed = mandatoryGranted,
            isAccessibilityGranted = accessibilityGranted
        )
    }

    fun requestPermission(type: PermissionType) {
        PermissionHelper.requestPermission(getApplication(), type)
    }

    fun openAppDetailsSettings() {
        PermissionHelper.openAppDetailsSettings(getApplication())
    }
}
