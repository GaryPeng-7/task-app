package com.gary.task_app

import org.springframework.http.MediaType
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.transaction.annotation.Transactional

@Transactional
class BlockOwnershipIT : IntegrationTestBase() {

    @Test
    fun `user A sees only their own blocks`() {
        val tokenA = registerAndLogin("alice@example.com")
        val tokenB = registerAndLogin("bob@example.com")
        createBlock(tokenA, "alice block 1")
        createBlock(tokenA, "alice block 2")
        createBlock(tokenB, "bob block 1")

        mockMvc.get("/api/blocks?date=2026-09-14") {
            header("Authorization", "Bearer $tokenA")
        }.andExpect {
            status { isOk() }
            jsonPath("$.length()") { value(2) }
            jsonPath("$[0].subject") {value("alice block 1")}
            jsonPath("$[1].subject") {value("alice block 2")}

        }
    }

    @Test
    fun `user A cannot smuggle user B block into a reorder`() {
        val tokenA = registerAndLogin("alice@example.com")
        val tokenB = registerAndLogin("bob@example.com")
        val aliceBlock1Id = createBlock(tokenA, "alice block 1")
        val aliceBlock2Id = createBlock(tokenA, "alice block 2")
        val bobBlock1Id = createBlock(tokenB, "bob block 1")

        mockMvc.patch("/api/blocks/reorder") {
            header("Authorization", "Bearer $tokenA")
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"blockDate":"2026-09-14","orderIds":["$aliceBlock1Id","$bobBlock1Id"]}
            """.trimIndent()
        }.andExpect {
            status { isNotFound() }
        }

        mockMvc.get("/api/blocks?date=2026-09-14") {
            header("Authorization", "Bearer $tokenA")
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].id") { value(aliceBlock1Id) }
            jsonPath("$[1].id") { value(aliceBlock2Id) }
            jsonPath("$.length()") { value(2) }
        }
    }

    @Test
    fun `user A cannot modify user B block directly`() {
        val tokenA = registerAndLogin("alice@example.com")
        val tokenB = registerAndLogin("bob@example.com")
        val bobBlock1Id = createBlock(tokenB, "bob block 1")

        mockMvc.delete("/api/blocks/$bobBlock1Id") {
            header("Authorization", "Bearer $tokenA")
        }.andExpect {
            status { isNotFound() }
        }

        mockMvc.get("/api/blocks?date=2026-09-14") {
            header("Authorization", "Bearer $tokenB")
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].id") { value(bobBlock1Id) }
            jsonPath("$.length()") { value(1) }
        }
    }
}
