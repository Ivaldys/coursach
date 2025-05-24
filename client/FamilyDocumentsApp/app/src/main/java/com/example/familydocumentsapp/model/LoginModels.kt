package com.example.familydocumentsapp.model

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val message: String,
    val error: String,
)

data class PasswordRecoveryRequest(
    val email: String
)

data class PasswordRecoveryResponse(
    val message: String,
    val error: String,
)

data class PasswordRecoveryConfirmRequest(
    val email: String,
    val code: String,
    val new_password: String,
    val confirm_password: String
)

data class PasswordRecoveryConfirmResponse(
    val message: String,
    val error: String,
)

data class EditName(
    val name: String
)

data class EditNameResponse(
    val message: String
)