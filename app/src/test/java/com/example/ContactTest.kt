package com.example

import com.example.data.util.GoogleContactsCsvParser
import org.junit.Assert.*
import org.junit.Test

class ContactTest {

    @Test
    fun testParseGoogleContactsCsv_NesrinOuis() {
        val csv = """
            First Name,Middle Name,Last Name,Phonetic First Name,Phonetic Middle Name,Phonetic Last Name,Name Prefix,Name Suffix,Nickname,File As,Organization Name,Organization Title,Organization Department,Birthday,Notes,Photo,Labels,Phone 1 - Label,Phone 1 - Value,Address 1 - Label,Address 1 - Formatted,Address 1 - Street,Address 1 - City,Address 1 - PO Box,Address 1 - Region,Address 1 - Postal Code,Address 1 - Country,Address 1 - Extended Address
            Nesrin Ouis,,,,,,,,,,Nesrin Ouis,,,,Here is the link to Nesrin Ouis profile on CanadaPages.com. https://www.canadapages.com/wp/nesrin-ouis-saintleonard-qc-4383809395/,,* myContacts ::: * starred,Mobile,+1 438-985-5041,Home,4347 Rue De Venise Saint-Leonard QC H1S1G2 Canada,4347 Rue De Venise,,,QC,H1S1G2,Canada,Saint-Leonard
        """.trimIndent()

        val contacts = GoogleContactsCsvParser.parseContactsCsv(csv)
        assertEquals(1, contacts.size)

        val contact = contacts[0]
        assertEquals("Nesrin Ouis", contact.firstName)
        assertEquals("+1 438-985-5041", contact.phoneNumber)
        assertEquals("Mobile", contact.phoneLabel)
        assertEquals("Nesrin Ouis", contact.organization)
        assertEquals("4347 Rue De Venise Saint-Leonard QC H1S1G2 Canada", contact.addressFormatted)
        assertEquals("4347 Rue De Venise", contact.street)
        assertEquals("QC", contact.region)
        assertEquals("H1S1G2", contact.postalCode)
        assertEquals("Canada", contact.country)
        assertEquals("Saint-Leonard", contact.extendedAddress)
        assertTrue(contact.isStarred)
        assertEquals("https://www.canadapages.com/wp/nesrin-ouis-saintleonard-qc-4383809395/", contact.profileUrl)
        assertEquals("NO", contact.initials)
    }
}
