package com.example.familydocumentsapp.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.familydocumentsapp.model.CreateFamilyRequest
import com.example.familydocumentsapp.model.CreateFolderRequest
import com.example.familydocumentsapp.model.PersonalFolder
import com.example.familydocumentsapp.model.PersonalDocument
import com.example.familydocumentsapp.model.Family
import com.example.familydocumentsapp.model.JoinFamilyRequest
import com.example.familydocumentsapp.network.RetrofitInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import java.io.File
import java.net.URL
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class HomeViewModel(private val context: Context) : ViewModel() {

    private val _folders = MutableStateFlow<List<PersonalFolder>>(emptyList())
    val folders: StateFlow<List<PersonalFolder>> = _folders

    private val _documents = MutableStateFlow<List<PersonalDocument>>(emptyList())
    val documents: StateFlow<List<PersonalDocument>> = _documents

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error

    private val _families = MutableStateFlow<List<Family>>(emptyList())
    val families: StateFlow<List<Family>> = _families

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    private val _currentFolderId = MutableStateFlow<Int?>(null)
    val currentFolderId: StateFlow<Int?> = _currentFolderId

    private val _currentFolder = MutableStateFlow<PersonalFolder?>(null)
    val currentFolder: StateFlow<PersonalFolder?> = _currentFolder

    val isOnlineMode = MutableStateFlow(true)

    fun setOnlineMode(value: Boolean) {
        isOnlineMode.value = value
    }



    fun logout(context: Context) {
        val sharedPref = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        sharedPref.edit().remove("TOKEN_KEY").apply()
    }

    fun loadPersonalStorage(folderId: Int? = null, onlinemod: Boolean? =null) {

        if (onlinemod == false) {
            _documents.value = getDownloadedDocumentsAsObjects(context)
            _folders.value =  emptyList()
        }
        else {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                _currentFolderId.value = folderId
                if (folderId == null){
                    _currentFolder.value = null
                }
                val response = RetrofitInstance.getDataApi(context).getPersonalStorage(folderId)
                if (response.isSuccessful) {
                    val data = response.body()
                    if (_currentFolderId.value != null){
                        val response2 = RetrofitInstance.getDataApi(context).getFolderById(folderId!!)
                        _currentFolder.value = response2
                    }

                    _folders.value = data?.folders ?: emptyList()
                    _documents.value = data?.documents ?: emptyList()
                    _error.value = ""

                } else {
                    _error.value = response.errorBody()?.string() ?: "Ошибка загрузки"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка подключения: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    fun String.toDateTime(): LocalDateTime {
        return OffsetDateTime.parse(this, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDateTime()
    }


    fun loadUserFamilies() {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.getDataApi(context).getUserFamilies()
                if (response.isSuccessful) {
                    _families.value = response.body()?.families ?: emptyList()
                } else {
                    _error.value = response.errorBody()?.string() ?: "Ошибка загрузки семей"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка: ${e.message}"
            }
        }
    }
    fun uriToFile(context: Context, uri: Uri): File? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("scanned_doc", ".pdf", context.cacheDir)
        tempFile.outputStream().use { fileOut ->
            inputStream.copyTo(fileOut)
        }
        return tempFile
    }

    suspend fun uploadScannedDocument(
        context: Context,
        uri: Uri,
        name: String,
        description: String,
        folderId: Int? = null,
    ) {
        val file = uriToFile(context, uri) ?: return
        var stringedfolderid = ""
        if (folderId !=null){
            stringedfolderid = folderId.toString()
        }
        else {
            stringedfolderid = ""
        }
        val requestFile = file.asRequestBody("application/pdf".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        val namePart = name.toRequestBody("text/plain".toMediaTypeOrNull())
        val descPart = description.toRequestBody("text/plain".toMediaTypeOrNull())
        val folderPart = stringedfolderid?.toRequestBody("text/plain".toMediaTypeOrNull())
        val response = RetrofitInstance.getDataApi(context).uploadDocument(
            file = body,
            name = namePart,
            description = descPart,
            folderId = folderPart
        )

        if (response.isSuccessful) {
            Log.d("Upload", "Файл успешно загружен: ${response.body()}")
        } else {
            Log.e("Upload", "Ошибка загрузки: ${response.errorBody()?.string()}")
        }
    }

    private val TAG = "DownloadDoc"

    fun downloadDocument(context: Context, doc: PersonalDocument) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Запрос presigned URL для документа id=${doc.id}, name='${doc.name}'")

                // Выполняем сетевой запрос в IO потоке
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.getDataApi(context).getPresignedDownloadUrl(doc.id)
                }

                if (!response.isSuccessful) {
                    Log.e(TAG, "Ошибка получения presigned URL: ${response.code()} ${response.message()}")
                    return@launch
                }
                val url = response.body()?.url
                Log.d(TAG, "Получен presigned URL: $url")

                if (url.isNullOrEmpty()) {
                    Log.e(TAG, "URL пустой, скачивание отменено")
                    return@launch
                }

                // Открытие соединения и чтение потока тоже в IO потоке
                withContext(Dispatchers.IO) {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    Log.d(TAG, "Открыто соединение по URL")

                    val inputStream = connection.inputStream

                    val safeFileName = if (doc.name.endsWith(".pdf")) doc.name else "${doc.name}.pdf"
                    val file = File(context.filesDir, safeFileName)
                    Log.d(TAG, "Файл будет сохранён в: ${file.absolutePath}")

                    val outputStream = FileOutputStream(file)

                    inputStream.copyTo(outputStream)

                    outputStream.close()
                    inputStream.close()

                    Log.d(TAG, "Файл '${safeFileName}' успешно сохранён")
                }

                saveDownloadedDocumentInfo(context, doc, if (doc.name.endsWith(".pdf")) doc.name else "${doc.name}.pdf")

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при скачивании файла: ${e.localizedMessage}", e)
            }
        }
    }

    fun getDownloadedDocumentsAsObjects(context: Context): List<PersonalDocument> {
        val prefs = context.getSharedPreferences("offline_docs", Context.MODE_PRIVATE)
        val list = prefs.all.mapNotNull { entry ->
            val id = entry.key.toIntOrNull()
            val fileName = entry.value as? String
            if (id != null && fileName != null) {
                val file = File(context.filesDir, fileName)
                if (file.exists()) {
                    Log.d(TAG, "Найден скачанный файл: id=$id, file=${file.absolutePath}")
                    PersonalDocument(
                        id = id,
                        name = fileName,
                        description = null,
                        folder_id = null,
                        created_at = null,
                        s3_key = null
                    )
                } else {
                    Log.d(TAG, "Файл не найден на диске: $fileName")
                    null
                }
            } else {
                Log.d(TAG, "Неверная запись в SharedPreferences: ключ=${entry.key}, значение=${entry.value}")
                null
            }
        }
        Log.d(TAG, "Всего скачанных документов: ${list.size}")
        return list
    }

    fun saveDownloadedDocumentInfo(context: Context, doc: PersonalDocument, safeFileName: String) {
        val prefs = context.getSharedPreferences("offline_docs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString(doc.id.toString(), safeFileName)
            .apply()
        Log.d(TAG, "Сохранена информация о скачанном документе id=${doc.id}, file=$safeFileName в SharedPreferences")
    }

    private val _sortType = MutableStateFlow(SortType.BY_NAME)
    val sortType: StateFlow<SortType> = _sortType

    enum class SortType {
        BY_NAME,
        BY_DATE_ASC,
        BY_DATE_DESC
    }
    fun setSortType(type: SortType) {
        _sortType.value = type
        // сортируем при изменении
    }


    fun createPersonalFolder(name: String, parentId: Int? = null) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.getDataApi(context).createFolder(CreateFolderRequest(name,parentId))
                if (response.isSuccessful) {
                    loadPersonalStorage()
                    Log.d("Folder", "Папка создана: ${response.body()}")
                } else {
                    Log.e("Folder", "Ошибка: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("Folder", "Ошибка при создании папки", e)
            }
        }
    }

    fun createFamily(name: String, password: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.getDataApi(context).createFamily(CreateFamilyRequest(name,password))
                if (response.isSuccessful) {
                    Log.d("Folder", "Семья создана: ${response.body()}")
                } else {
                    Log.e("Folder", "Ошибка: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("Folder", "Ошибка при создании семьи", e)
            }
        }
    }

    fun joinFamily(id: String, password: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.getDataApi(context).joinFamily(JoinFamilyRequest(id,password))
                if (response.isSuccessful) {
                    Log.d("Folder", "Вы успешно подали заявку: ${response.body()}")
                } else {
                    Log.e("Folder", "Ошибка: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("Folder", "Ошибка при подаче заявки", e)
            }
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    val filteredFolders: StateFlow<List<PersonalFolder>> =
        combine(_folders, _searchQuery, _sortType) { folders, query, sortType ->
            val filtered = if (query.isBlank()) {
                folders
            } else {
                folders.filter { it.name.contains(query, ignoreCase = true) }
            }
            when (sortType) {
                SortType.BY_NAME -> filtered.sortedBy { it.name.lowercase() }
                SortType.BY_DATE_ASC -> filtered.sortedBy { it.created_at.toDateTime()}
                SortType.BY_DATE_DESC -> filtered.sortedByDescending { it.created_at.toDateTime() }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    @RequiresApi(Build.VERSION_CODES.O)
    val filteredDocuments: StateFlow<List<PersonalDocument>> =
        combine(_documents, _searchQuery, _sortType) { docs, query, sortType ->
            val filtered = if (query.isBlank()) {
                docs
            } else {
                docs.filter { it.name.contains(query, ignoreCase = true) }
            }

            when (sortType) {
                SortType.BY_NAME -> filtered.sortedBy { it.name.lowercase() }
                SortType.BY_DATE_ASC -> filtered.sortedBy { it.created_at?.toDateTime() }
                SortType.BY_DATE_DESC -> filtered.sortedByDescending { it.created_at?.toDateTime() }
            }
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}