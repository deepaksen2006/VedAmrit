package com.example.vedaahar.network

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class CareDirectoryApiTest {
    @Test fun nearbyUsesDjangoCoordinatesRadiusSearchAndPageWithoutJwtOrProviderKey() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("""
                {"count":1,"next":null,"previous":null,"results":[
                {"id":1,"name":"Isolated nearby fixture","description":"","address":"Test street","city":"Udaipur",
                 "state":"Rajasthan","postal_code":"313001","latitude":"24.585400","longitude":"73.712500",
                 "phone":"","email":"","facilities":[],"verification_status":"VERIFIED","updated_at":null,
                 "location_status":"APPROVED","location_attribution":"Fixture attribution","distance_km":1.25,"booking_enabled":false}]}
            """.trimIndent()))
            val result = CareDirectoryClient.create(server.url("/").toString()).nearbyHospitals(
                24.5854, 73.7125, 25.0, "A & B", page = 2)
            val row = result.body()!!.results.single()
            assertEquals("APPROVED", row.locationStatus)
            assertEquals(1.25, row.distanceKm!!, 0.0)
            assertEquals("Fixture attribution", row.locationAttribution)
            val request = server.takeRequest(3, TimeUnit.SECONDS)!!
            assertEquals("/api/care/hospitals/nearby/", request.requestUrl!!.encodedPath)
            assertEquals("24.5854", request.requestUrl!!.queryParameter("latitude"))
            assertEquals("73.7125", request.requestUrl!!.queryParameter("longitude"))
            assertEquals("25.0", request.requestUrl!!.queryParameter("radius_km"))
            assertEquals("A & B", request.requestUrl!!.queryParameter("search"))
            assertEquals("2", request.requestUrl!!.queryParameter("page"))
            assertNull(request.getHeader("Authorization"))
            assertNull(request.requestUrl!!.queryParameter("key"))
        } finally { server.shutdown() }
    }

    @Test fun anonymousHospitalPagePreservesDecimalStringsAndPagination() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("""
                {"count":2,"next":"http://example.test/api/care/hospitals/?page=2","previous":null,"results":[
                {"id":9000000001,"name":"Isolated HTTP fixture","description":"","address":"Test street","city":"Pune",
                 "state":"Maharashtra","postal_code":"411001","latitude":"18.520400","longitude":"73.856700",
                 "phone":"9900000000","email":"","facilities":[],"verification_status":"VERIFIED","updated_at":null}]}
            """.trimIndent()))
            val result = CareDirectoryClient.create(server.url("/").toString()).hospitals(city = "Pune", pageSize = 1)
            assertTrue(result.isSuccessful)
            assertEquals(2, result.body()!!.count)
            assertNotNull(result.body()!!.next)
            val row = result.body()!!.results.single()
            assertEquals(9000000001L, row.id)
            assertEquals("18.520400", row.latitude)
            val request = requireNotNull(server.takeRequest(3, TimeUnit.SECONDS))
            assertEquals("/api/care/hospitals/", request.requestUrl!!.encodedPath)
            assertEquals("Pune", request.requestUrl!!.queryParameter("city"))
            assertEquals("1", request.requestUrl!!.queryParameter("page_size"))
            assertNull(request.getHeader("Authorization"))
        } finally { server.shutdown() }
    }

    @Test fun doctorPageMapsCredentialsAndAffiliationsWithoutInventingProfileFields() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setHeader("Content-Type", "application/json").setBody("""
                {"count":1,"next":null,"previous":null,"results":[{"id":5,"full_name":"HTTP fixture doctor",
                "qualifications":["Fixture degree"],"specialization":"Ayurveda","registration_number":"TEST-001",
                "registration_council":"Fixture council","experience_years":5,"bio":"","verification_status":"VERIFIED",
                "updated_at":"2026-10-26T10:00:00+05:30","credentials_reviewed_at":"2026-10-26T09:00:00+05:30",
                "affiliations":[{"id":3,"hospital_id":2,"verification_status":"VERIFIED",
                "hospital_name":"HTTP fixture hospital","city":"Pune","consultation_mode":"BOTH","fees":"500.00"}]}]}
            """.trimIndent()))
            val result = CareDirectoryClient.create(server.url("/").toString()).doctors(hospitalId = 2, specialization = "Ayurveda")
            val row = result.body()!!.results.single()
            assertEquals("TEST-001", row.registrationNumber)
            assertEquals("500.00", row.affiliations.single().fees)
            assertEquals("BOTH", row.affiliations.single().consultationMode)
            assertEquals(5, row.experienceYears)
            assertNotNull(row.credentialsReviewedAt)
            assertEquals("VERIFIED", row.affiliations.single().verificationStatus)
            val request = server.takeRequest(3, TimeUnit.SECONDS)!!
            assertEquals("2", request.requestUrl!!.queryParameter("hospital_id"))
            assertEquals("Ayurveda", request.requestUrl!!.queryParameter("specialization"))
            assertNull(request.getHeader("Authorization"))
            assertNull(request.requestUrl!!.queryParameter("key"))
        } finally { server.shutdown() }
    }

    @Test fun missingOrRestrictedRecordRemainsAnHttpErrorRatherThanMockSuccess() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(404).setBody("""{"detail":"Not found."}"""))
            val result = CareDirectoryClient.create(server.url("/").toString()).hospital(99)
            assertEquals(404, result.code())
            assertNull(result.body())
        } finally { server.shutdown() }
    }

    @Test fun doctorDetailUsesPublicRouteAndDoesNotFollowPaginationLinks() = runTest {
        val server = MockWebServer()
        server.start()
        try {
            server.enqueue(MockResponse().setResponseCode(404))
            val result = CareDirectoryClient.create(server.url("/").toString()).doctor(123)
            assertEquals(404, result.code())
            val request = server.takeRequest(3, TimeUnit.SECONDS)!!
            assertEquals("/api/care/doctors/123/", request.requestUrl!!.encodedPath)
            assertNull(request.getHeader("Authorization"))
        } finally { server.shutdown() }
    }
}
