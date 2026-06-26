package com.clatos.dialer.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire models for the PHPMaker CRM API. Field names mirror docs/API_CONTRACT.md and
 * MUST be confirmed against the real API; adjust @SerialName values as needed.
 */

@Serializable
data class LoginRequest(
    val username: String,
    val password: String,
)

@Serializable
data class LoginResponse(
    val token: String,
    @SerialName("expires_in") val expiresIn: Long? = null,
    val user: UserDto,
)

@Serializable
data class UserDto(
    val id: Long,
    val name: String,
    val username: String,
)

@Serializable
data class ContactDto(
    val id: Long,
    val name: String,
    val phone: String? = null,
    val phones: List<String> = emptyList(),
    val company: String? = null,
    val email: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class ContactListResponse(
    val data: List<ContactDto> = emptyList(),
    val page: Int = 1,
    @SerialName("per_page") val perPage: Int = 100,
    val total: Int = 0,
)

@Serializable
data class CreateContactRequest(
    val name: String,
    val phone: String,
    val company: String? = null,
    val notes: String? = null,
)

@Serializable
data class CallLogMetadata(
    @SerialName("agent_id") val agentId: Long,
    val direction: String,            // INCOMING | OUTGOING | MISSED
    val number: String,
    @SerialName("contact_id") val contactId: Long? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("started_at") val startedAt: String,
    @SerialName("duration_sec") val durationSec: Int,
    val status: String,
    @SerialName("recording_state") val recordingState: String,   // OK | FAILED | NONE
    @SerialName("recording_strategy") val recordingStrategy: String,
    @SerialName("client_call_id") val clientCallId: String,       // idempotency key
)

@Serializable
data class CallLogUploadResponse(
    val id: Long,
    @SerialName("client_call_id") val clientCallId: String,
    val status: String,
)

@Serializable
data class DeviceReportRequest(
    @SerialName("agent_id") val agentId: Long,
    @SerialName("device_model") val deviceModel: String,
    @SerialName("os_version") val osVersion: String,
    @SerialName("selected_strategy") val selectedStrategy: String,
    @SerialName("recording_degraded") val recordingDegraded: Boolean,
    @SerialName("app_version") val appVersion: String,
)
