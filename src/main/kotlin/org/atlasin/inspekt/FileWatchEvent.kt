package org.atlasin.inspekt

import java.io.File

interface FileWatchEvent {

    val file : File

    val type : Type

    val repeated : Boolean

    enum class Type { CREATE, DELETE, MODIFY }
}