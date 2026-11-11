package com.example.personalassistant.data

import com.example.personalassistant.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder

data class MailAccount(
    val id: Long,
    val email: String,
    val displayName: String,
    val status: Int,
    val lastSyncAt: String?
) {
    companion object {
        fun fromJson(value: JSONObject) = MailAccount(
            id = value.getLong("id"),
            email = value.getString("email"),
            displayName = value.optString("displayName", ""),
            status = value.optInt("status", 1),
            lastSyncAt = value.optString("lastSyncAt").takeIf { it.isNotBlank() && it != "null" }
        )
    }
}

data class RemoteMail(
    val id: Long,
    val subject: String,
    val fromName: String,
    val fromAddress: String,
    val sentAt: String?,
    val snippet: String,
    val read: Boolean,
    val starred: Boolean,
    val hasAttachments: Boolean
) {
    companion object {
        fun fromJson(value: JSONObject) = RemoteMail(
            id = value.getLong("id"),
            subject = value.optString("subject"),
            fromName = value.optString("fromName"),
            fromAddress = value.optString("fromAddress"),
            sentAt = value.optString("sentAt").takeIf { it.isNotBlank() && it != "null" },
            snippet = value.optString("snippet"),
            read = value.optBoolean("read"),
            starred = value.optBoolean("starred"),
            hasAttachments = value.optBoolean("hasAttachments")
        )
    }
}

data class RemoteMailDetail(
    val id: Long,
    val subject: String,
    val fromName: String,
    val fromAddress: String,
    val toAddresses: String,
    val sentAt: String?,
    val bodyText: String,
    val read: Boolean,
    val starred: Boolean,
    val hasAttachments: Boolean,
    val attachmentCount: Int
) {
    companion object {
        fun fromJson(value: JSONObject) = RemoteMailDetail(
            id = value.getLong("id"),
            subject = value.optString("subject"),
            fromName = value.optString("fromName"),
            fromAddress = value.optString("fromAddress"),
            toAddresses = value.optString("toAddresses"),
            sentAt = value.optString("sentAt").takeIf { it.isNotBlank() && it != "null" },
            bodyText = value.optString("bodyText"),
            read = value.optBoolean("read"),
            starred = value.optBoolean("starred"),
            hasAttachments = value.optBoolean("hasAttachments"),
            attachmentCount = value.optInt("attachmentCount", 0)
        )
    }
}

data class RemoteMailPage(
    val items: List<RemoteMail>,
    val page: Int,
    val size: Int,
    val total: Long,
    val hasMore: Boolean
)

data class MailBindResult(val account: MailAccount)

class MailApiException(val status: Int, override val message: String) : Exception(message)

class MailRepository(
    private val baseUrl: String = BuildConfig.API_BASE_URL
) {
    suspend fun listAccounts(token: String): Result<List<MailAccount>> = runCatching {
        val data = request("GET", "/api/mail/accounts", token).getJSONArray("data")
        (0 until data.length()).map { MailAccount.fromJson(data.getJSONObject(it)) }
    }

    suspend fun bind(token: String, email: String, secret: String, displayName: String): Result<MailAccount> = runCatching {
        val data = request(
            "POST", "/api/mail/accounts", token,
            JSONObject().apply {
                put("email", email.trim())
                put("secret", secret)
                put("displayName", displayName.trim())
            }
        ).getJSONObject("data")
        MailAccount.fromJson(data)
    }

    suspend fun unbind(token: String, accountId: Long): Result<Unit> = runCatching {
        request("DELETE", "/api/mail/accounts/$accountId", token)
        Unit
    }

    suspend fun sync(token: String, accountId: Long): Result<Unit> = runCatching {
        request("POST", "/api/mail/accounts/$accountId/sync", token)
        Unit
    }

    suspend fun listMessages(
        token: String,
        accountId: Long,
        page: Int = 1,
        size: Int = 30,
        keyword: String = "",
        unreadOnly: Boolean = false
    ): Result<RemoteMailPage> = runCatching {
        val query = buildString {
            append("/api/mail/messages?accountId=").append(accountId)
            append("&folder=INBOX&page=").append(page)
            append("&size=").append(size)
            append("&unreadOnly=").append(unreadOnly)
            if (keyword.isNotBlank()) append("&keyword=").append(URLEncoder.encode(keyword, "UTF-8"))
        }
        val data = request("GET", query, token).getJSONObject("data")
        val items = data.optJSONArray("items") ?: JSONArray()
        RemoteMailPage(
            items = (0 until items.length()).map { RemoteMail.fromJson(items.getJSONObject(it)) },
            page = data.optInt("page", page),
            size = data.optInt("size", size),
            total = data.optLong("total"),
            hasMore = data.optBoolean("hasMore")
        )
    }

    suspend fun getMessage(token: String, messageId: Long): Result<RemoteMailDetail> = runCatching {
        RemoteMailDetail.fromJson(
            request("GET", "/api/mail/messages/$messageId", token).getJSONObject("data")
        )
    }

    suspend fun markRead(token: String, messageId: Long, read: Boolean): Result<Unit> = runCatching {
        request(
            "POST", "/api/mail/messages/$messageId/read", token,
            JSONObject().put("read", read)
        )
        Unit
    }

    suspend fun send(token: String, accountId: Long, to: String, subject: String, body: String): Result<Unit> = runCatching {
        request(
            "POST", "/api/mail/send", token,
            JSONObject().apply {
                put("accountId", accountId)
                put("to", to.trim())
                put("subject", subject.trim())
                put("body", body)
            }
        )
        Unit
    }

    private suspend fun request(method: String, path: String, token: String, body: JSONObject? = null): JSONObject =
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL("${baseUrl.trimEnd('/')}$path").openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = 10_000
                    readTimeout = 60_000
                    useCaches = false
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                    if (body != null) {
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    }
                }
                if (body != null) {
                    val bytes = body.toString().toByteArray(Charsets.UTF_8)
                    connection.setFixedLengthStreamingMode(bytes.size)
                    connection.outputStream.use { it.write(bytes) }
                }

                val status = connection.responseCode
                val text = (if (status in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(text) }.getOrNull()
                if (status !in 200..299) {
                    throw MailApiException(status, json?.optString("message")?.takeIf { it.isNotBlank() } ?: "请求失败（$status）")
                }
                json ?: throw IllegalStateException("服务器返回数据不完整")
            } catch (exception: MailApiException) {
                throw exception
            } catch (exception: SocketTimeoutException) {
                throw IllegalStateException("连接服务器超时")
            } catch (exception: Exception) {
                throw IllegalStateException(exception.message ?: "无法连接服务器")
            } finally {
                connection?.disconnect()
            }
        }
}
