package documents.office.docx.reader.viewer.editor.database.repository

import androidx.lifecycle.LiveData
import documents.office.docx.reader.viewer.editor.common.SortState
import documents.office.docx.reader.viewer.editor.database.AppDatabase
import documents.office.docx.reader.viewer.editor.model.FileModel

class FileModelRepositoryImpl(
    private var appDatabase: AppDatabase
) : FileModelRepository {
    override suspend fun insert(fileModel: FileModel) {
        appDatabase.serverDao().insert(fileModel)
    }

    override suspend fun insert(fileModels: List<FileModel>) {
        appDatabase.serverDao().insert(fileModels)
    }

    override suspend fun delete(fileModel: FileModel) {
        appDatabase.serverDao().delete(fileModel)
    }
    override suspend fun delete(fileModels: List<FileModel>) {
        appDatabase.serverDao().delete(fileModels)
    }

//    override suspend fun setNotRecently(fileModels: List<FileModel>) {
//        val snapshot = fileModels.toList()              // copy the list first
//        snapshot.forEach { appDatabase.serverDao().setNotRecently(it.path) }
//    }

    override suspend fun setNotRecently(fileModels: List<FileModel>) {
        val paths = fileModels.map { it.path }
        appDatabase.serverDao().setNotRecently(paths)
    }

    override suspend fun deleteAll() {
        appDatabase.serverDao().deleteAll()
    }

    override suspend fun deleteAllRecent() {
        appDatabase.serverDao().deleteAllRecent()
    }

    override suspend fun deleteAllFavorite() {
        appDatabase.serverDao().deleteAllFavorite()
    }

    override fun getFileByPath(path: String): FileModel? {
        return appDatabase.serverDao().getFileByPath(path)
    }

    override fun getAllFiles(
        textSearch: String
    ): LiveData<List<FileModel>> {
        return appDatabase.serverDao().getLiveDataSearchFile(
            "%" + textSearch.trim() + "%"
        )
    }

    override fun getAllFiles(sortState: SortState): LiveData<List<FileModel>> {
        return appDatabase.serverDao().getLiveDataAllFile(sortState.value)
    }
    override fun getPdfFiles(sortState: SortState): LiveData<List<FileModel>> {
        return appDatabase.serverDao().getPdfFiles(sortState.value)
    }

    override fun getWordFiles(sortState: SortState): LiveData<List<FileModel>> {
        return appDatabase.serverDao().getWordFiles(sortState.value)
    }

    override fun getExcelFiles(sortState: SortState): LiveData<List<FileModel>> {
        return appDatabase.serverDao().getExcelFiles(sortState.value)
    }

    override fun getPptFiles(sortState: SortState): LiveData<List<FileModel>> {
        return appDatabase.serverDao().getPptFiles(sortState.value)
    }

    override suspend fun getAllFiles(): List<FileModel> {
        return appDatabase.serverDao().getAllFile()
    }

    override suspend fun getOldestUnreadFile(currentTime: Long, minDaysInMillis: Long): FileModel? {
        return appDatabase.serverDao().getOldestForgottenFile(currentTime, minDaysInMillis)
    }

    override suspend fun getRecentAllFiles(): LiveData<List<FileModel>> {
        return appDatabase.serverDao().getRecentFiles()
    }

    override suspend fun getRecentPdfFiles(): LiveData<List<FileModel>> {
        return appDatabase.serverDao().getRecentPdfFiles()
    }

    override suspend fun getRecentWordFiles(): LiveData<List<FileModel>> {
        return appDatabase.serverDao().getRecentWordFiles()
    }

    override suspend fun getRecentPptFiles(): LiveData<List<FileModel>> {
        return appDatabase.serverDao().getRecentPptFiles()
    }

    override suspend fun getRecentExcelFiles(): LiveData<List<FileModel>> {
        return appDatabase.serverDao().getRecentExcelFiles()
    }

    override suspend fun getFavoriteAllFiles(): LiveData<List<FileModel>> {
        return appDatabase.serverDao().getFavoriteFiles()
    }

    override suspend fun getLatestFiles(): List<FileModel> {
        return appDatabase.serverDao().getLatestFiles()
    }

    override suspend fun getFavoritePdfFiles(): LiveData<List<FileModel>> {
        return appDatabase.serverDao().getFavoritePdfFiles()
    }

    override suspend fun getFavoriteWordFiles(): LiveData<List<FileModel>> {
        return appDatabase.serverDao().getFavoriteWordFiles()
    }

    override fun getNumberOfTodayAddedFile(text: String): LiveData<Int> {
        return appDatabase.serverDao().getNumberOfTodayAddedFile(text)
    }

    override suspend fun getFavoritePptFiles(): LiveData<List<FileModel>> {
        return appDatabase.serverDao().getFavoritePptFiles()
    }

    override suspend fun getFavoriteExcelFiles(): LiveData<List<FileModel>> {
        return appDatabase.serverDao().getFavoriteExcelFiles()
    }

    override fun mergeFileModel(fileInDevice: List<FileModel>, fileModels: List<FileModel>) {
        fileInDevice.map { fileModel ->
            fileModels.find { it.path == fileModel.path }?.let {
                fileModel.name = it.name
                fileModel.isRecent = it.isRecent
                fileModel.timeRecent = it.timeRecent
                fileModel.unixTimeRecent = it.unixTimeRecent
                fileModel.isFavorite = it.isFavorite
                fileModel.timeAdd = it.timeAdd
                fileModel.isReadDone = it.isReadDone
                fileModel.isNoticed = it.isNoticed
                fileModel.isSample = it.isSample
            }
            return@map fileModel
        }
    }
}