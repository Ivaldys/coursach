package com.example.familydocumentsapp.model

data class RegisterRequest(
    val email: String,
    val username: String,  // в API поле называется username
    val password: String
)

data class RegisterResponse(
    val message: String,
    val error: String,
)

data class RegisterConfirmRequest(
    val email: String,
    val code: String
)

data class RegisterConfirmResponse(
    val message: String,
    val error: String,
)