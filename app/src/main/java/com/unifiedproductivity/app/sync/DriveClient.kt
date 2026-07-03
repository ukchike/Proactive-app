package com.unifiedproductivity.app.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal Google Drive v3 client scoped to the app's private `appDataFolder`. Talks
 * to the REST API directly with a bearer token, avoiding the heavyweight
 * google-api-services-drive dependency (which needs packaging workarounds on Android).
 */
class DriveClient {

    /** Map of fileName -> fileId currently stored in the appDataFolder. */
    suspend fun listFiles(token: String): Map<String, String> = withContext(Dispatchers.IO) {
        val url = "https://www.googleapis.com/drive/v3/files" +
            "?spaces=appDataFolder&fields=files(id,name)&pageSize=1000"
        val response = request("GET", url, token)
        val files = JSONObject(response).optJSONArray("files") ?: return@withContext emptyMap()
        buildMap {
            for (i in 0 until files.length()) {
                val f = files.getJSONObject(i)
                put(f.getString("name"), f.getString("id"))
            }
        }
    }

    suspend fun download(token: String, fileId: String): String = withContext(Dispatchers.IO) {
        request("GET", "https://www.googleapis.com/drive/v3/files/$fileId?alt=media", token)
    }

    /**
     * Create or overwrite a file by name in the appDataFolder. Returns its file id.
     * [existingId] avoids a duplicate when the file already exists.
     */
    suspend fun upsert(
        token: String,
        name: String,
        content: String,
        existingId: String?
    ): String = withContext(Dispatchers.IO) {
        val id = existingId ?: createMetadata(token, name)
        val uploadUrl = "https://www.googleapis.com/upload/drive/v3/files/$id?uploadType=media"
        request("PATCH", uploadUrl, token, body = content, contentType = "application/json")
        id
    }

    private fun createMetadata(token: String, name: String): String {
        val metadata = JSONObject()
            .put("name", name)
            .put("parents", org.json.JSONArray().put("appDataFolder"))
        val response = request(
            "POST",
            "https://www.googleapis.com/drive/v3/files?fields=id",
            token,
            body = metadata.toString(),
            contentType = "application/json"
        )
        return JSONObject(response).getString("id")
    }

    private fun request(
        method: String,
        urlString: String,
        token: String,
        body: String? = null,
        contentType: String? = null
    ): String {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("Authorization", "Bearer $token")
            connectTimeout = 30_000
            readTimeout = 30_000
            if (body != null) {
                doOutput = true
                if (contentType != null) setRequestProperty("Content-Type", contentType)
            }
        }
        try {
            if (body != null) {
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            if (code !in 200..299) {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }
                throw IOException("Drive API $method $urlString failed ($code): $error")
            }
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
