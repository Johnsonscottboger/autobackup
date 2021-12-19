package com.aning.autobackup

import com.aning.autobackup.service.core.Scheduled
import com.aning.autobackup.service.file.NettyServer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class AutobackupApplication

fun main(args: Array<String>) {
    val context = runApplication<AutobackupApplication>(*args)
    NettyServer(context).start()
}
