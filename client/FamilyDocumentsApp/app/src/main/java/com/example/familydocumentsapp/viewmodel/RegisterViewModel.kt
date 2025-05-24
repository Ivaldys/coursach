package com.example.familydocumentsapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.familydocumentsapp.network.RetrofitInstance
import com.example.familydocumentsapp.model.RegisterRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage

    private val _registerSuccess = MutableStateFlow(false)
    val registerSuccess: StateFlow<Boolean> = _registerSuccess

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
    }

    fun onNameChange(newName: String) {
        _name.value = newName
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
    }

    fun clearErrorMessage() {
        _errorMessage.value = ""
    }

    fun register(email: String, name: String, password: String, context: Context) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.getApi(context).register(RegisterRequest(email, name, password))
                if (response.isSuccessful) {
                    _registerSuccess.value = true
                    _errorMessage.value = ""
                } else {
                    _errorMessage.value = response.errorBody()?.string() ?: "Неизвестная ошибка"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка подключения: ${e.message}"
            }
        }
    }
}