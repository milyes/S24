package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.Contact
import com.example.data.util.GoogleContactsCsvParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ContactRepository(private val db: AppDatabase) {
    private val contactDao = db.contactDao()

    val allContacts: Flow<List<Contact>> = contactDao.getAllContacts()

    suspend fun ensureDefaultContactSeeded() = withContext(Dispatchers.IO) {
        if (contactDao.getContactCount() == 0) {
            contactDao.insertContact(GoogleContactsCsvParser.NESRIN_OUIS_CONTACT)
        }
    }

    suspend fun addContact(contact: Contact): Long = withContext(Dispatchers.IO) {
        contactDao.insertContact(contact)
    }

    suspend fun updateContact(contact: Contact) = withContext(Dispatchers.IO) {
        contactDao.updateContact(contact)
    }

    suspend fun deleteContact(contact: Contact) = withContext(Dispatchers.IO) {
        contactDao.deleteContact(contact)
    }

    suspend fun toggleStarred(contact: Contact) = withContext(Dispatchers.IO) {
        contactDao.updateContact(contact.copy(isStarred = !contact.isStarred))
    }

    suspend fun importFromCsv(csvText: String): Int = withContext(Dispatchers.IO) {
        val parsed = GoogleContactsCsvParser.parseContactsCsv(csvText)
        if (parsed.isNotEmpty()) {
            contactDao.insertContacts(parsed)
        }
        parsed.size
    }

    suspend fun restoreDefaultContact(): Contact = withContext(Dispatchers.IO) {
        val defaultContact = GoogleContactsCsvParser.NESRIN_OUIS_CONTACT
        contactDao.insertContact(defaultContact)
        defaultContact
    }
}
