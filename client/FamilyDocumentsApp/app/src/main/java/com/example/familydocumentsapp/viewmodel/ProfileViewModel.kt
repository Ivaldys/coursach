package com.example.familydocumentsapp.viewmodel


import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.familydocumentsapp.model.CreateFamilyRequest
import com.example.familydocumentsapp.model.EditName
import com.example.familydocumentsapp.model.Family
import com.example.familydocumentsapp.model.JoinFamilyRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.familydocumentsapp.model.LoginRequest
import com.example.familydocumentsapp.model.PersonalStorageRequest
import com.example.familydocumentsapp.network.RetrofitInstance

class ProfileViewModel (private val context: Context) : ViewModel() {

    private val _families = MutableStateFlow<List<Family>>(emptyList())
    val families: StateFlow<List<Family>> = _families

    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess

    private val _emailNotConfirmed = MutableStateFlow<String?>(null)
    val emailNotConfirmed: StateFlow<String?> = _emailNotConfirmed

    private val _oldName = MutableStateFlow<String?>(null)
    val oldName: StateFlow<String?> = _oldName

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
    }

    fun onNameChange(newName: String) {
        _name.value = newName
    }

    fun onNameChangeClicked() {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.getApi(context).editName(EditName(_name.value))
                val response2 = RetrofitInstance.getApi(context).getProfile()
                if (response2.isSuccessful) {
                    _oldName.value = response2.body()?.name.toString()
                } else {
                    _error.value = response.errorBody()?.string() ?: "Ошибка загрузки семей"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка: ${e.message}"
            }
        }
    }

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage


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

    fun loadprofile() {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.getApi(context).getProfile()
                if (response.isSuccessful) {
                    _oldName.value = response.body()?.name.toString()
                    _email.value = response.body()?.email.toString()
                } else {
                    _error.value = response.errorBody()?.string() ?: "Ошибка загрузки семей"
                }
            } catch (e: Exception) {
                _error.value = "Ошибка: ${e.message}"
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = ""
    }

}