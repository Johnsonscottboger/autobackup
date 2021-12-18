package com.aning.autobackup.extensions

import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes

fun File.attributes(): BasicFileAttributes {
    return Files.readAttributes(this.toPath(), BasicFileAttributes::class.java)
}