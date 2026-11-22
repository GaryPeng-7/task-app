package com.gary.task_app

import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.http.MediaType
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional
class BlockReorderIT: IntegrationTestBase() {
    @Test
    fun `reorder with full reversed list persists new order`() {
        // given: one user with 3 blocks on a date — capture all three IDs
        val tokenA = registerAndLogin("alice@example.com")
        val block1 = createBlock(tokenA, "block1")
        val block2 = createBlock(tokenA, "block2")
        val block3 = createBlock(tokenA, "block3")

        // when:  PATCH /api/blocks/reorder with orderIds = [third, second, first]
        mockMvc.patch("/api/blocks/reorder") {
            header("Authorization", "Bearer $tokenA")
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"blockDate":"2026-09-14","orderIds":
                ["$block3","$block2","$block1"]}
            """.trimIndent()
        }.andExpect {
            // then:  status 200
            status { isOk() }
        }

        // and:   a FRESH GET returns the blocks in the reversed order
        mockMvc.get("/api/blocks?date=2026-09-14") {
            header("Authorization", "Bearer $tokenA")
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].id") { value(block3) }
            jsonPath("$[1].id") { value(block2) }
            jsonPath("$[2].id") { value(block1) }

        }
    }

    @Test
    fun `reorder rejects payload missing one block id`() {
        // given: one user with 3 blocks
        val tokenA = registerAndLogin("alice@example.com")
        val block1 = createBlock(tokenA, "block1")
        val block2 = createBlock(tokenA, "block2")
        val block3 = createBlock(tokenA, "block3")

        // when:  PATCH with orderIds containing only 2 of the 3 ids
        mockMvc.patch("/api/blocks/reorder") {
            header("Authorization", "Bearer $tokenA")
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"blockDate":"2026-09-14","orderIds":
                ["$block2","$block1"]}
            """.trimIndent()
        }.andExpect {
            // then:  404 (size check fails)
            status { isNotFound() }
        }

        // and:   GET confirms the original order is unchanged
        mockMvc.get("/api/blocks?date=2026-09-14") {
            header("Authorization", "Bearer $tokenA")
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].id") { value(block1) }
            jsonPath("$[1].id") { value(block2) }
            jsonPath("$[2].id") { value(block3) }
        }
    }

    @Test
    fun `reorder rejects payload with a duplicate id`() {
        // given: one user with 3 blocks (ids a1, a2, a3)
        val tokenA = registerAndLogin("alice@example.com")
        val block1 = createBlock(tokenA, "block1")
        val block2 = createBlock(tokenA, "block2")
        val block3 = createBlock(tokenA, "block3")

        // when:  PATCH with orderIds = [a1, a1, a2]
        mockMvc.patch("/api/blocks/reorder") {
            header("Authorization", "Bearer $tokenA")
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"blockDate":"2026-09-14","orderIds":
                ["$block1","$block1","$block2"]}
            """.trimIndent()
        }.andExpect {
            //        — right SIZE (3), but the set shrinks to {a1, a2}
            // then:  404 — and note: ONLY the set-equality check can catch this one
            status { isNotFound() }
        }

        // and:   GET confirms original order unchanged
        mockMvc.get("/api/blocks?date=2026-09-14") {
            header("Authorization", "Bearer $tokenA")
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].id") { value(block1) }
            jsonPath("$[1].id") { value(block2) }
            jsonPath("$[2].id") { value(block3) }
        }
    }

    @Test
    fun `reorder rejects payload with an unknown uuid`() {
        // given: one user with 2 blocks
        val tokenA = registerAndLogin("alice@example.com")
        val block1 = createBlock(tokenA, "block1")
        val block2 = createBlock(tokenA, "block2")

        // when:  PATCH with orderIds = [realId, UUID.randomUUID().toString()]
        //        — right size, but one id belongs to nobody
        val newId = UUID.randomUUID().toString()
        mockMvc.patch("/api/blocks/reorder") {
            header("Authorization", "Bearer $tokenA")
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"blockDate":"2026-09-14","orderIds":
                ["$block1","$newId"]}
            """.trimIndent()
        }.andExpect {
            // then:  404
            status { isNotFound() }
        }


        // and:   GET confirms original order unchanged
        mockMvc.get("/api/blocks?date=2026-09-14") {
            header("Authorization", "Bearer $tokenA")
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].id") { value(block1) }
            jsonPath("$[1].id") { value(block2) }
        }
    }

    @Test
    fun `reorder on one date leaves other dates untouched`() {
        // given: one user with 2 blocks on date X and 2 blocks on date Y
        val tokenA = registerAndLogin("alice@example.com")
        val block1 = createBlock(tokenA, "block1")
        val block2 = createBlock(tokenA, "block2")
        val block3 = createBlock(tokenA, "block3", "2026-09-15")
        val block4 = createBlock(tokenA, "block4", "2026-09-15")

        //        (createBlock takes a date parameter — use it)
        // when:  PATCH a valid reorder for date X
        mockMvc.patch("/api/blocks/reorder") {
            header("Authorization", "Bearer $tokenA")
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"blockDate":"2026-09-14","orderIds":
                ["$block2","$block1"]}
            """.trimIndent()
        }.andExpect {
            status { isOk() }
        }
        // then:  200, and GET date X shows the new order
        mockMvc.get("/api/blocks?date=2026-09-14") {
            header("Authorization", "Bearer $tokenA")
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].id") { value(block2) }
            jsonPath("$[1].id") { value(block1) }
        }
        // and:   GET date Y shows ITS original order, untouched
        //        — this guards the blockDate scoping in your repository query
        mockMvc.get("/api/blocks?date=2026-09-15") {
            header("Authorization", "Bearer $tokenA")
        }.andExpect {
            status { isOk() }
            jsonPath("$[0].id") { value(block3) }
            jsonPath("$[1].id") { value(block4) }
        }
    }

}
