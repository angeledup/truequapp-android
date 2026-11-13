package com.truequapp.feature.reports.di

import com.truequapp.feature.reports.pdf.AndroidPdfReportGenerator
import com.truequapp.feature.reports.pdf.PdfReportGenerator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ReportsModule {

    @Binds
    @Singleton
    abstract fun bindPdfReportGenerator(
        impl: AndroidPdfReportGenerator
    ): PdfReportGenerator
}

