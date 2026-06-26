package com.clatos.dialer.sync

import com.clatos.dialer.core.common.PhoneNumberUtils
import com.clatos.dialer.core.contacts.DeviceContactsDataSource
import com.clatos.dialer.core.database.dao.CallLogDao
import com.clatos.dialer.core.database.dao.ContactDao
import com.clatos.dialer.core.database.entity.CallLogEntity
import com.clatos.dialer.core.database.entity.ContactEntity
import com.clatos.dialer.core.database.entity.ContactSource
import com.clatos.dialer.core.datastore.SessionStore
import com.clatos.dialer.core.network.CrmApi
import com.clatos.dialer.core.network.dto.ContactDto
import com.clatos.dialer.core.network.dto.CreateContactRequest
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** A contact shown in the unified list (device + CRM, deduped by number). */
data class UnifiedContact(
    val id: String,            // "crm:{id}" or "device:{lookupKey}"
    val name: String,
    val number: String?,
    val source: ContactSource,
    val crmId: Long?,
    val alsoInOtherSource: Boolean = false,
)

/**
 * Unified contacts: CRM contacts are cached in Room (synced from the API) and
 * merged with device contacts (ContactsContract), deduped by normalized number.
 * Deletion is intentionally unsupported.
 */
@Singleton
class ContactRepository @Inject constructor(
    private val crmApi: CrmApi,
    private val contactDao: ContactDao,
    private val callLogDao: CallLogDao,
    private val deviceContacts: DeviceContactsDataSource,
    private val sessionStore: SessionStore,
) {
    /**
     * Pull CRM contacts into the local cache. Incremental by default: sends the
     * stored last-sync timestamp as `since` and records a new one on success.
     * Pass [since] = "" to force a full refresh.
     */
    suspend fun syncCrmContacts(since: String? = null): Result<Int> = runCatching {
        val effectiveSince = when (since) {
            null -> sessionStore.lastContactSync()  // incremental
            "" -> null                               // forced full refresh
            else -> since
        }
        val startedAt = Instant.now().toString()
        val response = crmApi.contacts(since = effectiveSince)
        val entities = response.data.map { it.toEntity() }
        contactDao.upsertAll(entities)
        sessionStore.setLastContactSync(startedAt)
        entities.size
    }

    /** Merged device + CRM contacts, deduped by number, filtered by [query]. */
    suspend fun loadUnified(query: String = ""): List<UnifiedContact> {
        val crm = contactDao.getAll()
        val device = deviceContacts.query()

        val byNumber = LinkedHashMap<String, UnifiedContact>()
        // CRM takes precedence so opening a match shows the CRM profile.
        crm.forEach { entity ->
            val key = entity.normalizedNumber?.takeIf { it.isNotBlank() } ?: "crm:${entity.crmId}"
            byNumber[key] = UnifiedContact(
                id = entity.id,
                name = entity.name,
                number = entity.primaryNumber,
                source = ContactSource.CRM,
                crmId = entity.crmId,
            )
        }
        device.forEach { dc ->
            val key = PhoneNumberUtils.normalize(dc.number).takeIf { it.isNotBlank() }
                ?: "device:${dc.lookupKey}"
            val existing = byNumber[key]
            if (existing == null) {
                byNumber[key] = UnifiedContact(
                    id = "device:${dc.lookupKey}",
                    name = dc.name,
                    number = dc.number,
                    source = ContactSource.DEVICE,
                    crmId = null,
                )
            } else if (existing.source == ContactSource.CRM) {
                byNumber[key] = existing.copy(alsoInOtherSource = true)
            }
        }

        val q = query.trim()
        return byNumber.values
            .filter {
                q.isBlank() ||
                    it.name.contains(q, ignoreCase = true) ||
                    (it.number?.contains(q) == true)
            }
            .sortedBy { it.name.lowercase() }
    }

    suspend fun profile(crmId: Long): Result<ContactDto> = runCatching { crmApi.contact(crmId) }

    /** Recent local call history with a given number (for the profile screen). */
    suspend fun recentCalls(number: String?): List<CallLogEntity> {
        val normalized = PhoneNumberUtils.normalize(number)
        if (normalized.isBlank()) return emptyList()
        return callLogDao.recentForNumber(normalized)
    }

    suspend fun create(name: String, phone: String, company: String?, notes: String?): Result<ContactDto> =
        runCatching {
            val created = crmApi.createContact(CreateContactRequest(name, phone, company, notes))
            contactDao.upsertAll(listOf(created.toEntity()))
            created
        }

    private fun ContactDto.toEntity() = ContactEntity(
        id = "crm:$id",
        source = ContactSource.CRM,
        crmId = id,
        name = name,
        primaryNumber = phone ?: phones.firstOrNull(),
        normalizedNumber = PhoneNumberUtils.normalize(phone ?: phones.firstOrNull()),
        company = company,
    )
}
