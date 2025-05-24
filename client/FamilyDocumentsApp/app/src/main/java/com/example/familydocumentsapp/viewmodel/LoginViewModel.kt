package com.example.familydocumentsapp.viewmodel


import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.familydocumentsapp.model.LoginRequest
import com.example.familydocumentsapp.model.PersonalStorageRequest
import com.example.familydocumentsapp.network.RetrofitInstance

class LoginViewModel : ViewModel() {

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    private val _loginSuccess = MutableStateFlow(false)
    val loginSuccess: StateFlow<Boolean> = _loginSuccess

    private val _emailNotConfirmed = MutableStateFlow<String?>(null)
    val emailNotConfirmed: StateFlow<String?> = _emailNotConfirmed


    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
    }

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage




    fun clearErrorMessage() {
        _errorMessage.value = ""
    }

    private fun saveToken(context: Context, token: String) {
        val sharedPref = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        sharedPref.edit().putString("TOKEN_KEY", token).apply()
    }

    fun clearEmailNotConfirmed() {
        _emailNotConfirmed.value = null
    }


    fun login(email: String, password: String, context: Context) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.getApi(context).login(LoginRequest(email, password))
                val body = response.body()
                if (response.isSuccessful && response.body() != null) {
                    val token = body!!.token
                    saveToken(context, token)
                    _loginSuccess.value = true
                } else {
                    val errorBody = response.errorBody()?.string() ?: ""
                    if (response.code() == 403 && errorBody.contains("Почта не подтверждена")) {
                        // Передаем email для перехода на экран подтверждения
                        _emailNotConfirmed.value = _email.value
                    } else {
                        _errorMessage.value = "Ошибка: ${response.message()}"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value  = "Ошибка подключения: ${e.message}"
            }
        }
    }
}