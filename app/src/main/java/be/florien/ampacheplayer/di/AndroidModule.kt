package be.florien.ampacheplayer.di

import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides

/**
 * Created by florien on 31/03/17.
 */
@Module
class AndroidModule (var context: Context){
    private val PREFERENCE_NAME = "ampache_preferences"

    @Provides
    fun provideContext() : Context = context

    @Provides
    fun providePreferences() : SharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
}