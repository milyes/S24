package com.example.data.util

import com.example.data.model.Contact

object GoogleContactsCsvParser {

    /**
     * Parses standard Google Contacts CSV format text and returns a list of [Contact] objects.
     */
    fun parseContactsCsv(csvContent: String): List<Contact> {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val headerLine = lines.first()
        val headers = parseCsvLine(headerLine).map { it.trim() }

        val contacts = mutableListOf<Contact>()
        for (i in 1 until lines.size) {
            val rowTokens = parseCsvLine(lines[i])
            if (rowTokens.all { it.isBlank() }) continue

            val map = mutableMapOf<String, String>()
            headers.forEachIndexed { index, header ->
                map[header] = rowTokens.getOrElse(index) { "" }.trim()
            }

            val contact = mapToContact(map)
            if (contact != null) {
                contacts.add(contact)
            }
        }

        return contacts
    }

    /**
     * Splits a CSV line taking quotation marks into account.
     */
    fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++ // skip escaped quote
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.setLength(0)
                }
                else -> {
                    current.append(c)
                }
            }
            i++
        }
        result.add(current.toString())
        return result
    }

    private fun mapToContact(map: Map<String, String>): Contact? {
        val firstName = map["First Name"] ?: ""
        val middleName = map["Middle Name"] ?: ""
        val lastName = map["Last Name"] ?: ""
        val org = map["Organization Name"] ?: ""
        val notes = map["Notes"] ?: ""
        val labels = map["Labels"] ?: ""
        val phoneLabel = map["Phone 1 - Label"] ?: "Mobile"
        val phoneValue = map["Phone 1 - Value"] ?: ""
        val addressFormatted = map["Address 1 - Formatted"] ?: ""
        val street = map["Address 1 - Street"] ?: ""
        val city = map["Address 1 - City"] ?: ""
        val region = map["Address 1 - Region"] ?: ""
        val postalCode = map["Address 1 - Postal Code"] ?: ""
        val country = map["Address 1 - Country"] ?: ""
        val extendedAddress = map["Address 1 - Extended Address"] ?: ""

        // Extract profile URL from notes if present
        var profileUrl = ""
        val urlRegex = "(https?://[\\w./\\-?=&%#]+)".toRegex()
        val match = urlRegex.find(notes)
        if (match != null) {
            profileUrl = match.value
        }

        val isStarred = labels.contains("starred", ignoreCase = true)

        if (firstName.isBlank() && lastName.isBlank() && org.isBlank() && phoneValue.isBlank()) {
            return null
        }

        return Contact(
            firstName = firstName,
            middleName = middleName,
            lastName = lastName,
            organization = org,
            phoneNumber = phoneValue,
            phoneLabel = phoneLabel.ifBlank { "Mobile" },
            addressFormatted = addressFormatted,
            street = street,
            city = city,
            region = region,
            postalCode = postalCode,
            country = country,
            extendedAddress = extendedAddress,
            notes = notes,
            profileUrl = profileUrl,
            labels = labels,
            isStarred = isStarred
        )
    }

    /**
     * Pre-defined contact for Nesrin Ouis from Google Contacts CSV
     */
    val NESRIN_OUIS_CONTACT = Contact(
        id = 1,
        firstName = "Nesrin Ouis",
        lastName = "",
        organization = "Nesrin Ouis",
        phoneNumber = "+1 438-985-5041",
        phoneLabel = "Mobile",
        email = "",
        addressFormatted = "4347 Rue De Venise Saint-Leonard QC H1S1G2 Canada",
        street = "4347 Rue De Venise",
        city = "Saint-Leonard",
        region = "QC",
        postalCode = "H1S1G2",
        country = "Canada",
        extendedAddress = "Saint-Leonard",
        notes = "Here is the link to Nesrin Ouis profile on CanadaPages.com. https://www.canadapages.com/wp/nesrin-ouis-saintleonard-qc-4383809395/",
        profileUrl = "https://www.canadapages.com/wp/nesrin-ouis-saintleonard-qc-4383809395/",
        labels = "* myContacts ::: * starred",
        isStarred = true
    )
}
