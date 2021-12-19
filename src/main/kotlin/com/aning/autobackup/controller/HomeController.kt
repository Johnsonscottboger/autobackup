package com.aning.autobackup.controller

import com.aning.autobackup.config.Config
import com.aning.autobackup.model.BackupFiles
import com.aning.autobackup.model.Profile
import com.aning.autobackup.service.core.Scheduled
import com.aning.autobackup.service.email.IMailService
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.tomcat.util.json.JSONParser
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.configurationprocessor.json.JSONObject
import org.springframework.boot.configurationprocessor.json.JSONStringer
import org.springframework.core.env.Environment
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.servlet.ModelAndView
import java.io.File

@Controller
class HomeController {

    @Autowired
    private lateinit var config: Config

    @Autowired
    private lateinit var scheduled: Scheduled

    @Autowired
    private lateinit var mailService: IMailService

    @Autowired
    private lateinit var environment: Environment

    @GetMapping("/")
    fun index(): String {
        return "index"
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
        return "Stop success!"
    }

    @ResponseBody
    @GetMapping("/execute")
    fun execute(): String {
        scheduled.start()
        scheduled.execute(true)
        return "OK"
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

    @GetMapping("/profile")
    fun profile(): ModelAndView {
        val model = Profile(environment.getProperty("spring.mail.username")!!)
        return ModelAndView("profile", "profile", model)
    }

    @PostMapping("/profile")
    fun profile(username: String, password: String): ModelAndView {
        mailService.setPassword(username, password)
        val model = Profile(environment.getProperty("spring.mail.username")!!)
        return ModelAndView("profile", "profile", model)
    }

    @ResponseBody
    @GetMapping("/test")
    fun test(): String {
        return try {
            mailService.test()
            "OK"
        } catch (ex: Exception) {
            "失败: ${ex.message}"
        }
    }
}