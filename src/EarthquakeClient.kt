package com.github.fstien

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.zopa.ktor.opentracing.OpenTracingClient
import com.zopa.ktor.opentracing.span
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

    private suspend fun getAll(): List<Earthquake> = span("EarthquakeClient.getAll()") {
        val date = LocalDateTime.now().toLocalDate()

        val call: HttpStatement = client.get("https://earthquake.usgs.gov/fdsnws/event/1/query?format=geojson&starttime=$date")

        val earthquakeResponse: EarthquakeResponse = call.execute {
            when(it.status) {
                HttpStatusCode.OK -> it.receive()
                else -> throw Exception("Error response received from earquakes.usgs ${it.status}")
            }
        }

        val earthquakes = earthquakeResponse.features.map { it.properties.toEarthQuake() }

        setTag("count", earthquakes.size)

        return earthquakes
    }

    suspend fun getLatest(): Earthquake = span("EarthquakeClient.getLatest()") {
        val earthquakes = getAll()
        val latest = earthquakes.first()
        setTag("location", latest.location)
        setTag("magnitude", latest.magnitude)
        setTag("timeGMT", latest.timeGMT)

        return latest
    }

    suspend fun getBiggest(): Earthquake = span("EarthquakeClient.getBiggest()") {
        val earthquakes = getAll()
        val biggest = earthquakes.sortedBy { it.magnitude }.last()
        setTag("location", biggest.location)
        setTag("magnitude", biggest.magnitude)
        setTag("timeGMT", biggest.timeGMT)

        return biggest
    }

    suspend fun getBiggerThan(threshold: Double): List<Earthquake> = span("EarthquakeClient.getBiggerThan()") {
        setTag("threshold", threshold)
        val earthquakes = getAll()
        val biggerThan = earthquakes.filter { it.magnitude > threshold }
        setTag("count", biggerThan.size)

        return biggerThan
    }
}

data class Earthquake(
    val location: String,
    val magnitude: Double,
    val timeGMT: String
)

fun EarthquakeProperties.toEarthQuake(): Earthquake = Earthquake(
    location = this.place,
    magnitude = this.mag,
    timeGMT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date(time))
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EarthquakeResponse(
    val features: List<EarthQuakeFeature>
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EarthQuakeFeature(
    val properties: EarthquakeProperties
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EarthquakeProperties(
    val mag: Double,
    val place: String,
    val time: Long
)
