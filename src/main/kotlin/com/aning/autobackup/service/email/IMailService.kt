package com.aning.autobackup.service.email

import com.aning.autobackup.model.MailInfo

interface IMailService {

    fun setPassword(username: String, password: String)

    fun getPassword(username: String) : String?

    fun test()

    fun send(to: String, content: MailInfo)
}