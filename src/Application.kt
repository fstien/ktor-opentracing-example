package com.github.fstien

import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    val earthquakeClient = EarthquakeClient()

    routing {
        route("/earthquake") {
            get("/latest") {
                val latestEarthQuake = earthquakeClient.getLatest()
                call.respond(HttpStatusCode.OK, latestEarthQuake)
            }

            get("/biggest") {
                val biggestEarthquake = earthquakeClient.getBiggest()
                call.respond(HttpStatusCode.OK, biggestEarthquake)
            }

            get("/biggerthan/{threshold}") {
                val threshold: Double? = call.parameters["threshold"]?.toDouble()
                if (threshold == null) {
                    call.respond(HttpStatusCode.BadRequest, "Invalid threshold: ${call.parameters["threshold"]}")
                    return@get
                }

                val earthquakes = earthquakeClient.getBiggerThan(threshold)

                if (earthquakes.isEmpty()) {
                    call.respond(HttpStatusCode.NotFound, "No earthquakes found above magnitude $threshold.")
                    return@get
                }

                call.respond(HttpStatusCode.OK, earthquakes)
            }
        }
    }
}

