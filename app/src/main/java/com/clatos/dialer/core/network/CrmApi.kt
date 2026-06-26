package com.clatos.dialer.core.network

import com.clatos.dialer.core.network.dto.CallLogUploadResponse
import com.clatos.dialer.core.network.dto.ContactDto
import com.clatos.dialer.core.network.dto.ContactListResponse
import com.clatos.dialer.core.network.dto.CreateContactRequest
import com.clatos.dialer.core.network.dto.DeviceReportRequest
import com.clatos.dialer.core.network.dto.LoginRequest
import com.clatos.dialer.core.network.dto.LoginResponse
import com.clatos.dialer.core.network.dto.UserDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the PHPMaker CRM. Paths mirror docs/API_CONTRACT.md.
 * Confirm against the real API and adjust as needed.
 */
interface CrmApi {

    @POST("api/login")
    suspend fun login(@Body body: LoginRequest): LoginResponse

    @GET("api/me")
    suspend fun me(): UserDto

    @GET("api/contacts")
    suspend fun contacts(
        @Query("since") since: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 100,
    ): ContactListResponse

    @GET("api/contacts/{id}")
    suspend fun contact(@Path("id") id: Long): ContactDto

    @POST("api/contacts")
    suspend fun createContact(@Body body: CreateContactRequest): ContactDto

    /**
     * Upload one call log + optional recording as multipart.
     * [metadata] is a JSON part; [recording] is the .m4a file part (nullable).
     */
    @Multipart
    @POST("api/calllogs")
    suspend fun uploadCallLog(
        @Part("metadata") metadata: RequestBody,
        @Part recording: MultipartBody.Part?,
    ): CallLogUploadResponse

    @POST("api/devices/report")
    suspend fun reportDevice(@Body body: DeviceReportRequest)
}
