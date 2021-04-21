package com.dxc.ssi.agent.transport


import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket

internal actual class PlatformSocket actual constructor(url: String) {
    private val socketEndpoint = url
    private var webSocket: WebSocket? = null
    actual fun openSocket(socketListenerAdapter: SocketListenerAdapter) {
        val socketRequest = Request.Builder().url(socketEndpoint).build()
        val webClient = OkHttpClient().newBuilder().build()
        webSocket = webClient.newWebSocket(
            socketRequest,
            object : okhttp3.WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) = socketListenerAdapter.onOpened()
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) =
                    socketListenerAdapter.onFailure(t)

                override fun onMessage(webSocket: WebSocket, text: String) =
                    socketListenerAdapter.onMessageReceived(text)

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) =
                    socketListenerAdapter.onClosed(SocketClosureDetails(code, reason))

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) =
                    socketListenerAdapter.onClosed(SocketClosureDetails(code, reason))
            }
        )


    }

    actual fun closeSocket(code: Int, reason: String) {
        webSocket?.close(code, reason)
        webSocket = null
    }

    actual fun sendMessage(msg: String) {
        webSocket?.send(msg)
    }
}