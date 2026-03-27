package ai.openclaw.app.cloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * HTTP client for the AppClaw backend provisioning API.
 *
 * The backend is responsible for:
 *  1. Allocating an isolated Docker container running the OpenClaw daemon.
 *  2. Returning a WebSocket URL + bootstrap token the phone can use to connect.
 *  3. Sleeping containers that are idle; waking them when the phone reconnects.
 *
 * All methods suspend and run on [Dispatchers.IO].
 *
 * @param baseUrl     Root URL of the AppClaw backend, e.g.
 *                    `https://api.appclaw.ai`. No trailing slash.
 * @param deviceToken Opaque device-bound token obtained during onboarding
 *                    (stored in [ai.openclaw.app.SecurePrefs]).
 */
class CloudProvisioningClient(
    private val baseUrl: String,
    private val deviceToken: String,
    private val httpClient: OkHttpClient = defaultHttpClient(),
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    /**
     * Provisions a new OpenClaw container for the current device, or returns
     * info about an existing one.
     *
     * Corresponds to `POST /v1/containers/spawn`.
     *
     * @throws CloudProvisioningException on HTTP or network errors.
     */
    suspend fun spawn(): SpawnResponse = withContext(Dispatchers.IO) {
        val request =
            Request.Builder()
                .url("$baseUrl/v1/containers/spawn")
                .post("{}".toRequestBody(jsonMediaType))
                .header("Authorization", "Bearer $deviceToken")
                .build()
        execute(request) { body ->
            json.decodeFromString<SpawnResponse>(body)
        }
    }

    /**
     * Fetches the current status of the container identified by [containerId].
     *
     * Corresponds to `GET /v1/containers/{id}/status`.
     *
     * @throws CloudProvisioningException on HTTP or network errors.
     */
    suspend fun status(containerId: String): ContainerStatus = withContext(Dispatchers.IO) {
        val request =
            Request.Builder()
                .url("$baseUrl/v1/containers/$containerId/status")
                .get()
                .header("Authorization", "Bearer $deviceToken")
                .build()
        execute(request) { body ->
            json.decodeFromString<ContainerStatus>(body)
        }
    }

    /**
     * Wakes a sleeping container.
     *
     * Corresponds to `POST /v1/containers/{id}/wake`.
     *
     * @throws CloudProvisioningException on HTTP or network errors.
     */
    suspend fun wake(containerId: String): ContainerStatus = withContext(Dispatchers.IO) {
        val request =
            Request.Builder()
                .url("$baseUrl/v1/containers/$containerId/wake")
                .post("{}".toRequestBody(jsonMediaType))
                .header("Authorization", "Bearer $deviceToken")
                .build()
        execute(request) { body ->
            json.decodeFromString<ContainerStatus>(body)
        }
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private fun <T> execute(request: Request, parse: (String) -> T): T {
        val response =
            try {
                httpClient.newCall(request).execute()
            } catch (e: Exception) {
                throw CloudProvisioningException(
                    "Network error while connecting to provisioning API: ${e.message ?: "connection failed"}",
                    e,
                )
            }
        return response.use { resp ->
            val body = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                throw CloudProvisioningException("HTTP ${resp.code}: $body")
            }
            try {
                parse(body)
            } catch (e: Exception) {
                throw CloudProvisioningException(
                    "Failed to parse JSON response from provisioning API: ${e.message ?: "unknown error"}",
                    e,
                )
            }
        }
    }

    companion object {
        private fun defaultHttpClient(): OkHttpClient =
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()

        /** Base URL used for the production AppClaw backend. */
        const val PRODUCTION_BASE_URL = "https://api.appclaw.ai"
    }
}

/** Thrown when a provisioning API call fails. */
class CloudProvisioningException(message: String, cause: Throwable? = null) : Exception(message, cause)
