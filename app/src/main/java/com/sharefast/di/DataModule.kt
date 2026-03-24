package com.sharefast.di

import com.sharefast.data.repository.AppsRepositoryImpl
import com.sharefast.data.repository.DeviceRepositoryImpl
import com.sharefast.data.repository.DiscoveryRepositoryImpl
import com.sharefast.data.repository.DocumentsRepositoryImpl
import com.sharefast.data.repository.MediaRepositoryImpl
import com.sharefast.data.repository.TransferHistoryRepositoryImpl
import com.sharefast.domain.repository.AppsRepository
import com.sharefast.domain.repository.DeviceRepository
import com.sharefast.domain.repository.DiscoveryRepository
import com.sharefast.domain.repository.DocumentsRepository
import com.sharefast.domain.repository.MediaRepository
import com.sharefast.domain.repository.TransferHistoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun deviceRepository(impl: DeviceRepositoryImpl): DeviceRepository

    @Binds
    @Singleton
    abstract fun discoveryRepository(impl: DiscoveryRepositoryImpl): DiscoveryRepository

    @Binds
    @Singleton
    abstract fun transferHistoryRepository(impl: TransferHistoryRepositoryImpl): TransferHistoryRepository

    @Binds
    @Singleton
    abstract fun mediaRepository(impl: MediaRepositoryImpl): MediaRepository

    @Binds
    @Singleton
    abstract fun appsRepository(impl: AppsRepositoryImpl): AppsRepository

    @Binds
    @Singleton
    abstract fun documentsRepository(impl: DocumentsRepositoryImpl): DocumentsRepository
}
