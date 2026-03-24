package com.sharefast.domain.repository

import com.sharefast.domain.model.InstalledApp

interface AppsRepository {
    suspend fun loadInstalledApps(): List<InstalledApp>
}
