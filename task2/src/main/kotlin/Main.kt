import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.security.MessageDigest

suspend fun getFileSHA256(file: File): String = withContext(Dispatchers.IO) {
    val digest = MessageDigest.getInstance("SHA-256")
    val bytes = file.readBytes()
    digest.digest(bytes).joinToString("") { "%02x".format(it) }
}

fun main() {
    val root = File("./task2/src/main/resources")
    val timeout = 1000L;

    val hashes = runBlocking {
        withTimeoutOrNull(timeout) {
            val files = root.walkTopDown().filter { it.isFile && it.extension == "json" }.toList()

            files.map { file ->
                async {
                    file to getFileSHA256(file)
                }
            }.awaitAll()
        }
    }

    if (hashes == null) {
        println("Operation timed out after $timeout milliseconds.")
        return
    }

    val duplicates = hashes.groupBy { it.second }
        .filter { it.value.size > 1 }
        .mapValues { entry -> entry.value.map { it.first } }

    if (duplicates.isNotEmpty()) {
        println("Duplicate files found:")
        duplicates.forEach { (hash, files) ->
            println("Hash: $hash")
            files.forEach { file ->
                println(" - ${file.absolutePath}")
            }
        }
    } else {
        println("No duplicate files found.")
    }
}