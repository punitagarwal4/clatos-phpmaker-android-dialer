package com.clatos.dialer.sync

import com.clatos.dialer.core.common.PhoneNumberUtils
import com.clatos.dialer.core.database.dao.ContactDao
import com.clatos.dialer.core.database.entity.ContactEntity
import com.clatos.dialer.core.database.entity.ContactSource
import com.clatos.dialer.core.network.CrmApi
import com.clatos.dialer.core.network.dto.ContactDto
import com.clatos.dialer.core.network.dto.CreateContactRequest
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides the unified contacts view. CRM contacts are cached in Room (synced
 * incrementally); device contacts are read live via ContactsContract (TODO)
 * and merged/deduped by normalized number. Deletion is intentionally unsupported.
 */
@Singleton
class ContactRepository @Inject constructor(
    private val crmApi: CrmApi,
    private val contactDao: ContactDao,
) {
    fun observeCachedCrmContacts(): Flow<List<ContactEntity>> = contactDao.observeAll()

    /** Pull CRM contacts (optionally incremental) into the local cache. */
    suspend fun syncCrmContacts(since: String? = null): Result<Int> = runCatching {
        val response = crmApi.contacts(since = since)
        val entities = response.data.map { it.toEntity() }
        contactDao.upsertAll(entities)
        entities.size
    }

    suspend fun profile(crmId: Long): Result<ContactDto> = runCatching { crmApi.contact(crmId) }

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
