package com.example.smartcampus.di

import com.example.smartcampus.data.repository.AuthRepositoryImpl
import com.example.smartcampus.data.repository.ChatRepositoryImpl
import com.example.smartcampus.data.repository.NoticeRepositoryImpl
import com.example.smartcampus.domain.repository.AuthRepository
import com.example.smartcampus.domain.repository.ChatRepository
import com.example.smartcampus.domain.repository.NoticeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindNoticeRepository(
        noticeRepositoryImpl: NoticeRepositoryImpl
    ): NoticeRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository
}