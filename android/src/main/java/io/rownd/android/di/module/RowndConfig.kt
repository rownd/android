package io.rownd.android.di.module

import dagger.Module
import dagger.Provides
import io.rownd.android.models.RowndConfig
import javax.inject.Singleton

@Module
class RowndConfigProvider {
    @Singleton
    @Provides
    fun provideRowndConfig(): RowndConfig {
        return RowndConfig()
    }
}