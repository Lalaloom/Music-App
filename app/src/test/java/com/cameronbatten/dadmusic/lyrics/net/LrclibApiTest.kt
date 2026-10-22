package com.cameronbatten.dadmusic.lyrics.net

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalSerializationApi::class)
class LrclibApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: LrclibApi

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        val client = OkHttpClient.Builder()
            // Short read timeout so the timeout test runs quickly.
            .readTimeout(500, TimeUnit.MILLISECONDS)
            .build()
        val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        api = retrofit.create(LrclibApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun successfulResponse_isDeserializedWithSyncedLyrics() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                    "id": 999,
                    "trackName": "Hotel California",
                    "artistName": "Eagles",
                    "albumName": "Hotel California",
                    "duration": 391.0,
                    "instrumental": false,
                    "plainLyrics": "On a dark desert highway...",
                    "syncedLyrics": "[00:12.00]On a dark desert highway"
                }
                """.trimIndent(),
            ),
        )

        val response = api.get("Hotel California", "Eagles", "Hotel California", 391.0)

        assertEquals(999L, response.id)
        assertEquals("Hotel California", response.trackName)
        assertEquals("Eagles", response.artistName)
        assertEquals("[00:12.00]On a dark desert highway", response.syncedLyrics)
    }

    @Test
    fun successfulResponse_handlesMissingOptionalFields() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                { "id": 1, "trackName": "Unknown" }
                """.trimIndent(),
            ),
        )

        val response = api.get("Unknown", null, null, null)

        assertEquals(1L, response.id)
        assertEquals("Unknown", response.trackName)
        assertEquals(null, response.artistName)
        assertEquals(null, response.plainLyrics)
        assertEquals(null, response.syncedLyrics)
    }

    @Test
    fun notFound_throwsHttpException404() {
        server.enqueue(MockResponse().setResponseCode(404))

        try {
            runBlocking { api.get("Nope", "Nobody", null, null) }
            error("Expected HttpException")
        } catch (e: HttpException) {
            assertEquals(404, e.code())
        }
    }

    @Test
    fun malformedJson_throwsSerializationException() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("{ this is not json"),
        )

        try {
            runBlocking { api.get("x", null, null, null) }
            error("Expected SerializationException")
        } catch (e: SerializationException) {
            // expected
        }
    }

    @Test
    fun readTimeout_throwsIOException() {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody("""{"id":1,"trackName":"x"}""")
                .setBodyDelay(2, TimeUnit.SECONDS),
        )

        try {
            runBlocking { api.get("x", null, null, null) }
            error("Expected IOException")
        } catch (e: IOException) {
            // expected — read timeout
            assertTrue(
                "SerializationException should not be the thrown type",
                e !is SerializationException,
            )
        }
    }

    @Test
    fun requestUsesCorrectPathAndQueryParams() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(200)
                .setBody("""{"id":1,"trackName":"Hotel California"}"""),
        )

        api.get("Hotel California", "Eagles", "Hotel California", 391.0)

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertTrue("path should start with /api/get: ${recorded.path}", recorded.path!!.startsWith("/api/get"))
        val qs = recorded.path!!.substringAfter('?', "")
        assertTrue(qs.contains("track_name=Hotel%20California"))
        assertTrue(qs.contains("artist_name=Eagles"))
        assertTrue(qs.contains("duration=391.0"))
    }
}
