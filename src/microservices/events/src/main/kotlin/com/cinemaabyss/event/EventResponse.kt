package com.cinemaabyss.event

import kotlinx.serialization.Serializable

@Serializable
data class EventResponse<T> (
    val status: String,
    var reason: String? = null,
    var partition: Int? = null,
    var offset: Long? = null,
    var event: T? = null
)