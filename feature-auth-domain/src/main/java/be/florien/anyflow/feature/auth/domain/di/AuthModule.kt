package be.florien.anyflow.feature.auth.domain.di

import androidx.lifecycle.LiveData
import be.florien.anyflow.feature.auth.domain.repository.AuthRepository
import dagger.Module
import dagger.Provides

@Module
class AuthModule {
    @Provides
    fun provideConnectionStatus(connection: AuthRepository): LiveData<AuthRepository.ConnectionStatus> =
        connection.connectionStatusUpdater
}