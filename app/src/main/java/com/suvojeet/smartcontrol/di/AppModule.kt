package com.suvojeet.smartcontrol.di

import android.content.Context
import com.suvojeet.smartcontrol.SmartControlApplication
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApplication(@ApplicationContext app: Context): SmartControlApplication {
        return app as SmartControlApplication
    }
}
