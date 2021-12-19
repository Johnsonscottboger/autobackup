package com.aning.autobackup.service.backup.impl

import com.aning.autobackup.config.DatabaseType
import com.aning.autobackup.config.Option
import com.aning.autobackup.extensions.attributes
import com.aning.autobackup.service.backup.IBackup
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.system.measureTimeMillis

@Service
class MySqlBackupImpl : IBackup {

    private val log = LoggerFactory.getLogger(this::class.java)

    override val databaseType: DatabaseType
        get() = DatabaseType.MySql

    override fun execute(option: Option): String {
        val now = LocalDateTime.now()
        val path = Path(option.backupPath, now.format(DateTimeFormatter.ofPattern("yyyyMMdd")))
        if (!path.exists())
            path.createDirectories()
        val os = System.getProperty("os.name")

        for (database in option.databaseNames) {
            log.info("\tMySql: 正在备份 $database")
            val file = Path(path.toString(), "$database.sql")
            val cmd =
                "mysqldump -h${option.ip} -P${option.port} -u${option.userName} -p${option.password} --databases $database > $file"
            val shell = if (os.startsWith("win", true)) arrayOf("cmd", "/c", cmd) else arrayOf("/bin/sh", "-c", cmd)
            val process = Runtime.getRuntime().exec(shell)
            val error = process.errorStream.reader().use { p -> p.readText() }
            val exitValue = process.waitFor()
            if (exitValue != 0)
                throw RuntimeException("Backup MySql error ${System.lineSeparator()} cmd: $cmd ${System.lineSeparator()} $error")
            log.info("\t$database 备份成功: $file")
        }

        var compressFileName: String
        log.info("\t开始压缩 $path")
        val duration = measureTimeMillis {
            compressFileName = compress(path.toString())
            path.toFile().deleteRecursively()
        }
        log.info("\t压缩完成, 耗时 $duration ms")

        if (option.keep > 0) {
            val backups = File(option.backupPath).listFiles()?.sortedByDescending { p ->
                p.attributes().creationTime()
            }
            backups?.drop(option.keep)?.forEach { p ->
                p.deleteRecursively()
            }
        }
        return compressFileName
    }
}