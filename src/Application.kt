package com.github.fstien

import com.fasterxml.jackson.databind.SerializationFeature
import com.zopa.ktor.opentracing.OpenTracingServer
import com.zopa.ktor.opentracing.ThreadContextElementScopeManager
import io.jaegertracing.Configuration
import io.jaegertracing.internal.samplers.ConstSampler
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.opentracing.util.GlobalTracer

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    val tracer = Configuration("tracing-example")
        .withSampler(Configuration.SamplerConfiguration.fromEnv()
            .withType(ConstSampler.TYPE)
            .withParam(1))
        .withReporter(Configuration.ReporterConfiguration.fromEnv()
            .withLogSpans(true)
            .withSender(
                Configuration.SenderConfiguration()
                    .withAgentHost("localhost")
                    .withAgentPort(6831))).tracerBuilder
        .withScopeManager(ThreadContextElementScopeManager())
        .build()

    GlobalTracer.registerIfAbsent(tracer)

    install(OpenTracingServer)

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

