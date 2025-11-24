package com.cinemaabyss.event

import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class KafkaConsumerService {

    @KafkaListener(topics = ["movie-events"])
    fun listenMovieEv(message: String) {
        println("Получено уведомление movie: $message")
    }

    @KafkaListener(topics = ["user-events"])
    fun listenUserEv(message: String) {
        println("Получено уведомление user: $message")
    }

    @KafkaListener(topics = ["payment-events"])
    fun listenPaymentEv(message: String) {
        println("Получено уведомление payments: $message")
    }

}
