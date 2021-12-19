package com.aning.autobackup.service.notification.impl

import com.aning.autobackup.config.Config
import com.aning.autobackup.event.BackupEvent
import com.aning.autobackup.eventbus.IEventHandler
import com.aning.autobackup.model.MailInfo
import com.aning.autobackup.service.email.IMailService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class DefaultNotificationServiceImpl : IEventHandler<BackupEvent> {

    @Autowired
    private lateinit var config: Config

    @Autowired
    private lateinit var mailService: IMailService

    override fun handle(e: BackupEvent) {
        val notification = config.notification ?: return
        if (notification.success && e.success) {
            mailService.send(
                notification.email, MailInfo(
                    "备份成功",
                    dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    databaseType = e.option.databaseType.name,
                    databaseIpPort = "${e.option.ip}:${e.option.port}",
                    success = true,
                    backupPath = e.backupPath
                )
            )
        }

        if (notification.error && !e.success) {
            mailService.send(
                notification.email, MailInfo(
                    title = "备份失败",
                    dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    databaseType = e.option.databaseType.name,
                    databaseIpPort = "${e.option.ip}:${e.option.port}",
                    success = false,
                    backupPath = "",
                    message = e.exception.toString()
                )
            )
        }
    }
}