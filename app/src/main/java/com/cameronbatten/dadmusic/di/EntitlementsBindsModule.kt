package com.cameronbatten.dadmusic.di

import com.cameronbatten.dadmusic.entitlements.EntitlementSource
import com.cameronbatten.dadmusic.entitlements.SideloadEntitlementSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class EntitlementsBindsModule {

    @Binds
    @Singleton
    abstract fun bindEntitlementSource(impl: SideloadEntitlementSource): EntitlementSource
}
