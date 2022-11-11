package org.atlasin.inspekt

import com.sun.nio.file.ExtendedWatchEventModifier
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.StandardWatchEventKinds.*

internal val fileSystemWatchService by lazy { FileSystems.getDefault().newWatchService() }
internal fun createFileWatcher(file : File, watchTree : Boolean, config : FileWatcher.Config) : FileWatcher {
    val eventKinds = listOfNotNull(
        ENTRY_CREATE.takeIf { config.watchCreations },
        ENTRY_MODIFY.takeIf { config.watchModifications },
        ENTRY_DELETE.takeIf { config.watchDeletions }
    )

    require(eventKinds.isNotEmpty()) { "FileWatcher must match some form of file-system event!"}

    val mods = if(!watchTree) emptyArray() else {
        require(file.isDirectory) {"watchTree may not be true if file is not a directory!"}
        arrayOf(ExtendedWatchEventModifier.FILE_TREE)
    }

    val key = file.toPath().register(fileSystemWatchService, eventKinds.toTypedArray(), *mods)
    return FileWatcherImpl(key)
}


fun File.watcher(watchTree: Boolean = isDirectory, block: (FileWatcher.Config.() -> Unit)? = null) : FileWatcher {
    val config = FileWatcher.Config()
    block?.let { config.apply(it) }
    return createFileWatcher(this, watchTree, config)
}
