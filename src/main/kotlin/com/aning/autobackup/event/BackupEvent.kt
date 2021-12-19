package com.aning.autobackup.event

import com.aning.autobackup.config.Option
import com.aning.autobackup.eventbus.IEvent

data class BackupEvent(
    val option: Option,
    val success: Boolean,
    val backupPath: String,
    val exception: Exception? = null
) : IEvent
