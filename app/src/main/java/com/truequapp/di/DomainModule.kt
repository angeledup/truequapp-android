package com.truequapp.di

import com.truequapp.core.domain.rules.CalculateBayesianRatingUseCase
import com.truequapp.core.domain.rules.EvaluateCancellationWindowUseCase
import com.truequapp.core.domain.rules.LockInventoryForTradeUseCase
import com.truequapp.core.domain.rules.ObfuscateLocationUseCase
import com.truequapp.core.domain.rules.ValidateDirectChatAccessUseCase
import com.truequapp.core.domain.rules.ValidateProhibitedItemUseCase
import com.truequapp.core.domain.rules.ValidateTradeValueUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de inyección de dependencias Hilt para los casos de uso canónicos de `:core:domain`.
 * Permite que los ViewModels de las capas de presentación consuman las reglas de negocio
 * puras sin acoplar `:core:domain` a dependencias de frameworks de Android o Dagger.
 */
@Module
@InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides
    @Singleton
    fun provideCalculateBayesianRatingUseCase(): CalculateBayesianRatingUseCase = CalculateBayesianRatingUseCase()

    @Provides
    @Singleton
    fun provideObfuscateLocationUseCase(): ObfuscateLocationUseCase = ObfuscateLocationUseCase()

    @Provides
    @Singleton
    fun provideValidateProhibitedItemUseCase(): ValidateProhibitedItemUseCase = ValidateProhibitedItemUseCase()

    @Provides
    @Singleton
    fun provideValidateTradeValueUseCase(): ValidateTradeValueUseCase = ValidateTradeValueUseCase()

    @Provides
    @Singleton
    fun provideLockInventoryForTradeUseCase(): LockInventoryForTradeUseCase = LockInventoryForTradeUseCase()

    @Provides
    @Singleton
    fun provideEvaluateCancellationWindowUseCase(): EvaluateCancellationWindowUseCase = EvaluateCancellationWindowUseCase()

    @Provides
    @Singleton
    fun provideValidateDirectChatAccessUseCase(): ValidateDirectChatAccessUseCase = ValidateDirectChatAccessUseCase()
}

