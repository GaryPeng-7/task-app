package com.gary.task_app.timer

import com.gary.task_app.taskblock.TaskBlock
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "timers")
class Timer (

    @Id
    @Column(name = "block_id")
    var blockId: UUID? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "block_id")
    var block: TaskBlock? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "timer_state", nullable = false, length = 20)
    var timerState: TimerState = TimerState.IDLE,

    @Column(name = "ends_at")
    var endsAt: Instant? = null,

    @Column(name = "remaining_seconds")
    var remainingSeconds: Int? = null,

    @Column(name = "work_seconds", nullable = false)
    var workSeconds: Int = 3000,

    @Column(name = "break_seconds", nullable = false)
    var breakSeconds: Int = 300
)
