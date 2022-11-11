package org.atlasin.inspekt

import java.io.File
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import java.util.LinkedList
import java.util.concurrent.ConcurrentLinkedQueue

interface FileWatcher : AutoCloseable, Sequence<FileWatchEvent> {

    val isOpen : Boolean

    fun poll() : FileWatchEvent?

    fun queue() : List<FileWatchEvent>

    fun flush()

    class Config {
        var watchCreations = true
        var watchModifications = true
        var watchDeletions = true
    }
}

internal class FileWatcherImpl(private val key : WatchKey) : FileWatcher, AutoCloseable, Sequence<FileWatchEvent> {
    private val queue = ConcurrentLinkedQueue<WatchEvent<Path>>()

    override val isOpen: Boolean = key.isValid

    override fun poll(): FileWatchEvent? {
        check(key.isValid) { "FileWatcher has been closed!" }

        queue.poll()?.let { return it.toEventImpl() }
        val events = key.pollEvents()


        when(events.size) {
            0 -> return null
            1 -> {
                val event = events[0]
                val context = event.context()
                check(context is Path) { "Invalid context: $context"}
                return (event as WatchEvent<Path>).toEventImpl()
            }

            else -> {
                for(event in events) {
                    val context = event.context()
                    check(context is Path) { "Invalid context: $context"}
                    this.queue += event as WatchEvent<Path>
                }
                return queue.poll()?.toEventImpl()
            }
        }
    }

    override fun queue(): List<FileWatchEvent> {
        val queue = LinkedList<FileWatchEvent>()
        while(true) queue += poll() ?: break
        return queue
    }

    override fun flush() {
        queue.clear()
        key.pollEvents()
    }

    override fun close() {
        if(key.isValid) {
            flush()
            key.cancel()
        }
    }

    override fun iterator(): Iterator<FileWatchEvent> = object : Iterator<FileWatchEvent> {
        var next : FileWatchEvent? = null

        override fun hasNext(): Boolean {
            if(!isOpen) return false
            this.next = poll()
            return this.next != null
        }

        override fun next(): FileWatchEvent = this.next ?: throw NoSuchElementException("no next element!")
    }
}

internal data class FileWatchEventImpl(
    override val file: File,
    override val type: FileWatchEvent.Type,
    override val repeated: Boolean
) : FileWatchEvent

private fun WatchEvent<Path>.toEventImpl() : FileWatchEventImpl {
    return FileWatchEventImpl(
        file = context().toFile(),
        type = when(kind()) {
            StandardWatchEventKinds.ENTRY_DELETE -> FileWatchEvent.Type.CREATE
            StandardWatchEventKinds.ENTRY_DELETE -> FileWatchEvent.Type.DELETE
            StandardWatchEventKinds.ENTRY_MODIFY -> FileWatchEvent.Type.MODIFY
            else -> error("Encountered an unexpected kind")
        },
        repeated = count() > 1
    )
}

private fun verifyContextIsPath(event : WatchEvent<*>) {
    val context = event.context()
    check(context is Path) {"Invalid event context : $context"}
}