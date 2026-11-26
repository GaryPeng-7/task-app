package com.gary.task_app

import com.gary.task_app.taskblock.BlockStatus
import com.gary.task_app.taskblock.TaskBlock
import com.gary.task_app.taskblock.TaskBlockRepository
import com.gary.task_app.timer.Timer
import com.gary.task_app.timer.TimerRepository
import com.gary.task_app.timer.TimerService
import com.gary.task_app.timer.TimerState
import com.gary.task_app.user.User
import com.gary.task_app.user.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.web.server.ResponseStatusException
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

// Add imports for these existing project classes with IntelliJ auto-import:
// BlockStatus, TaskBlock, TaskBlockRepository, User, UserRepository

@SpringBootTest
@Testcontainers
class TimerServiceIntegrationTest {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")

        @DynamicPropertySource
        @JvmStatic
        fun configureTestDatabase(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)

            registry.add("jwt.secret") {
                "integration-test-secret-with-at-least-32-characters"
            }
        }
    }

    @Autowired
    private lateinit var timerService: TimerService

    @Autowired
    private lateinit var timerRepository: TimerRepository

    @Autowired
    private lateinit var taskBlockRepository: TaskBlockRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun cleanDatabase() {
        timerRepository.deleteAll()
        taskBlockRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `start persists timer and marks block in progress`() {
        val user = newUser("start@example.com")
        val block = newBlock(user, position = 0)

        val response = timerService.start(block.id, user.id)

        assertEquals(TimerState.WORK_RUNNING, response.state)
        assertNotNull(response.endAt)

        val persistedTimer = timerRepository.findById(block.id).orElseThrow()
        val persistedBlock = taskBlockRepository.findById(block.id).orElseThrow()

        assertEquals(TimerState.WORK_RUNNING, persistedTimer.timerState)
        assertNotNull(persistedTimer.endsAt)
        assertNull(persistedTimer.remainingSeconds)
        assertEquals(BlockStatus.IN_PROGRESS, persistedBlock.status)
    }

    @Test
    fun `starting an already running timer is idempotent`() {
        val user = newUser("idempotent@example.com")
        val block = newBlock(user, position = 0)

        val firstResponse = timerService.start(block.id, user.id)
        val secondResponse = timerService.start(block.id, user.id)

        assertEquals(TimerState.WORK_RUNNING, secondResponse.state)
        assertEquals(firstResponse.endAt, secondResponse.endAt)

        val persistedTimer = timerRepository.findById(block.id).orElseThrow()
        assertEquals(firstResponse.endAt, persistedTimer.endsAt)
    }

    @Test
    fun `pause and resume preserve the remaining duration`() {
        val user = newUser("pause-resume@example.com")
        val block = newBlock(user, position = 0)

        timerService.start(block.id, user.id)
        timerService.pause(block.id, user.id)

        val pausedTimer = timerRepository.findById(block.id).orElseThrow()
        val frozenSeconds = pausedTimer.remainingSeconds

        assertEquals(TimerState.WORK_PAUSED, pausedTimer.timerState)
        assertNotNull(frozenSeconds)
        assertNull(pausedTimer.endsAt)

        timerService.resume(block.id, user.id)

        val resumedTimer = timerRepository.findById(block.id).orElseThrow()

        assertEquals(TimerState.WORK_RUNNING, resumedTimer.timerState)
        assertNull(resumedTimer.remainingSeconds)
        assertNotNull(resumedTimer.endsAt)

        val actualRemaining = Duration.between(
            Instant.now(),
            resumedTimer.endsAt,
        ).seconds

        assertTrue(
            actualRemaining in (frozenSeconds!! - 2L)..(frozenSeconds + 1L),
            "Expected approximately $frozenSeconds seconds, got $actualRemaining",
        )
    }

    @Test
    fun `illegal intent returns conflict and rolls back lazily created timer`() {
        val user = newUser("illegal@example.com")
        val block = newBlock(user, position = 0)

        val exception = assertThrows(ResponseStatusException::class.java) {
            timerService.pause(block.id, user.id)
        }

        assertEquals(HttpStatus.CONFLICT, exception.statusCode)

        assertTrue(
            timerRepository.findById(block.id).isEmpty,
            "A rejected intent should not persist a lazily created timer",
        )
    }

    @Test
    fun `user cannot operate another users timer`() {
        val owner = newUser("owner@example.com")
        val stranger = newUser("stranger@example.com")
        val block = newBlock(owner, position = 0)

        val exception = assertThrows(ResponseStatusException::class.java) {
            timerService.start(block.id, stranger.id)
        }

        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
        assertTrue(timerRepository.findById(block.id).isEmpty)
    }

    @Test
    fun `complete and skip break finish block and promote next block`() {
        val user = newUser("promotion@example.com")
        val firstBlock = newBlock(user, position = 0)
        val secondBlock = newBlock(user, position = 1)

        timerService.start(firstBlock.id, user.id)
        timerService.complete(firstBlock.id, user.id)

        val breakTimer = timerRepository.findById(firstBlock.id).orElseThrow()

        assertEquals(TimerState.BREAK_RUNNING, breakTimer.timerState)
        assertEquals(
            BlockStatus.FINISHED,
            taskBlockRepository.findById(firstBlock.id).orElseThrow().status,
        )

        timerService.skipBreak(firstBlock.id, user.id)

        val finishedTimer = timerRepository.findById(firstBlock.id).orElseThrow()
        val promotedBlock = taskBlockRepository.findById(secondBlock.id).orElseThrow()
        val promotedTimer = timerRepository.findById(secondBlock.id).orElseThrow()

        assertEquals(TimerState.DONE, finishedTimer.timerState)
        assertNull(finishedTimer.endsAt)
        assertNull(finishedTimer.remainingSeconds)
        assertEquals(BlockStatus.IN_PROGRESS, promotedBlock.status)
        assertEquals(TimerState.IDLE, promotedTimer.timerState)
    }

    @Test
    fun `pause after silent expiry never creates negative remaining time`() {
        val user = newUser("silent-expiry@example.com")
        val block = newBlock(user, position = 0)

        timerRepository.save(
            Timer(
                block = block,
                timerState = TimerState.WORK_RUNNING,
                endsAt = Instant.now().minusSeconds(10),
                remainingSeconds = null,
                workSeconds = 60,
                breakSeconds = 300,
            ),
        )

        val response = timerService.pause(block.id, user.id)

        assertEquals(TimerState.BREAK_PAUSED, response.state)

        val persistedTimer = timerRepository.findById(block.id).orElseThrow()
        val remaining = persistedTimer.remainingSeconds

        assertEquals(TimerState.BREAK_PAUSED, persistedTimer.timerState)
        assertNull(persistedTimer.endsAt)
        assertNotNull(remaining)
        assertTrue(remaining!! >= 0)
        assertTrue(remaining <= 300)
        assertEquals(
            BlockStatus.FINISHED,
            taskBlockRepository.findById(block.id).orElseThrow().status,
        )
    }

    @Test
    fun `getState recursively expires work and break after long absence`() {
        val user = newUser("recursive-expiry@example.com")
        val firstBlock = newBlock(user, position = 0)
        val secondBlock = newBlock(user, position = 1)

        timerRepository.save(
            Timer(
                block = firstBlock,
                timerState = TimerState.WORK_RUNNING,
                endsAt = Instant.now().minusSeconds(600),
                remainingSeconds = null,
                workSeconds = 60,
                breakSeconds = 300,
            ),
        )

        val response = timerService.getState(firstBlock.id, user.id)

        assertEquals(TimerState.DONE, response.state)

        val persistedTimer = timerRepository.findById(firstBlock.id).orElseThrow()
        val firstBlockAfterExpiry =
            taskBlockRepository.findById(firstBlock.id).orElseThrow()
        val secondBlockAfterExpiry =
            taskBlockRepository.findById(secondBlock.id).orElseThrow()

        assertEquals(TimerState.DONE, persistedTimer.timerState)
        assertEquals(BlockStatus.FINISHED, firstBlockAfterExpiry.status)
        assertEquals(BlockStatus.IN_PROGRESS, secondBlockAfterExpiry.status)
    }

    private fun newUser(email: String): User =
        userRepository.save(
            User(
                email = email,
                passwordHash = "not-used-by-these-tests",
            ),
        )

    private fun newBlock(
        owner: User,
        position: Int,
    ): TaskBlock =
        taskBlockRepository.save(
            TaskBlock(
                user = owner,
                subject = "Test block $position",
                blockDate = LocalDate.of(2026, 1, 1),
                position = position,
                status = BlockStatus.TO_BE_DONE,
            ),
        )
}
