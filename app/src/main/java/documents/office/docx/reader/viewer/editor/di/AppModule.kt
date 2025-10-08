package documents.office.docx.reader.viewer.editor.di

import documents.office.docx.reader.viewer.editor.database.AppDatabase
import documents.office.docx.reader.viewer.editor.database.repository.FileModelRepository
import documents.office.docx.reader.viewer.editor.database.repository.FileModelRepositoryImpl
import documents.office.docx.reader.viewer.editor.screen.main.MainViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val appModule = module {
    single { AppDatabase.getInstance(androidApplication()) }
    single { FileModelRepositoryImpl(get()) as FileModelRepository}
    single { MainViewModel(androidApplication(), get()) }
}