package be.florien.anyflow.feature.auth.domain.di

import android.content.Context
import android.content.SharedPreferences
import be.florien.anyflow.feature.auth.domain.persistence.AuthPersistence
import be.florien.anyflow.feature.auth.domain.persistence.AuthPersistenceKeystore
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

@Module
class NonAuthModule {
    @Singleton
    @Provides
    fun provideAuthPersistence(
        @Named("preferences") preferences: SharedPreferences,
        context: Context
    ): AuthPersistence =
        AuthPersistenceKeystore(preferences, context)
}