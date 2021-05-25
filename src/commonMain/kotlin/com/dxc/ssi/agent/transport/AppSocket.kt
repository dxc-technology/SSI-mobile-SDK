package com.dxc.ssi.agent.transport

import com.dxc.ssi.agent.model.messages.MessageEnvelop
import co.touchlab.stately.collections.IsoMutableList
import kotlinx.coroutines.*

//Common

class AppSocket(url: String, incomingMessagesQueue: IsoMutableList<MessageEnvelop>) {
    private val ws = PlatformSocket(url)
    private val job: CompletableJob = Job()

    private val socketListenerAdapter = SocketListenerAdapter()


    // private val isolatedWs = IsolateState {}

    var socketError: Throwable? = null
        private set
    var currentState: State = State.CLOSED
        private set(value) {
            field = value
            stateListener?.invoke(value)
        }
    var stateListener: ((State) -> Unit)? = null
        set(value) {
            field = value
            value?.invoke(currentState)
        }


    //TODO: rework this function to be more robust and more suited for different platforms
    suspend fun connect() {
        if (currentState != State.CLOSED) {
            throw IllegalStateException("The socket is available.")
        }
        socketError = null
        currentState = State.CONNECTING


        //TODO: refactor this to have cleaner code, introduce single listen fun combining the funs above
        //TODO: introduce liseners for other types of events
        listenForMessages()
        listenForFailures()

        ws.openSocket(socketListenerAdapter)
        println("awaiting while websocket is opened")

        socketListenerAdapter.socketOpenedChannel.receive()
        socketListener.onOpen()
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

    private suspend fun listenForMessages() {
        println("IN listenForMessages function")

        //TODO: check that is is working as expected. I presume that once job is completed on socket disconnect them this coroutine willbe cancelled
        CoroutineScope(Dispatchers.Default + job).async {
            val receivedMessage = socketListenerAdapter.socketReceivedMessageChannel.receive()
            socketListener.onMessage(receivedMessage)

            listenForMessages()
        }

    }

    private suspend fun listenForClosure() {
        println("IN listenForClosure function")

        //TODO: check that is is working as expected. I presume that once job is completed on socket disconnect them this coroutine willbe cancelled
        CoroutineScope(Dispatchers.Default + job).async {
            val closureDetails = socketListenerAdapter.socketClosedChannel.receive()
            socketListener.onClosed(closureDetails.code, closureDetails.reason)

            listenForMessages()
        }

    }


    private suspend fun listenForFailures() {
        println("IN listenForFailures function")
        CoroutineScope(Dispatchers.Default + job).async {
            val receivedThrowable = socketListenerAdapter.socketFailureChannel.receive()
            socketListener.onFailure(receivedThrowable)

            listenForFailures()
        }

    }

    private val socketListener: PlatformSocketListener = object : PlatformSocketListener {
        override fun onOpen() {
            println("Opened socket")


            currentState = State.CONNECTED
        }

        override fun onFailure(t: Throwable) {
            socketError = t
            currentState = State.CLOSED
            println("Socket failure: $t \n ${t.stackTraceToString()}")
        }

        override fun onMessage(msg: String) {
            println("Received message: $msg")
            incomingMessagesQueue.add(MessageEnvelop(msg))

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

    enum class State {
        CONNECTING,
        CONNECTED,
        CLOSING,
        CLOSED
    }
}