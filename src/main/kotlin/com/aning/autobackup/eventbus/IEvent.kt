package com.aning.autobackup.eventbus

import java.time.LocalDateTime

interface IEvent {
    val raiseTime: LocalDateTime
        get() = LocalDateTime.now()
}