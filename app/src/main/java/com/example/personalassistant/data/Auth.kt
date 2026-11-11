package com.example.personalassistant.data

import android.util.Log
import com.example.personalassistant.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

data class AuthUser(
    val id: Long,
    val username: String,
    val phone: String,
    val displayName: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("username", username)
        put("phone", phone)
        put("displayName", displayName)
    }

    companion object {
        fun fromJson(value: JSONObject) = AuthUser(
            id = value.getLong("id"),
            username = value.getString("username"),
            phone = value.optString("phone"),
            displayName = value.optString("displayName", value.getString("username"))
        )
    }
}

data class AuthSession(val token: String, val user: AuthUser)

class AuthRepository(
    private val baseUrl: String = BuildConfig.API_BASE_URL
) {
    suspend fun login(account: String, password: String): Result<AuthSession> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            Log.d("AuthRepository", "POST ${baseUrl.trimEnd('/')}/api/auth/login")
            connection = (URL("${baseUrl.trimEnd('/')}/api/auth/login").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 6_000
                readTimeout = 10_000
                doOutput = true
                useCaches = false
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }

            val bytes = JSONObject().apply {
                put("account", account.trim())
                put("password", password)
            }.toString().toByteArray(Charsets.UTF_8)
            connection.setFixedLengthStreamingMode(bytes.size)
            connection.outputStream.use { it.write(bytes) }

            val status = connection.responseCode
            Log.d("AuthRepository", "HTTP $status")
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(text) }.getOrNull()

            if (status in 200..299) {
                val data = json?.getJSONObject("data")
                val token = data?.getString("token")
                val user = data?.getJSONObject("user")?.let(AuthUser::fromJson)
                if (token.isNullOrBlank() || user == null) {
                    Result.failure(IllegalStateException("服务器返回数据不完整"))
                } else {
                    Result.success(AuthSession(token, user))
                }
            } else {
                Result.failure(IllegalStateException(
                    json?.optString("message")?.takeIf { it.isNotBlank() } ?: "登录失败（$status）"
                ))
            }
        } catch (exception: SocketTimeoutException) {
            Log.w("AuthRepository", "Login timeout", exception)
            Result.failure(IllegalStateException("连接服务器超时"))
        } catch (exception: UnknownHostException) {
            Log.w("AuthRepository", "Unknown host", exception)
            Result.failure(IllegalStateException("无法连接服务器"))
        } catch (exception: Exception) {
            Log.w("AuthRepository", "Login failed", exception)
            Result.failure(IllegalStateException(exception.message ?: "无法连接服务器"))
        } finally {
            connection?.disconnect()
        }
    }
}
