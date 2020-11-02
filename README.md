## Earthquake API 

Example Ktor application instrumented with OpenTracing using [ktor-opentracing](https://github.com/zopaUK/ktor-opentracing). 

Retrieves data about earthquakes that happened today using an [API from the U.S. Geological Survey](https://earthquake.usgs.gov/fdsnws/event/1/). 

### Running

1. Start an [all-in-one Jaeger backend](https://www.jaegertracing.io/docs/1.20/getting-started/) with in-memory storage.

        docker run -d --name jaeger \          
             -e COLLECTOR_ZIPKIN_HTTP_PORT=9411 \
             -p 5775:5775/udp \
             -p 6831:6831/udp \
             -p 6832:6832/udp \
             -p 5778:5778 \
             -p 16686:16686 \
             -p 14268:14268 \
             -p 14250:14250 \
             -p 9411:9411 \
             jaegertracing/all-in-one:1.20
             
2. Start the application.

        ./gradlew run
        
3. Send some requests. Other routes available are `/earthquake/biggest` and `/earthquake/biggerthan/5` (where 5 is a parameter).

        curl http://localhost:8080/earthquake/latest
        {
          "location" : "21 km SSE of Karluk, Alaska",
          "magnitude" : 1.9,
          "timeGMT" : "2020-11-02 09:46:39"
        }  
        
4. See traces in Jaeger.

    http://localhost:16686/

5. Stop the Jaeger docker container.

        docker ps
        docker stop <containerId>

### Steps

1. Import [ktor-opentracing](https://github.com/zopaUK/ktor-opentracing) and the [Java Jaeger client](https://github.com/jaegertracing/jaeger-client-java). 

        implementation "io.jaegertracing:jaeger-client:1.3.2"
        implementation "com.zopa:ktor-opentracing:0.1.1"

2. Instantiate a tracer and register it in [GlobalTracer](https://opentracing.io/guides/java/tracers/).

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

3. Install the `OpenTracingServer` feature into the application call pipeline. 
        install(OpenTracingServer)
        
4. Install the `OpenTracingClient` feature onto the http client. 

        install(OpenTracingClient)
5. Instrument method calls using the `span` helper function.

        = span("EarthquakeClient.getBiggest()") {

    From the lamdba expression passed to `span`, you can add tags or logs to the span by calling `setTag()` or `log()`.