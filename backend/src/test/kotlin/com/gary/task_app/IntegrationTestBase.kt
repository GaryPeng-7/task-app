package com.gary.task_app

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.testcontainers.postgresql.PostgreSQLContainer
import org.springframework.http.MediaType

@SpringBootTest
@AutoConfigureMockMvc
abstract class IntegrationTestBase {

    companion object {
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .apply { start() }
    }

    @Autowired
    lateinit var mockMvc: MockMvc

    protected fun registerAndLogin(email: String): String {
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"email":"$email","password":"pw12345"}
            """.trimIndent()
        }.andExpect { status { is2xxSuccessful() } }

        val result = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"email":"$email","password":"pw12345"}
            """.trimIndent()
        }.andReturn()

        return com.jayway.jsonpath.JsonPath.read(result.response.contentAsString, "$.token")
    }

    protected fun createBlock(token: String, subject: String, date: String = "2026-09-14"): String {
        val result = mockMvc.post("/api/blocks") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"subject":"$subject","blockDate":"$date"}
            """.trimIndent()
        }.andReturn()

        return com.jayway.jsonpath.JsonPath.read(result.response.contentAsString, "$.id")
    }
}
