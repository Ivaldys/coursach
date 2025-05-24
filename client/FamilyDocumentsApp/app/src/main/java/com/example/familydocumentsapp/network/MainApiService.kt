package com.example.familydocumentsapp.network
import com.example.familydocumentsapp.model.CreateFolderRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import com.example.familydocumentsapp.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.Path


interface MainApiService {
    @GET("api/personal_storage/")
    suspend fun getPersonalStorage(
        @Query("folder") folderId: Int? = null
    ): Response<PersonalStorageResponse>

    @GET("api/family_storage/")
    suspend fun getFamilyStorage(
        @Query("folder") folderId: Int? = null,
        @Query("family_id") familyId: Int
    ): Response<PersonalStorageResponse>

    @GET("api/user_families/")
    suspend fun getUserFamilies(
    ): Response<UserFamiliesResponse>

    @Multipart
    @POST("api/upload_personal_document/")
    suspend fun uploadDocument(
        @Part file: MultipartBody.Part,
        @Part("name") name: RequestBody,
        @Part("description") description: RequestBody,
        @Part("folder_id") folderId: RequestBody?
    ): Response<DocumentResponse>

    @Multipart
    @POST("api/upload_family_document/")
    suspend fun uploadFamilyDocument(
        @Part file: MultipartBody.Part,
        @Part("name") name: RequestBody,
        @Part("description") description: RequestBody,
        @Part("family_id") familyId: RequestBody,
        @Part("folder_id") folderId: RequestBody?
    ): Response<DocumentResponse>

    @POST("api/create_personal_folder/")
    suspend fun createFolder(@Body request: CreateFolderRequest): Response<PersonalFolderResponse>

    @POST("api/create_family_folder/")
    suspend fun createFamilyFolder(@Body request: CreateFamilyFolderRequest): Response<FamilyFolderResponse>

    @GET("api/folders/{id}/")
    suspend fun getFolderById(
        @Path("id") id: Int,
    ): PersonalFolder

    @GET("api/familyfolders/")
    suspend fun getFamilyFolderById(
        @Query("id") folderId: Int,
        @Query("family") familyId: Int
    )
    : PersonalFolder


    @GET("api/personal_presigned_url/")
    suspend fun getPresignedDownloadUrl(
        @Query("pk") documentId: Int
    ): Response<DownloadUrlResponse>

    @GET("api/family_presigned_url/")
    suspend fun getFamilyPresignedDownloadUrl(
        @Query("pk") documentId: Int
    ): Response<DownloadUrlResponse>

    @POST("api/create_family/")
    suspend fun createFamily(@Body request: CreateFamilyRequest): Response<CreateFamilyResponse>

    @POST("api/join_family/")
    suspend fun joinFamily(@Body request: JoinFamilyRequest): Response<JoinFamilyResponse>

}
