package com.gary.task_app

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.shaded.org.bouncycastle.asn1.x500.style.RFC4519Style.st

@Transactional
class AuthIT: IntegrationTestBase() {

    @Test
    fun `given valid payload, when register, then succeeds`() {
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"email":"alice@example.com","password":"BlockPass123!"}
            """.trimIndent()
        }.andExpect {
            status { isCreated() }
        }
    }

    @Test
    fun `given registered user, when login with correct password, then token returned`() {
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"email":"bob@example.com","password":"BlockPass456!"}
            """.trimIndent()
        }.andExpect { status {is2xxSuccessful() } }

        val result = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"email":"bob@example.com","password":"BlockPass456!"}
            """.trimIndent()
        }.andExpect {
            status { isOk() }
            jsonPath("$.token") { exists() }
        }.andReturn()

        val token = com.jayway.jsonpath.JsonPath
            .read<String>(result.response.contentAsString, "$.token")

        mockMvc.get("/api/me") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isOk() } }
    }

    @Test
    fun `given registered user, when login with wrong password, then 401`() {
        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"email":"carlo@example.com","password":"pw12345"}
            """.trimIndent()
        }

        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"email":"carlo@example.com","password":"WRONG"}
            """.trimIndent()
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `given no token, when I hit a protected endpoint, then 401`() {
        mockMvc.get("/api/me")
            .andExpect { status { isUnauthorized() }}
    }
}
