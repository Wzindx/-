package com.yang.emperor

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


fun now(): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
fun secureConfigPreferences(context: Context): android.content.SharedPreferences {
    val legacyPrefs = context.getSharedPreferences("config", Context.MODE_PRIVATE)

    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    val securePrefs = EncryptedSharedPreferences.create(
        context,
        "secure_config",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    if (!securePrefs.getBoolean("secureMigratedFromV16", false)) {
        val editor = securePrefs.edit()
        val keys = listOf("baseUrl", "apiKey", "apiMode", "generateModel", "editModel", "model", "history")
        keys.forEach { key ->
            val value = legacyPrefs.getString(key, null)
            if (value != null) editor.putString(key, value)
        }
        editor.putBoolean("secureMigratedFromV16", true).apply()

        legacyPrefs.edit()
            .remove("baseUrl")
            .remove("apiKey")
            .remove("apiMode")
            .remove("generateModel")
            .remove("editModel")
            .remove("model")
            .remove("history")
            .apply()
    }

    return securePrefs
}
fun loadHistory(prefs: android.content.SharedPreferences): List<HistoryItem> {
    val raw = prefs.getString("history", "[]") ?: "[]"
    return runCatching {
        val arr = JSONArray(raw)
        (0 until arr.length()).mapNotNull { i ->
            arr.optJSONObject(i)?.let {
                HistoryItem(
                    time = it.optString("time"),
                    mode = it.optString("mode"),
                    model = it.optString("model"),
                    prompt = it.optString("prompt"),
                    path = it.optString("path"),
                    state = it.optString("state", "success"),
                    error = it.optString("error", "")
                )
            }
        }
    }.getOrElse { emptyList() }
}
fun saveHistory(prefs: android.content.SharedPreferences, items: List<HistoryItem>) {
    val arr = JSONArray()
    items.take(50).forEach {
        arr.put(
            JSONObject()
                .put("time", it.time)
                .put("mode", it.mode)
                .put("model", it.model)
                .put("prompt", it.prompt)
                .put("path", it.path)
                .put("state", it.state)
                .put("error", it.error)
        )
    }
    prefs.edit().putString("history", arr.toString()).apply()
}
