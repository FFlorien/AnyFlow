package be.florien.anyflow.tags.local.di

import android.content.Context
import be.florien.anyflow.tags.local.LibraryDatabase
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class DataLocalModule {

    @Singleton
    @Provides
    fun provideLibrary(context: Context): LibraryDatabase = LibraryDatabase.getInstance(context)
}