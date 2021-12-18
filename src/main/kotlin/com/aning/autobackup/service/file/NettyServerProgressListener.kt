package com.aning.autobackup.service.file

import io.netty.channel.ChannelProgressiveFuture
import io.netty.channel.ChannelProgressiveFutureListener
import io.netty.channel.ChannelProgressivePromise
import org.slf4j.LoggerFactory
import kotlin.math.round

class NettyServerProgressListener : ChannelProgressiveFutureListener {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun operationProgressed(future: ChannelProgressiveFuture?, progress: Long, total: Long) {
        if (total < 0) {
            log.info("${future?.channel()} 传输进度: $progress")
        } else {
            log.info("${future?.channel()} 传输进度: ${round((progress / total).toDouble())}")
        }
    }

    override fun operationComplete(future: ChannelProgressiveFuture?) {
        log.info("${future?.channel()} 传输完毕")
    }
}