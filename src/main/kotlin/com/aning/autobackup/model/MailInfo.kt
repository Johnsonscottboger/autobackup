package com.aning.autobackup.model

data class MailInfo(
    val title: String,
    val dateTime: String,
    val databaseType: String,
    val databaseIpPort: String,
    val success: Boolean,
    val backupPath: String,
    val message: String? = null
)
