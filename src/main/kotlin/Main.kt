package org.example

import kotlinx.coroutines.*
import java.io.File
import java.security.MessageDigest

fun main() = runBlocking {

    val directoryPath = "/Users/shadow/Documents/Test"
    val timeoutSeconds = 10L

    val result = withTimeoutOrNull(timeoutSeconds * 1000) {

        val jsonFiles = File(directoryPath)
            .walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .toList()

        val hashes = jsonFiles.map { file ->
            async(Dispatchers.IO) {
                file to sha256(file)
            }
        }.awaitAll()

        hashes.groupBy({ it.second }, { it.first })
            .filter { it.value.size > 1 }
    }

    if (result == null) {
        println("Поиск прерван по таймауту")
    } else {
        if (result.isEmpty()) {
            println("Дубликаты не найдены")
        } else {
            result.forEach { (hash, files) ->
                println("\nSHA-256: $hash")
                files.forEach { println(it.absolutePath) }
            }
        }
    }
}

suspend fun sha256(file: File): String = withContext(Dispatchers.IO) {
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(file.readBytes())
    hash.joinToString("") { "%02x".format(it) }
}


