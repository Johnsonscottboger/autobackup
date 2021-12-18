package com.aning.autobackup.model

import com.aning.autobackup.config.DatabaseType

data class BackupFiles(
    val databaseType: DatabaseType,
    val backupPath: String,
    val files: List<String>
)
