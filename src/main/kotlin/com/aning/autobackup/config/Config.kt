package com.aning.autobackup.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("backup")
class Config {
    var filePort: Int? = null
    lateinit var options: List<Option>
    var notification: Notification? = null
}

class Option {
    lateinit var databaseType: DatabaseType
    var backupPath: String = "/usr/backup"
    var ip: String = "127.0.0.1"
    var port: Int = 3306
    lateinit var userName: String
    lateinit var password: String
    lateinit var databaseNames: Array<String>
    var keep: Int = 2
    var executeTime: String = "03:00:00"
}

class Notification {
    lateinit var email: String
    var success: Boolean = false
    var error: Boolean = true
}

enum class DatabaseType {
    MySql,
    Mongodb
}