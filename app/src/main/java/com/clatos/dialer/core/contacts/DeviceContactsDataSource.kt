package com.clatos.dialer.core.contacts

import android.Manifest
import android.content.Context
import android.provider.ContactsContract
import com.clatos.dialer.core.common.PermissionUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class DeviceContact(
    val lookupKey: String,
    val name: String,
    val number: String,
)

/**
 * Reads contacts from the device address book via ContactsContract. Returns an
 * empty list when READ_CONTACTS isn't granted (handled gracefully upstream).
 */
@Singleton
class DeviceContactsDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun query(): List<DeviceContact> {
        if (!PermissionUtils.isGranted(context, Manifest.permission.READ_CONTACTS)) {
            return emptyList()
        }
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
        )
        val result = mutableListOf<DeviceContact>()
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC",
        )?.use { cursor ->
            val lookupIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
            val nameIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIdx = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                result += DeviceContact(
                    lookupKey = cursor.getString(lookupIdx).orEmpty(),
                    name = cursor.getString(nameIdx).orEmpty(),
                    number = cursor.getString(numberIdx).orEmpty(),
                )
            }
        }
        return result
    }
}
