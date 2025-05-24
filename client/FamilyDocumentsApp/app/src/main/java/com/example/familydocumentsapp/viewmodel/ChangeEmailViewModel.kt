package com.example.familydocumentsapp.viewmodel


import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.familydocumentsapp.model.EmailChangeRequest
import com.example.familydocumentsapp.model.LoginRequest
import com.example.familydocumentsapp.model.PasswordRecoveryRequest
import com.example.familydocumentsapp.network.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class ChangeEmailViewModel : ViewModel() {
    private val _newemail = MutableStateFlow("")
    val newemail: StateFlow<String> = _newemail

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    private val _secondpassword = MutableStateFlow("")
    val secondpassword: StateFlow<String> = _secondpassword

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message

    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error

    private val _changeSuccess = MutableStateFlow(false)
    val changeSuccess: StateFlow<Boolean> = _changeSuccess

    fun onEmailChange(newEmail: String) {
        _newemail.value = newEmail
    }

    fun onPasswordChange(password: String) {
        _password.value = password
    }

    fun onSecondPasswordChange(secondpassword: String) {
        _secondpassword.value = secondpassword
    }

    fun clearMessages() {
        _message.value = ""
        _error.value = ""
    }

    fun sendResetPasswordRequest(context: Context) {
        viewModelScope.launch {
                try {
                    val response = RetrofitInstance.getApi(context)
                        .emailChange(EmailChangeRequest(_password.value, _newemail.value))
                    if (response.isSuccessful) {
                        _changeSuccess.value = true
                        _message.value = "Код для восстановления отправлен на почту"
                    } else {
                        _error.value = "Ошибка: ${response.message()}"
                    }
                } catch (e: Exception) {
                    _error.value = "Ошибка сети: ${e.message}"
                }
        }
    }
}