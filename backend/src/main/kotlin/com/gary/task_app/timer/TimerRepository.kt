package com.gary.task_app.timer

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TimerRepository: JpaRepository<Timer, UUID>
