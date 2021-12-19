package com.aning.autobackup.service.core

import com.aning.autobackup.config.Config
import com.aning.autobackup.event.BackupEvent
import com.aning.autobackup.eventbus.IEventBus
import com.aning.autobackup.service.backup.IBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class Scheduled {

    private val log = LoggerFactory.getLogger(this::class.java)

    private val formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    @Autowired
    private lateinit var config: Config

    @Autowired
    private lateinit var eventBus: IEventBus

    @Autowired
    private lateinit var backup: Array<IBackup>

    private var start: Boolean = false

    fun start() {
        start = true
        log.info("定时任务已启动")
    }

    fun stop() {
        start = false
        log.info("定时任务已停止")
    }

    @Scheduled(initialDelay = 10000, fixedRate = 61000)
    fun task() {
        if (!start) return
        log.info("定时任务执行中")
        execute(false)
    }

    fun execute(immediately: Boolean = false) = runBlocking(Dispatchers.IO) {
        val now = LocalDateTime.now()
        for (option in config.options) {
            if (!immediately) {
                if (option.executeTime != now.format(formatter))
                    continue
            }
            val impls = backup.filter { p -> p.databaseType == option.databaseType }
            if (impls.isEmpty()) {
                log.error("找不到支持的数据库类型:${option.databaseType}")
            }
            launch {
                impls.forEach {
                    try {
                        val fileName = it.execute(option)
                        eventBus.publishAsync(
                            BackupEvent(
                                option = option,
                                success = true,
                                backupPath = fileName
                            )
                        )
                    } catch (ex: Exception) {
                        log.error("备份失败", ex)
                        eventBus.publishAsync(
                            BackupEvent(
                                option = option,
                                success = false,
                                backupPath = "",
                                exception = ex,
                            )
                        )
                    }
                }
            }
        }
    }
}