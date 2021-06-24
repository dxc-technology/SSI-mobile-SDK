package com.dxc.ssi.agent.transport

import co.touchlab.stately.isolate.IsolateState
import com.dxc.ssi.agent.model.messages.MessageEnvelop
import com.dxc.ssi.agent.utils.CoroutineHelper
import com.dxc.ssi.agent.utils.ObjectHolder
import com.dxc.utils.System
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

//Common

class AppSocket(url: String, incomingMessagesChannel: Channel<MessageEnvelop>) {
    private val ws = PlatformSocket(url)
    private val job: CompletableJob = Job()

    var socketError: Throwable? = null
        private set

    var currentState: State
        get() = isoCurrentState.access { it.obj }!!
        set(value) {
            isoCurrentState.access { it.obj = value }
        }
    private val isoCurrentState = IsolateState { ObjectHolder(State.CLOSED) }

    private val socketListener: PlatformSocketListener = object : PlatformSocketListener {
        override fun onOpen() {

            currentState = State.CONNECTED
            CoroutineHelper.waitForCompletion(CoroutineScope(Dispatchers.Default).async { socketOpenedChannel.send(Unit) })
            println("${System.getCurrentThread()} - Opened socket")
        }

        override fun onFailure(t: Throwable) {
            socketError = t
            currentState = State.CLOSED
            println("Socket failure: $t \n ${t.stackTraceToString()}")
        }

        override fun onMessage(msg: String) {
            println("${System.getCurrentThread()} - Received message: $msg")
            CoroutineHelper.waitForCompletion(CoroutineScope(Dispatchers.Default).async {
                incomingMessagesChannel.send(MessageEnvelop(msg))
            })

        }

        override fun onClosing(code: Int, reason: String) {
            currentState = State.CLOSING
            println("Closing socket: code = $code, reason = $reason")
        }

        override fun onClosed(code: Int, reason: String) {
            currentState = State.CLOSED

            println("Closed socket: code = $code, reason = $reason")
            job.complete()
        }
    }

    val socketOpenedChannel: Channel<Unit> = Channel()

    suspend fun connect() {
        if (currentState != State.CLOSED) {
            throw IllegalStateException("The socket is available.")
        }
        socketError = null
        currentState = State.CONNECTING


        ws.openSocket(socketListener)
        println("Thread = ${System.getCurrentThread()} awaiting while websocket is opened")

        socketOpenedChannel.receive()
        println("After socketListener.onOpen")

        if (currentState != State.CONNECTED)
            throw throw IllegalStateException("Could not be opened")

    }

    //TODO: ensure to disconnect properly otherwise we will have leaking threads
    fun disconnect() {
        if (currentState != State.CLOSED) {
            currentState = State.CLOSING
            ws.closeSocket(1000, "The user has closed the connection.")
            job.complete()
        }
    }

    fun send(msg: String) {
        if (currentState != State.CONNECTED) throw IllegalStateException("The connection is lost.")
        println("Sending message to websocket")
        ws.sendMessage(msg)
        println("Sent message to websocket")
    }

    enum class State {
        CONNECTING,
        CONNECTED,
        CLOSING,
        CLOSED
    }
}