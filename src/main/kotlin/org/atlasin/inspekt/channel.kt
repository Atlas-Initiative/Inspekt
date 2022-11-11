package org.atlasin.inspekt

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import java.io.File
import java.time.Duration
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun CoroutineScope.watchFile(
    file : File,
    context : CoroutineContext = EmptyCoroutineContext,
    capacity : Int = Channel.RENDEZVOUS,
    timeout : Duration = Duration.ofMillis(500),
    watchTree : Boolean = file.isDirectory,
    config : (FileWatcher.Config.() -> Unit)? = null
) : ReceiveChannel<FileWatchEvent> {
    require(file.isDirectory) { "file does not represent a directory!"}
    require(file.exists()) { "file must exist!" }
    require(!timeout.isZero && !timeout.isNegative) { "timeout must be a non-zero positive integer!" }

    val watcher = file.watcher(watchTree, config)

    return produce(context = context, capacity = capacity) {
        invokeOnClose { watcher.close() }

        while(!isClosedForSend && watcher.isOpen) {
            for(event in watcher.queue()) {
                send(event)
            }

            delay(timeout.toMillis())
        }
    }
}