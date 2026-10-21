package com.cameronbatten.dadmusic.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Application-level Hilt module.
 * Database, DAO, and other singleton providers will be added in Task 2+.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule
