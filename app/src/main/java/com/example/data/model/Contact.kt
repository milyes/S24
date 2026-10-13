package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing a contact/recipient imported from Google Contacts CSV
 * or entered manually, with direct telephony, messaging, and cloud sync capabilities.
 */
@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val firstName: String = "",
    val middleName: String = "",
    val lastName: String = "",
    val organization: String = "",
    val phoneNumber: String = "",
    val phoneLabel: String = "Mobile",
    val email: String = "",
    val addressFormatted: String = "",
    val street: String = "",
    val city: String = "",
    val region: String = "",
    val postalCode: String = "",
    val country: String = "",
    val extendedAddress: String = "",
    val notes: String = "",
    val profileUrl: String = "",
    val labels: String = "* myContacts",
    val isStarred: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    val displayName: String
        get() = when {
            firstName.isNotBlank() && lastName.isNotBlank() -> "$firstName $lastName"
            firstName.isNotBlank() -> firstName
            lastName.isNotBlank() -> lastName
            organization.isNotBlank() -> organization
            else -> phoneNumber.ifBlank { "Contact sans nom" }
        }

    val initials: String
        get() {
            val parts = displayName.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
            return when {
                parts.size >= 2 -> "${parts[0].firstOrNull()?.uppercaseChar() ?: ""}${parts[1].firstOrNull()?.uppercaseChar() ?: ""}"
                parts.isNotEmpty() && parts[0].isNotEmpty() -> parts[0].take(2).uppercase()
                else -> "CT"
            }
        }
}
