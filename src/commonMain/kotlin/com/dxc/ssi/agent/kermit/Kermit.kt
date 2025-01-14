package com.dxc.ssi.agent.kermit

//The broader explanation of why I have introduced Kermit code instead of using the library is that we need to have a library available on Kotlin Multiplatform world.
// We can use the android library in such a way (there is a touch lib), and for IOS, we need a cocoapod that is not available.
// Right now library supports pod source code or the ability to use external-cocoapods plugin, which can't be applied in Kotlin Multiplatform.
// We need a library that will reside in one place and be easier to maintain, however, it's worth to reintroduce it when both parts will be working in Kotlin Multiplatform.
// TODO Re-introduce *Kermit* as a common library for both platform IOS and Android
class Kermit(
    private val loggerList: List<Logger> = listOf(
        LogcatLogger()
    ), private val defaultTag: String = "Logger"
) {

    constructor(vararg loggers: Logger) : this(loggers.asList())
    constructor(logger: Logger) : this(listOf(logger))

    fun withTag(defaultTag: String): Kermit {
        return Kermit(this.loggerList, defaultTag)
    }

    fun v(tag: String = defaultTag, throwable: Throwable? = null, message: () -> String) {
        processLog(Severity.Verbose, tag, throwable, message, Logger::v)
    }

    fun v(withMessage: () -> String) {
        v(message = withMessage)
    }

    fun v(withThrowable: Throwable, message: () -> String) {
        v(throwable = withThrowable, message = message)
    }

    fun v(withTag: String, message: () -> String) {
        v(tag = withTag, message = message)
    }

    fun d(tag: String = defaultTag, throwable: Throwable? = null, message: () -> String) {
        processLog(Severity.Debug, tag, throwable, message, Logger::d)
    }

    fun d(withMessage: () -> String) {
        d(message = withMessage)
    }

    fun d(withThrowable: Throwable?, message: () -> String) {
        d(throwable = withThrowable, message = message)
    }

    fun d(withTag: String, message: () -> String) {
        d(tag = withTag, message = message)
    }

    fun i(tag: String = defaultTag, throwable: Throwable? = null, message: () -> String) {
        processLog(Severity.Info, tag, throwable, message, Logger::i)
    }

    fun i(withMessage: () -> String) {
        i(message = withMessage)
    }

    fun i(withThrowable: Throwable?, message: () -> String) {
        i(throwable = withThrowable, message = message)
    }

    fun i(withTag: String, message: () -> String) {
        i(tag = withTag, message = message)
    }

    fun w(tag: String = defaultTag, throwable: Throwable? = null, message: () -> String) {
        processLog(Severity.Warn, tag, throwable, message, Logger::w)
    }

    fun w(withMessage: () -> String) {
        w(message = withMessage)
    }

    fun w(withThrowable: Throwable?, message: () -> String) {
        w(throwable = withThrowable, message = message)
    }

    fun w(withTag: String, message: () -> String) {
        w(tag = withTag, message = message)
    }

    fun e(tag: String = defaultTag, throwable: Throwable? = null, message: () -> String) {
        processLog(Severity.Error, tag, throwable, message, Logger::e)
    }

    fun e(withMessage: () -> String) {
        e(message = withMessage)
    }

    fun e(withThrowable: Throwable?, message: () -> String) {
        e(throwable = withThrowable, message = message)
    }

    fun e(withTag: String, message: () -> String) {
        e(tag = withTag, message = message)
    }

    fun wtf(tag: String = defaultTag, throwable: Throwable? = null, message: () -> String) {
        processLog(Severity.Assert, tag, throwable, message, Logger::wtf)
    }

    fun wtf(withMessage: () -> String) {
        wtf(message = withMessage)
    }

    fun wtf(withThrowable: Throwable?, message: () -> String) {
        wtf(throwable = withThrowable, message = message)
    }

    fun wtf(withTag: String, message: () -> String) {
        wtf(tag = withTag, message = message)
    }
    fun log(
        severity: Severity,
        tag: String = defaultTag,
        throwable: Throwable?,
        message: () -> String
    ) {
        processLog(
            severity,
            tag,
            throwable,
            message
        ) { processedMessage, processedTag, processedThrowable ->
            log(severity, processedMessage, processedTag, processedThrowable)
        }
    }

    private inline fun processLog(
        severity: Severity,
        tag: String,
        throwable: Throwable?,
        crossinline message: () -> String,
        loggingCall: Logger.(message: String, tag: String, throwable: Throwable?) -> Unit
    ) {
        val processedMessage: String by lazy { message() }
        loggerList.forEach {
            if (it.isLoggable(severity)) {
                it.loggingCall(processedMessage, tag, throwable)
            }
        }
    }
}