import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.system.measureTimeMillis

@Serializable
data class User(val id: Int, val name: String)

@Serializable
data class Item(val product: String, val qty: Int, val revenue: Int)

@Serializable
data class Sales(val today: String, val items: List<Item>)

@Serializable
data class Weather(val city: String, val temp: Int, val condition: String)

suspend fun loadUsers(): List<String>? {
    delay(1000);
    try {
        val filePath = object {}.javaClass.getResource("users.json")?.path
        if (filePath == null) {
            println("Error loading users: File not found")
            return null
        }

        val users = Json.decodeFromString<List<User>>(File(filePath).readText())
        return users.map { it.name }
    }
    catch (e: Exception) {
        println("Error loading users: ${e.message}")
        return null
    }
}

suspend fun loadSales(): Map<String, Int>? {
    delay(1200);
    try {
        val filePath = object {}.javaClass.getResource("sales.json")?.path
        if (filePath == null) {
            println("Error loading sales: File not found")
            return null
        }

        val sales = Json.decodeFromString<Sales>(File(filePath).readText())
        return sales.items.associate { it.product to it.qty }
    }
    catch (e: Exception) {
        println("Error loading sales: ${e.message}")
        return null
    }
}

suspend fun loadWeather(): List<String>? {
    delay(2500)
    try {
        val filePath = object {}.javaClass.getResource("weather.json")?.path
        if (filePath == null) {
            println("Error loading weather: File not found")
            return null
        }

        val weathers = Json.decodeFromString<List<Weather>>(File(filePath).readText())
        return weathers.map { "${it.city}: ${it.temp}" }
    }
    catch (e: Exception) {
        println("Error loading weather: ${e.message}")
        return null
    }
}

fun main() {
    val totalTime = measureTimeMillis {
        runBlocking {
            val usersDef: Deferred<List<String>?> = async { loadUsers() }
            val salesDef: Deferred<Map<String, Int>?> = async { loadSales() }
            val weatherDef: Deferred<List<String>?> = async { loadWeather() }

            val users = usersDef.await()
            val sales = salesDef.await()
            val weather = weatherDef.await()

            println("Users: $users")
            println("Sales: $sales")
            println("Weather: $weather")
        }
    }

    println("Total time: ${totalTime}ms")
}