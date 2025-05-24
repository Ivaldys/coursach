package com.example.familydocumentsapp.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.familydocumentsapp.model.EmailChangeConfirmRequest
import com.example.familydocumentsapp.model.EmailChangeRequest
import com.example.familydocumentsapp.model.LoginRequest
import com.example.familydocumentsapp.model.PasswordRecoveryConfirmRequest
import com.example.familydocumentsapp.model.PasswordRecoveryRequest
import com.example.familydocumentsapp.model.RegisterConfirmRequest
import com.example.familydocumentsapp.network.RetrofitInstance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

class ChangeEmailConfirmViewModel : ViewModel() {

    private val _code = MutableStateFlow("")
    val code: StateFlow<String> get() = _code

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> get() = _isLoading

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> get() = _errorMessage

    private val _confirmSuccess = MutableStateFlow(false)
    val confirmSuccess: StateFlow<Boolean> get() = _confirmSuccess

    fun onCodeChange(newCode: String) {
        _code.value = newCode
    }

    fun clearErrorMessage() {
        _errorMessage.value = ""
    }

    fun confirmCode(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = ""
            try {
                val response = RetrofitInstance.getApi(context).emailChangeConfirm (EmailChangeConfirmRequest(_code.value))
                if (response.isSuccessful) {
                    _confirmSuccess.value = true
                } else {
                    _errorMessage.value = response.errorBody()?.string() ?: "Ошибка сервера"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка подключения: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}