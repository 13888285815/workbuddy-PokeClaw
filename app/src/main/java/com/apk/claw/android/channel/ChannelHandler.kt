package com.apk.claw.android.channel

/**
 * 各消息通道的统一接口。
 * 每个通道（钉钉、飞书、QQ、Discord、Telegram）各自实现此接口。
 */
interface ChannelHandler {

    val channel: Channel

    /** 当前通道是否已连接/正在运行 */
    fun isConnected(): Boolean

    fun init()

    fun disconnect()

    fun reinitFromStorage()

    fun sendMessage(content: String, messageID: String)

    fun sendImage(imageBytes: ByteArray, messageID: String)

    fun sendFile(file: java.io.File, messageID: String)
}
