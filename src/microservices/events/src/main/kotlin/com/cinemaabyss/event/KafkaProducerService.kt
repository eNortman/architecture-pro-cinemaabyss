package com.cinemaabyss.event

import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture


@Service
class KafkaProducerService(private val kafkaTemplate: KafkaTemplate<String, String>) {

    companion object {
        private val logger = LoggerFactory.getLogger(KafkaProducerService::class.java)
    }

    fun sendMessage(topicName: String, message: String) : CompletableFuture<SendResult<String, String>>  {
        logger.info("Попытка отправки сообщения: $message в топик: $topicName")
        return kafkaTemplate.send(topicName, message)
    }

}
