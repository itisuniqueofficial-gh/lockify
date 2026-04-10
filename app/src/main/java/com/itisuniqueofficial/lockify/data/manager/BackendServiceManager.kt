package com.itisuniqueofficial.lockify.data.manager

import com.itisuniqueofficial.lockify.data.repository.BackendImplementation
import com.itisuniqueofficial.lockify.services.AppLockAccessibilityService
import com.itisuniqueofficial.lockify.services.ExperimentalAppLockService

/**
 * Manages backend service selection and lifecycle.
 */
class BackendServiceManager {

    private var activeBackend: BackendImplementation? = null

    fun setActiveBackend(backend: BackendImplementation) {
        activeBackend = backend
    }

    fun shouldStartService(
        serviceClass: Class<*>,
        chosenBackend: BackendImplementation
    ): Boolean {
        val serviceBackend = getBackendForService(serviceClass) ?: return false
        if (serviceBackend == chosenBackend) return true
        if (activeBackend != null && serviceBackend == activeBackend) return true
        return false
    }

    private fun getBackendForService(serviceClass: Class<*>): BackendImplementation? {
        return when (serviceClass) {
            AppLockAccessibilityService::class.java -> BackendImplementation.ACCESSIBILITY
            ExperimentalAppLockService::class.java -> BackendImplementation.USAGE_STATS
            else -> null
        }
    }
}
