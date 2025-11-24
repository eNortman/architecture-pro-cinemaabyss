package com.cinemaabyss.proxy

import jakarta.annotation.PostConstruct
import org.apache.camel.builder.RouteBuilder
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class MainRoute : RouteBuilder() {

    @Value("\${MONOLITH_URL}") lateinit var monolithUrl: String
    @Value("\${MOVIES_SERVICE_URL}") lateinit var moviesServiceUrl: String
    @Value("\${EVENTS_SERVICE_URL}") lateinit var eventsServiceUrl: String
    @Value("\${GRADUAL_MIGRATION}") lateinit var gradualMigration: String
    @Value("\${MOVIES_MIGRATION_PERCENT}") lateinit var moviesMigrationPrc: String

    private var distibutionRatiosMovie : Pair<Int, Int> = Pair(1, 0);

    private fun gcd(a: Int, b: Int): Int {
        return if (b == 0) a else gcd(b, a % b)
    }

    fun getDistribution(percent: Int): Pair<Int, Int> {
        require(percent in 0..100) { "Процент должен быть от 0 до 100" }

        return when (percent) {
            0 ->  Pair(1, 0)
            100 ->  Pair(0, 1)
            else -> {
                val gcd = gcd(percent, 100)
                val numerator = percent / gcd
                val denominator = 100 / gcd
                Pair(denominator - numerator, numerator)
            }
        }
    }

    @PostConstruct
    fun init(){

        try {
            if (gradualMigration.toBoolean()) {

                log.info("GRADUAL_MIGRATION enabled")

                val percent = moviesMigrationPrc.toInt()
                distibutionRatiosMovie = getDistribution(percent)

            }
        } catch (ex: Exception) {
            log.error(ex.message, ex)
        }

        log.info("MOVIES_MIGRATION distribution = $distibutionRatiosMovie")
    }

    override fun configure() {

        from("servlet:///*")
            .routeId("http_endpoint")
            .choice()
            .`when`(header("CamelHttpPath").startsWith("/api/movies"))
                .to("direct:movies")
            .`when`(header("CamelHttpPath").startsWith("/api/events"))
                .to("direct:events")
            .otherwise()
                .to("direct:default")

        from("direct:movies")
            .routeId("movies_proxy")
            .loadBalance().weighted(true, "${distibutionRatiosMovie.first},${distibutionRatiosMovie.second}")
                .to("$monolithUrl?bridgeEndpoint=true")
                .to("$moviesServiceUrl?bridgeEndpoint=true")
            .end()

        from("direct:events")
            .routeId("events_proxy")
            .to("$eventsServiceUrl?bridgeEndpoint=true")

        from("direct:default")
            .routeId("monolith_proxy")
            .to("$monolithUrl?bridgeEndpoint=true")

    }
}
