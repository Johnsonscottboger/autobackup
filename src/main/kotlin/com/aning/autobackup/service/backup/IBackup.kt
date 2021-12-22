package com.aning.autobackup.service.backup

import com.aning.autobackup.config.DatabaseType
import com.aning.autobackup.config.Option
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists

interface IBackup {

    private val log: Logger
        get() = LoggerFactory.getLogger(this::class.java)

    val databaseType: DatabaseType

    fun execute(option: Option): String

    fun compress(source: String): String {
        val file = File(source)
        val targetFileName = "$source.zip"

        val tempPath = Path(System.getProperty("java.io.tmpdir"), "autobackup", databaseType.name)
        if (tempPath.notExists())
            tempPath.createDirectories()
        val tempFile = File(tempPath.toString(), "${file.name}.zip")
        val outputStream = FileOutputStream(tempFile)
        ZipOutputStream(outputStream).use { zipOutputStream ->
            internalCompress(file, zipOutputStream, file.name)
        }
        val targetFile = File(targetFileName)
        log.info("\t开始移动临时文件: $tempFile")
        val result = tempFile.renameTo(targetFile)
        if (!result) {
            log.info("\t移动临时文件失败, 开始复制临时文件: $tempFile")
            tempFile.copyTo(targetFile, true)
        }
        if (!targetFile.exists())
            throw IOException("找不到压缩后的文件: $targetFile")
        return targetFileName
    }

    private fun internalCompress(file: File, zipOutputStream: ZipOutputStream, name: String) {
        if (file.isFile) {
            zipOutputStream.putNextEntry(ZipEntry(name))
            file.inputStream().copyTo(zipOutputStream)
            zipOutputStream.closeEntry()
        } else {
            val subFiles = file.listFiles()
            if (subFiles != null) {
                for (subFile in subFiles) {
                    internalCompress(subFile, zipOutputStream, "${name}/${subFile.name}")
                }
            }
        }
    }
}