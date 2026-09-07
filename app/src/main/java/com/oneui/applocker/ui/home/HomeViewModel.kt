package com.oneui.applocker.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.oneui.applocker.AppLockerApp
import com.oneui.applocker.core.permission.PermissionHelper
import com.oneui.applocker.data.model.AppItem
import com.oneui.applocker.service.AppMonitorForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class HomeUiState(
    val apps: List<AppItem> = emptyList(),
    val totalCount: Int = 0,
    val lockedCount: Int = 0,
    val unlockedCount: Int = 0,
    val downloadedCount: Int = 0,
    val systemCount: Int = 0,
    val isLoading: Boolean = true,
    val hasRequiredPermissions: Boolean = false
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepository = (application as AppLockerApp).appRepository

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(0) // 0: Hepsi, 1: İndirilenler, 2: Sistem, 3: Kilitli
    val selectedFilter: StateFlow<Int> = _selectedFilter.asStateFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        appRepository.installedAppsFlow,
        _searchQuery,
        _selectedFilter
    ) { allApps, query, filter ->
        val lockedCount = allApps.count { it.isLocked }
        val unlockedCount = allApps.count { !it.isLocked }
        val downloadedCount = allApps.count { !it.isSystemApp }
        val systemCount = allApps.count { it.isSystemApp }

        val filtered = allApps.filter { app ->
            val matchesQuery = query.isBlank() ||
                    app.appName.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                1 -> !app.isSystemApp // İndirilenler
                2 -> app.isSystemApp  // Sistem
                3 -> app.isLocked     // Kilitli
                else -> true          // Hepsi
            }

            matchesQuery && matchesFilter
        }

        HomeUiState(
            apps = filtered,
            totalCount = allApps.size,
            lockedCount = lockedCount,
            unlockedCount = unlockedCount,
            downloadedCount = downloadedCount,
            systemCount = systemCount,
            isLoading = false,
            hasRequiredPermissions = PermissionHelper.hasAllMandatoryPermissions(getApplication())
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onFilterSelect(index: Int) {
        _selectedFilter.value = index
    }

    fun toggleAppLock(app: AppItem) {
        viewModelScope.launch {
            appRepository.setAppLockStatus(app, !app.isLocked)
            // Ensure service is running if user locked an app
            if (!app.isLocked && PermissionHelper.hasAllMandatoryPermissions(getApplication())) {
                AppMonitorForegroundService.start(getApplication())
            }
        }
    }

    fun lockAll() {
        viewModelScope.launch {
            val currentApps = uiState.value.apps
            appRepository.lockAll(currentApps)
            if (PermissionHelper.hasAllMandatoryPermissions(getApplication())) {
                AppMonitorForegroundService.start(getApplication())
            }
        }
    }

    fun unlockAll() {
        viewModelScope.launch {
            appRepository.unlockAll()
        }
    }

    fun checkPermissions() {
        // Triggers recomposition of permission banner
    }
}
