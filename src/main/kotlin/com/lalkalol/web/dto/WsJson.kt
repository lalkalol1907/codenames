package com.lalkalol.web.dto

import kotlinx.serialization.json.Json

val wsJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
