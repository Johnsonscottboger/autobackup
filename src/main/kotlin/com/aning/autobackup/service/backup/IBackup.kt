package com.aning.autobackup.service.backup

import com.aning.autobackup.config.DatabaseType
import com.aning.autobackup.config.Option
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

interface IBackup {
    val databaseType: DatabaseType

    fun execute(option: Option): String

    fun compress(source: String) : String {
        val file = File(source)
        val targetFileName = "$source.zip"
        val outputStream = FileOutputStream(targetFileName)
        ZipOutputStream(outputStream).use { zipOutputStream ->
            internalCompress(file, zipOutputStream, file.name)
        }
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