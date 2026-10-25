package com.example.vedaahar.network

import android.content.Context
import android.content.SharedPreferences
import com.example.vedaahar.BuildConfig
import okio.ByteString.Companion.decodeBase64
import org.json.JSONObject
import java.security.MessageDigest

object PatientSession {
    // Decoding only partitions local data. Django still validates every JWT.
    fun keyFor(accessToken: String?, baseUrl: String): String? = runCatching {
        val payload = accessToken?.split('.')?.getOrNull(1)?.decodeBase64()?.utf8() ?: return null
        val id = JSONObject(payload).get("user_id").toString()
        require(id.matches(Regex("[1-9][0-9]*")))
        "${baseUrl.trimEnd('/')}|$id"
    }.getOrNull()

    fun currentKey(context: Context): String? = keyFor(AuthTokenStore(context).getAccessToken(), BuildConfig.AGENTIC_API_BASE_URL)

    fun preferences(context: Context, name: String, owner: String? = currentKey(context)): SharedPreferences? {
        if (owner == null) return null
        val suffix = MessageDigest.getInstance("SHA-256").digest(owner.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        // Unowned legacy preferences are retained, never assigned to whoever logs in next.
        return context.getSharedPreferences("${name}_patient_$suffix", Context.MODE_PRIVATE)
    }
}
