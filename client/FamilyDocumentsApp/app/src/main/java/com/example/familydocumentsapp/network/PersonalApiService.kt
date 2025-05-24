package com.example.familydocumentsapp.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import com.example.familydocumentsapp.model.*
import retrofit2.http.GET


interface PersonalApiService {
    @POST("api/login/")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
    @POST("api/register/")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>
    @POST("api/confirm/")
    suspend fun confirmEmail(@Body request: RegisterConfirmRequest): Response<RegisterConfirmResponse>
    @POST("api/password_recovery/")
    suspend fun passwordRecovery(@Body request: PasswordRecoveryRequest): Response<PasswordRecoveryResponse>
    @POST("api/password_recovery_confirm/")
    suspend fun passwordRecoveryConfirm(@Body request: PasswordRecoveryConfirmRequest): Response<PasswordRecoveryConfirmResponse>
    @GET("api/profile/")
    suspend fun getProfile():Response<ProfileResponse>
    @POST("api/edit_name/")
    suspend fun editName(@Body request: EditName): Response<EditNameResponse>
    @POST("api/email_change/")
    suspend fun emailChange(@Body request: EmailChangeRequest): Response<EmailChangeResponse>
    @POST("api/email_change_confirm/")
    suspend fun emailChangeConfirm(@Body request: EmailChangeConfirmRequest): Response<EmailChangeResponse>
    @POST("api/password_change/")
    suspend fun passwordChange(): Response<EmailChangeResponse>
    @POST("api/password_change_confirm/")
    suspend fun passwordChangeConfirm(@Body request:PasswordChangeConfirmRequest): Response<EmailChangeResponse>

}