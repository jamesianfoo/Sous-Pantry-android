package com.souspantry.app.services

import retrofit2.http.Body
import retrofit2.http.POST

/**
 * PARKED — not wired into the app.
 *
 * ILMU (console.ilmu.ai) is an OpenAI-compatible gateway. Model calls now go
 * through the Cloudflare Worker proxy instead, which routes by X-SP-Region
 * (MY → ILMU local model), so nothing here is bound in NetworkModule and no
 * ILMU key ships in the APK. Kept for a possible future direct-ILMU path on
 * Android/iOS.
 *
 * To reactivate: add a @Named("ilmu") OkHttp/Retrofit pair in NetworkModule
 * (baseUrl "https://api.ilmu.ai/v1/", header Authorization: Bearer <key>),
 * provide IlmuService, and call chatCompletion with the same prompt contract
 * the Worker path uses. Do NOT embed the key in a release build.
 */
interface IlmuService {
    @POST("chat/completions")
    suspend fun chatCompletion(@Body body: IlmuChatRequest): IlmuChatResponse
}

data class IlmuChatRequest(
    val model      : String = "ilmu-v3.1",
    val messages   : List<IlmuMessage>,
    val temperature: Double = 0.7,
)

data class IlmuMessage(
    val role   : String, // "system" | "user" | "assistant"
    val content: String,
)

data class IlmuChatResponse(
    val choices: List<IlmuChoice> = emptyList(),
)

data class IlmuChoice(
    val message: IlmuMessage,
)
