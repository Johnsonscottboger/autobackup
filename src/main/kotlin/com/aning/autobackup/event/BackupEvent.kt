package com.aning.autobackup.event

import com.aning.autobackup.eventbus.IEvent

data class BackupEvent(
    val fileName: String
) : IEvent
