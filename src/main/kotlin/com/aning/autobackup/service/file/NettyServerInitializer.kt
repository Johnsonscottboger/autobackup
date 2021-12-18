package com.aning.autobackup.service.file

import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.stream.ChunkedWriteHandler

class NettyServerInitializer : ChannelInitializer<SocketChannel>() {
    override fun initChannel(channel: SocketChannel?) {
        val pipeline = channel!!.pipeline()
        pipeline.addLast(HttpServerCodec())
            .addLast(HttpObjectAggregator(65536))
            .addLast(ChunkedWriteHandler())
            .addLast(NettyServerHandler())
    }
}