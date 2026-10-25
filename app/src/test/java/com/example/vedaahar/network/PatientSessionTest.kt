package com.example.vedaahar.network

import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

class PatientSessionTest {
    private fun token(id: Int) = "header.${Base64.getUrlEncoder().withoutPadding().encodeToString("{\"user_id\":$id}".toByteArray())}.signature"

    @Test fun patientAndBackendBothPartitionLocalRecords() {
        assertEquals(PatientSession.keyFor(token(1), "https://one.example/"), PatientSession.keyFor(token(1), "https://one.example/"))
        assertNotEquals(PatientSession.keyFor(token(1), "https://one.example/"), PatientSession.keyFor(token(2), "https://one.example/"))
        assertNotEquals(PatientSession.keyFor(token(1), "https://one.example/"), PatientSession.keyFor(token(1), "https://two.example/"))
        assertNull(PatientSession.keyFor(null, "https://one.example/"))
        assertNull(PatientSession.keyFor("not-a-jwt", "https://one.example/"))
    }
}
