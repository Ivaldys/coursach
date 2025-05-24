package com.example.familydocumentsapp.viewmodel


import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.familydocumentsapp.model.LoginRequest
import com.example.familydocumentsapp.model.PasswordRecoveryRequest
import com.example.familydocumentsapp.network.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class ForgotPasswordViewModel : ViewModel() {
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message

    private val _error = MutableStateFlow("")
    val error: StateFlow<String> = _error

    private val _recoverySuccess = MutableStateFlow(false)
    val recoverySuccess: StateFlow<Boolean> = _recoverySuccess

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
    }

    fun clearMessages() {
        _message.value = ""
        _error.value = ""
    }

    fun sendResetPasswordRequest(context: Context) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.getApi(context).passwordRecovery(PasswordRecoveryRequest(_email.value))
                if (response.isSuccessful) {
                    _recoverySuccess.value  = true
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