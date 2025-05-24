package com.example.familydocumentsapp.model

import android.os.Message

data class PersonalFolder(
    val id: Int,
    val name: String,
    val parent: Int?,
    val created_at: String
)

data class PersonalDocument(
    val id: Int,
    val name: String,
    val description: String?,
    val folder_id: Int?,
    val created_at: String?,
    val s3_key: String?
)

data class DocumentResponse(
    val id: Int,
    val name: String,
    val description: String?,
    val s3_key: String,
    val created_at: String,
    val folder: Int?
)

data class FamilyDocumentResponse(
    val id: Int,
    val name: String,
    val description: String?,
    val s3_key: String,
    val created_at: String,
    val folder: Int?,
    val family: Int
)

data class PersonalStorageRequest(
    val folder: String
)

data class DownloadUrlResponse(
    val url: String
)

data class ParentFolderDto(
    val id: Int,
    val name: String
)


data class PersonalStorageResponse(
    val current_folder: Int?,
    val folders: List<PersonalFolder>,
    val documents: List<PersonalDocument>
)


data class Family(
    val family_id: Int,
    val family_name: String,
    val role: String
)

data class UserFamiliesResponse(
    val families: List<Family>
)

data class CreateFolderRequest(
    val name: String,
    val parent_id: Int? = null
)

data class CreateFamilyFolderRequest(
    val name: String,
    val family_id: Int,
    val parent_id: Int? = null
)

data class PersonalFolderResponse(
    val id: String,
    val name: String,
    val created_at: String,
    val parent_id: String?
)

data class FamilyFolderResponse(
    val id: String,
    val name: String,
    val created_at: String,
    val parent_id: String?
)

data class ProfileResponse(
    val name: String,
    val email: String
)

data class FamilyFolderById(
    val id: Int,
    val family: Int
)

data class EmailChangeRequest(
    val password: String,
    val new_email: String
)

data class EmailChangeResponse(
    val message: String,
)

data class EmailChangeConfirmRequest(
    val code: String
)

data class PasswordChangeConfirmRequest(
    val code: String,
    val new_password: String
)

data class CreateFamilyRequest(
    val name: String,
    val password: String
)

data class CreateFamilyResponse(
    val message: String,
    val family_id: String
)

data class JoinFamilyRequest(
    val family_id: String,
    val password: String
)

data class JoinFamilyResponse(
    val message: String
)