package com.aning.autobackup.service.email.impl

import com.aning.autobackup.model.MailInfo
import com.aning.autobackup.service.email.IMailService
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context
import java.io.File
import java.util.*
import javax.annotation.Resource
import kotlin.io.path.createDirectories
import kotlin.reflect.full.memberProperties

@Service
class DefaultMailServiceImpl : IMailService {

    @Resource
    private lateinit var sender: JavaMailSenderImpl

    @Resource
    private lateinit var templateEngine: TemplateEngine

    private val profile: String
        get() {
            val os = System.getProperty("os.name")
            return if (os.startsWith("win", true)) {
                "${System.getProperty("user.name")}\\AppData\\autobackup\\.profile"
            } else {
                "${System.getProperty("user.name")}/autobackup/.profile"
            }
        }

    override fun setPassword(username: String, password: String) {
        val file = File(profile)
        if (!file.parentFile.exists())
            file.parentFile.toPath().createDirectories()
        val text = "${username}:$password\n"
        val bytes = Base64.getEncoder().encode(text.toByteArray())
        file.writeBytes(bytes)
    }

    override fun getPassword(username: String): String? {
        val file = File(profile)
        if (!file.exists() || !file.isFile)
            return null
        val bytes = Base64.getDecoder().decode(file.readBytes())
        val text = String(bytes)
        val lines = text.split('\n')
        for (line in lines) {
            val pair = line.split(':')
            if (pair.count() == 2) {
                if (pair[0] == username)
                    return pair[1]
            }
        }
        return null
    }

    override fun test() {
        if (sender.username.isNullOrEmpty())
            throw IllegalStateException("?????????????????????")
        sender.password = getPassword(sender.username!!)
        if (sender.password.isNullOrEmpty())
            throw IllegalStateException("?????????????????????")
        sender.testConnection()
        val message = sender.createMimeMessage()
        MimeMessageHelper(message).apply {
            setSubject("??????????????????")
            setTo(sender.username!!)
            setFrom(sender.username!!)
            setSentDate(Date())
            setText("?????????????????????, ????????????????????????!")
        }
        sender.send(message)
    }

    override fun send(to: String, content: MailInfo) {
        val context = Context()
        for (prop in MailInfo::class.memberProperties) {
            context.setVariable(prop.name, prop.get(content))
        }
        val text = templateEngine.process("mail.html", context)
        sender.password = getPassword(sender.username!!)

        val message = sender.createMimeMessage()
        MimeMessageHelper(message, true).apply {
            setSubject("?????????${if (content.success) "??????" else "??????"}???")
            setTo(to)
            setFrom(sender.username!!)
            setSentDate(Date())
            setText(text, true)
        }
        sender.send(message)
    }
}