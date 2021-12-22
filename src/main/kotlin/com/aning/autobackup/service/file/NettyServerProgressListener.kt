package com.aning.autobackup.service.file

import io.netty.channel.ChannelProgressiveFuture
import io.netty.channel.ChannelProgressiveFutureListener
import org.slf4j.LoggerFactory

class NettyServerProgressListener : ChannelProgressiveFutureListener {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun operationProgressed(future: ChannelProgressiveFuture?, progress: Long, total: Long) {

    }

    override fun operationComplete(future: ChannelProgressiveFuture?) {
        log.info("${future?.channel()} 传输完毕")
    }
}