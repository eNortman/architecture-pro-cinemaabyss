package com.cinemaabyss.event

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthEndpoint
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.kafka.support.SendResult
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.CompletableFuture

@RestController
@RequestMapping("/api/events")
class EventsController(
    private val kafkaProducerService: KafkaProducerService,
    private val healthEndpoint: HealthEndpoint
) {

    @GetMapping("/health")
    fun health(): Map<String,Any>{
        val health  = healthEndpoint.health()
        val code = health.status.code
        val status = code.uppercase() == "UP"
        val details = health.status.description

        return mapOf(
            "status" to status,
            "code" to code,
            "description" to details
        )

    }

    @OptIn(ExperimentalSerializationApi::class)
    val json2 = Json {
        encodeDefaults = false // Исключает поля, имеющие значения по умолчанию (если они null)
        explicitNulls = false  // Явно указывает не включать null значения
        prettyPrint = false     // Для красивого форматирования вывода (необязательно)
    }

    fun applyRequest(futureReturn: CompletableFuture<SendResult<String, String>>, event:Any? = null) : CompletableFuture<ResponseEntity<EventResponse<Any>>> {

        return futureReturn.thenApply { result ->

            val metadata = result.recordMetadata
            val resp = EventResponse(
                status = "success",
                offset = metadata.offset(),
                partition = metadata.partition(),
                event = event
            )

            ResponseEntity.status(HttpStatus.CREATED).body(resp)

        } .exceptionally { ex ->

            val eresp = EventResponse<Any>("failure", ex.message ?: "")
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(eresp)

        }

    }

    @PostMapping("/movie")
    fun movies(@RequestBody movieEvent: MovieEvent) : CompletableFuture<ResponseEntity<EventResponse<MovieEvent>>> {
        val strJson = json2.encodeToString(MovieEvent.serializer(), movieEvent)
        val futureReturn = kafkaProducerService.sendMessage("movie-events", strJson)
        return applyRequest(futureReturn, movieEvent) as CompletableFuture<ResponseEntity<EventResponse<MovieEvent>>>

    }

    @Serializable
    data class MovieEvent (
        val movie_id: Int,
        val title: String,
        val action: String,
        var user_id : Int? = null,
        var rating : Double? = null,
        var genres : List<String>? = null,
        var description : String? = null
    )

    @PostMapping("/user")
    fun user(@RequestBody usersEvent: UsersEvent) : CompletableFuture<ResponseEntity<EventResponse<UsersEvent>>> {
        val strJson = json2.encodeToString(UsersEvent.serializer(), usersEvent)
        val futureReturn = kafkaProducerService.sendMessage("user-events", strJson)
        return applyRequest(futureReturn, usersEvent) as CompletableFuture<ResponseEntity<EventResponse<UsersEvent>>>
    }


    @Serializable
    data class UsersEvent (
        val user_id: Int,
        var username: String? = null,
        var email: String? = null,
        val action: String,
        val timestamp: String /* example: 2023-01-15T14:30:00Z */
    )

    @PostMapping("/payment")
    fun payment(@RequestBody paymentEvent: PaymentEvent) : CompletableFuture<ResponseEntity<EventResponse<PaymentEvent>>> {
        val strJson = json2.encodeToString(PaymentEvent.serializer(), paymentEvent)
        val futureReturn = kafkaProducerService.sendMessage("payment-events", strJson)
        return applyRequest(futureReturn, paymentEvent) as CompletableFuture<ResponseEntity<EventResponse<PaymentEvent>>>
    }

    @Serializable
    data class PaymentEvent (
        val payment_id: Int,
        val user_id: Int,
        val amount: Double,
        val status: String,
        val timestamp: String,
        var method_type: String? = null
    )
}
