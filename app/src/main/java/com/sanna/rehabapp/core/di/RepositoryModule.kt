package com.sanna.rehabapp.core.di

import com.sanna.rehabapp.data.repository.AuthRepositoryImpl
import com.sanna.rehabapp.data.repository.EjercicioRepositoryImpl
import com.sanna.rehabapp.data.repository.SesionRepositoryImpl
import com.sanna.rehabapp.data.repository.UsuarioRepositoryImpl
import com.sanna.rehabapp.domain.repository.AuthRepository
import com.sanna.rehabapp.domain.repository.EjercicioRepository
import com.sanna.rehabapp.domain.repository.SesionRepository
import com.sanna.rehabapp.domain.repository.UsuarioRepository
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
    abstract fun ligarAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun ligarUsuarioRepository(impl: UsuarioRepositoryImpl): UsuarioRepository

    @Binds
    @Singleton
    abstract fun ligarEjercicioRepository(impl: EjercicioRepositoryImpl): EjercicioRepository

    @Binds
    @Singleton
    abstract fun ligarSesionRepository(impl: SesionRepositoryImpl): SesionRepository
}
