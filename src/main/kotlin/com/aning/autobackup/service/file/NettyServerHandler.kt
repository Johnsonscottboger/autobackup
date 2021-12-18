package com.aning.autobackup.service.file

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.*
import io.netty.handler.stream.ChunkedFile
import io.netty.util.CharsetUtil
import io.netty.util.internal.SystemPropertyUtil
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import java.io.RandomAccessFile
import java.io.UnsupportedEncodingException
import java.lang.StringBuilder
import java.net.URLDecoder
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.activation.MimetypesFileTypeMap

class NettyServerHandler : SimpleChannelInboundHandler<FullHttpRequest>() {

    private lateinit var request: FullHttpRequest

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun channelRead0(ctx: ChannelHandlerContext?, request: FullHttpRequest?) {
        if (ctx == null || request == null) return
        if (!request.decoderResult().isSuccess) {
            sendError(ctx, HttpResponseStatus.BAD_REQUEST)
            return
        }

        if (!HttpMethod.GET.equals(request.method())) {
            sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED)
            return
        }
        this.request = request
        val keepAlive = HttpUtil.isKeepAlive(request)
        val uri = request.uri()
        val path = sanitizeUri(uri)
        if (path.isEmpty()) {
            sendError(ctx, HttpResponseStatus.FORBIDDEN)
            return
        }

        val file = File(path)
        if (file.isHidden || !file.exists()) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND)
            return
        }

        if (file.isDirectory) {
            if (uri.endsWith("/")) sendListing(ctx, file, uri)
            else sendRedirect(ctx, "$uri/")
            return
        }

        if (!file.isFile) {
            sendError(ctx, HttpResponseStatus.FORBIDDEN)
            return
        }

        val ifModifiedSince = request.headers().get(HttpHeaderNames.IF_MODIFIED_SINCE)
        if (!ifModifiedSince.isNullOrEmpty()) {
            val ifModifiedSinceDate = LocalDateTime.parse(ifModifiedSince)
            val ifModifiedSinceDateSecond = ifModifiedSinceDate.toEpochSecond(ZoneOffset.UTC)
            val lastModified = file.lastModified() / 1000
            if (lastModified == ifModifiedSinceDateSecond) {
                sendNotModified(ctx)
                return
            }
        }

        val randomAccessFile = try {
            RandomAccessFile(file, "r")
        } catch (ignore: FileNotFoundException) {
            sendError(ctx, HttpResponseStatus.NOT_FOUND)
            return
        }

        val fileLength = randomAccessFile.length()
        val response = DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK).apply {
            HttpUtil.setContentLength(this, fileLength)
            setKeepAliveHeader(this, keepAlive)
            setContentTypeHeader(this, file)
            setDateAndCacheHeader(this, file)
        }
        ctx.write(response)

        val sendFileFuture = ctx.writeAndFlush(
            HttpChunkedInput(ChunkedFile(randomAccessFile, 0, fileLength, 8192)), ctx
                .newProgressivePromise()
        )

        sendFileFuture.addListener(NettyServerProgressListener())
        if (!keepAlive)
            sendFileFuture.addListener(ChannelFutureListener.CLOSE)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext?, cause: Throwable?) {
        log.error("NettyServerHandler error", cause)
        if (ctx!!.channel().isActive) {
            sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR)
        }
    }

    private fun setKeepAliveHeader(response: DefaultHttpResponse, keepAlive: Boolean) {
        HttpUtil.setKeepAlive(response, keepAlive)
    }

    private fun setDateAndCacheHeader(response: DefaultHttpResponse, file: File) {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern(HTTP_DATE_FORMAT).withZone(ZoneId.systemDefault())
        response.headers().set(HttpHeaderNames.DATE, now.format(formatter))

        val expires = now.plusSeconds(HTTP_CACHE_SECONDS.toLong())
        response.headers().set(HttpHeaderNames.EXPIRES, expires.format(formatter))
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "private, max-age=$HTTP_CACHE_SECONDS")
        response.headers().set(
            HttpHeaderNames.LAST_MODIFIED,
            LocalDateTime.ofEpochSecond(file.lastModified() / 1000, 0, ZoneOffset.UTC).format(formatter)
        )
    }

    private fun setContentTypeHeader(response: DefaultHttpResponse, file: File) {
        val mimeTypeMap = MimetypesFileTypeMap()
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, mimeTypeMap.getContentType(file))
    }

    private fun sendNotModified(ctx: ChannelHandlerContext) {
        val response =
            DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_MODIFIED, Unpooled.EMPTY_BUFFER)
        setDateHeader(response)
        sendAndCleanupConnection(ctx, response)
    }

    private fun sendAndCleanupConnection(ctx: ChannelHandlerContext, response: DefaultFullHttpResponse) {
        val keepAlive = HttpUtil.isKeepAlive(this.request)
        HttpUtil.setContentLength(response, response.content().readableBytes().toLong())
        HttpUtil.setKeepAlive(response, keepAlive)
        val flushFuture = ctx.writeAndFlush(response)
        if (!keepAlive) {
            flushFuture.addListener(ChannelFutureListener.CLOSE)
        }
    }

    private fun setDateHeader(response: DefaultFullHttpResponse) {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern(HTTP_DATE_FORMAT).withZone(ZoneId.systemDefault())
        response.headers().set(HttpHeaderNames.DATE, now.format(formatter))
    }

    private fun sendListing(ctx: ChannelHandlerContext, file: File, uri: String?) {
        val builder = StringBuilder().apply {
            append("<!DOCTYPE html>\r\n")
            append("<html><head><meta charset='utf-8' /><title>")
            append(uri)
            append("file list")
            append("</title></head><body>\r\n")
            append("<h3>")
            append(uri)
            append("file list")
            append("</h3>\r\n")
            append("<ul>")
            append("<li><a href=\"../\">..</a></li>\r\n")
        }

        val files = file.listFiles()!!
        for (subFile in files) {
            if(subFile.isHidden || !subFile.canRead()) continue
            val name = subFile.name
            if(!ALLOW_FILE_NAME.matches(name)) continue
            builder.append("<li><a href=\"$name\">$name</a></li>\r\n")
        }
        builder.append("</ul></body></html>\r\n")
        val buffer = ctx.alloc().buffer(builder.length)
        buffer.writeCharSequence(builder.toString(), CharsetUtil.UTF_8)
        val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, buffer).apply {
            headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8")
        }
        sendAndCleanupConnection(ctx, response)
    }

    private fun sendRedirect(ctx: ChannelHandlerContext, uri: String) {
        val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FOUND, Unpooled.EMPTY_BUFFER).apply {
            headers().set(HttpHeaderNames.LOCATION, uri)
        }
        sendAndCleanupConnection(ctx, response)
    }

    private fun sendError(ctx: ChannelHandlerContext, status: HttpResponseStatus) {
        val response = DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, Unpooled.copiedBuffer("异常: $status\r\n", CharsetUtil.UTF_8)).apply {
            headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8")
        }
        sendAndCleanupConnection(ctx, response)
    }

    private fun sanitizeUri(uri: String): String {
        var decodeUri = try {
            URLDecoder.decode(uri, CharsetUtil.UTF_8.name())
        } catch (ex: UnsupportedEncodingException) {
            throw ex
        }

        if (decodeUri.isEmpty() || !decodeUri.startsWith('/')) return ""
        decodeUri = decodeUri.replace('/', File.separatorChar)

        if (decodeUri.contains("${File.separatorChar}.")
            || decodeUri.contains(".${File.separatorChar}")
            || decodeUri.startsWith('.')
            || decodeUri.endsWith('.')
            || INSECURE_URI.matches(decodeUri)
        ) {
            return ""
        }

        return File.separator + decodeUri
    }

    companion object {
        const val HTTP_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss zzz"
        const val HTTP_DATE_GET_TIMEZONE = "GMT+8:00"
        const val HTTP_CACHE_SECONDS = 60
        val ALLOW_FILE_NAME = Regex("[^-\\._]?[^<>&\\\"]*")
        val INSECURE_URI = Regex(".*[<>&\"].*")
    }
}