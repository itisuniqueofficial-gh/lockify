package com.itisuniqueofficial.lockify.features.applist.ui

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.itisuniqueofficial.lockify.data.repository.AppLockRepository
import com.itisuniqueofficial.lockify.features.applist.domain.AppSearchManager
import com.itisuniqueofficial.lockify.features.applist.ui.AppIconCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(FlowPreview::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appSearchManager = AppSearchManager(application)
    private val appLockRepository = AppLockRepository(application)

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _allApps = MutableStateFlow<Set<ApplicationInfo>>(emptySet())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _lockedApps = MutableStateFlow<Set<String>>(emptySet())

    private val _debouncedQuery = MutableStateFlow("")

    val lockedAppsFlow: StateFlow<List<ApplicationInfo>> =
        combine(_allApps, _lockedApps, _debouncedQuery) { apps, locked, query ->
            apps.filter { it.packageName in locked }
                .filter { it.matchesQuery(query) }
                .sortedBy { it.packageName }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    val unlockedAppsFlow: StateFlow<List<ApplicationInfo>> =
        combine(_allApps, _lockedApps, _debouncedQuery) { apps, locked, query ->
            apps.filterNot { it.packageName in locked }
                .filter { it.matchesQuery(query) }
                .sortedBy { it.packageName }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    private fun ApplicationInfo.matchesQuery(query: String): Boolean {
        if (query.isBlank()) return true
        val label = AppIconCache.getLabel(getApplication(), this)
        return label.contains(query, ignoreCase = true) ||
                packageName.contains(query, ignoreCase = true)
    }

    init {
        loadAllApplications()
        loadLockedApps()

        viewModelScope.launch {
            _searchQuery
                .debounce(100L)
                .collect { query ->
                    _debouncedQuery.value = query
                }
        }
    }

    private fun loadAllApplications() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val apps = withContext(Dispatchers.IO) {
                    appSearchManager.loadApps(true)
                }
                _allApps.value = apps
            } catch (e: Exception) {
                e.printStackTrace()
                _allApps.value = emptySet()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadLockedApps() {
        _lockedApps.value = appLockRepository.getLockedApps()
    }

    fun lockApps(packageNames: List<String>) {
        appLockRepository.addMultipleLockedApps(packageNames.toSet())
        _lockedApps.value = appLockRepository.getLockedApps()
    }

    fun unlockApp(packageName: String) {
        appLockRepository.removeLockedApp(packageName)
        _lockedApps.value = appLockRepository.getLockedApps()
    }
}

