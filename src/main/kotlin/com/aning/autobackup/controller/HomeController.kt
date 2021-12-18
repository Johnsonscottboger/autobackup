package com.aning.autobackup.controller

import com.aning.autobackup.config.Config
import com.aning.autobackup.model.BackupFiles
import com.aning.autobackup.service.core.Scheduled
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.tomcat.util.json.JSONParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.configurationprocessor.json.JSONObject
import org.springframework.boot.configurationprocessor.json.JSONStringer
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import java.io.File

@Controller
class HomeController {

    @Autowired
    private lateinit var config: Config

    @Autowired
    private lateinit var scheduled: Scheduled

    @ResponseBody
    @GetMapping("/", produces = ["text/html; charset=UTF-8"])
    fun index(): String {
        return """
            <!DOCTYPE html>
            <html><head><meta charset='utf-8' /><title>自动备份服务</title></head>
            <body>
            <h3>数据库自动备份服务: 支持 MySql, Mongodb</h3>
            <ul>
            <li><a href="./start">启动</a></li>
            <li><a href="./stop">停止</a></li>
            <li><a href="./config">查看配置</a></li>
            </ul>
        """.trimIndent()
    }

    @ResponseBody
    @GetMapping("/start")
    fun start(): String {
        scheduled.start()
        return "Start success!"
    }

    @ResponseBody
    @GetMapping("/stop")
    fun stop(): String {
        scheduled.stop()
        return "Stop"
    }

    @ResponseBody
    @GetMapping("/config")
    fun config(): Config {
        return config
    }

    @ResponseBody
    @GetMapping("/files")
    fun files(): List<BackupFiles> {
        val list = mutableListOf<BackupFiles>()
        for (option in config.options) {
            val path = File(option.backupPath)
            if (path.exists() && path.isDirectory) {
                val item = BackupFiles(
                    databaseType = option.databaseType,
                    backupPath = option.backupPath,
                    files = path.listFiles { p -> p.isFile && p.canRead() }!!.map { p -> p.name }
                )
                list.add(item)
            }
        }
        return list
    }
}