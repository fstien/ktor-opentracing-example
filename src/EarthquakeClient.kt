package com.github.fstien

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.zopa.ktor.opentracing.OpenTracingClient
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.apache.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.*

class EarthquakeClient {
    val client = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = JacksonSerializer {}
        }

        install(OpenTracingClient)
    }

    private suspend fun getAll(): List<Earthquake> {
        val date = LocalDateTime.now().toLocalDate()

        val call: HttpStatement = client.get("https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&starttime=$date")

        val earthQuakeResponse: EarthQuakeResponse = call.execute {
            when(it.status) {
                HttpStatusCode.OK -> it.receive()
                else -> throw Exception("Error response received from earquakes.usgs ${it.status}")
            }
        }

        val earthquakes = earthQuakeResponse.features.map { it.properties.toEarthQuake() }

        return earthquakes
    }

    suspend fun getLatest(): Earthquake {
        val earthquakes = getAll()
        val latest = earthquakes.first()

        return latest
    }

    suspend fun getBiggest(): Earthquake {
        val earthquakes = getAll()
        val biggest = earthquakes.sortedBy { it.magnitude }.last()
        return biggest
    }

    suspend fun getBiggerThan(threshold: Double): List<Earthquake> {
        val earthquakes = getAll()
        val biggerThan = earthquakes.filter { it.magnitude > threshold }
        return biggerThan
    }
}

data class Earthquake(
    val location: String,
    val magnitude: Double,
    val timeGMT: String
)

fun EarthQuakeProperties.toEarthQuake(): Earthquake = Earthquake(
    location = this.place,
    magnitude = this.mag,
    timeGMT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(time))
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EarthQuakeResponse(
    val features: List<EarthQuakeFeature>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EarthQuakeFeature(
    val properties: EarthQuakeProperties
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EarthQuakeProperties(
    val mag: Double,
    val place: String,
    val time: Long
)
