package com.truequapp.di

import com.truequapp.core.domain.rules.*
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

    // Casos de Uso Fases 3, 4 y 5
    @Provides
    @Singleton
    fun provideVerifySafeZonePresenceUseCase(): VerifySafeZonePresenceUseCase = VerifySafeZonePresenceUseCase()

    @Provides
    @Singleton
    fun provideEvaluateReviewVisibilityUseCase(): EvaluateReviewVisibilityUseCase = EvaluateReviewVisibilityUseCase()

    @Provides
    @Singleton
    fun provideEscrowHoldAndReleaseUseCase(): EscrowHoldAndReleaseUseCase = EscrowHoldAndReleaseUseCase()

    @Provides
    @Singleton
    fun provideLedgerDoubleEntryValidatorUseCase(): LedgerDoubleEntryValidatorUseCase = LedgerDoubleEntryValidatorUseCase()

    @Provides
    @Singleton
    fun provideTriangularCycleRulesUseCase(): TriangularCycleRulesUseCase = TriangularCycleRulesUseCase()
}
