package com.aning.autobackup.service.file

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContext

class NettyServer(context: ApplicationContext) {

    private val log = LoggerFactory.getLogger(this::class.java)

    private val port: Int = context.environment.getProperty("backup.filePort", Int::class.java) ?: 5014

    fun start() {
        val bossGroup = NioEventLoopGroup(1)
        val workerGroup = NioEventLoopGroup()
        try {
            val bootstrap = ServerBootstrap()
            bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel::class.java)
                .handler(LoggingHandler(LogLevel.INFO))
                .childHandler(NettyServerInitializer())
            val channel = bootstrap.bind(port).sync().channel()
            log.info("Netty server started at $port")
            channel.closeFuture().sync()
        } finally {
            bossGroup.shutdownGracefully()
            workerGroup.shutdownGracefully()
        }
    }
}