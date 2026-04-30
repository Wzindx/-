package com.yang.emperor

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val LEGACY_CONFIG_PREFS_NAME = "config"
private const val SECURE_CONFIG_PREFS_NAME = "secure_config"
private const val SECURE_MIGRATED_FROM_V16 = "secureMigratedFromV16"
private const val HISTORY_KEY = "history"
private const val MAX_HISTORY_ITEMS = 50
private const val MAX_HISTORY_ERROR_CHARS = 1_500

fun now(): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

fun secureConfigPreferences(context: Context): SharedPreferences {
    val appContext = context.applicationContext
    val legacyPrefs = appContext.getSharedPreferences(LEGACY_CONFIG_PREFS_NAME, Context.MODE_PRIVATE)

    val securePrefs = createEncryptedPreferencesOrNull(appContext)
        ?: return legacyPrefs

    migrateLegacyConfigIfNeeded(legacyPrefs, securePrefs)
    return securePrefs
}

fun loadHistory(prefs: SharedPreferences): List<HistoryItem> {
    val raw = prefs.getString(HISTORY_KEY, "[]") ?: "[]"
    return runCatching {
        val arr = JSONArray(raw)
        (0 until arr.length()).mapNotNull { index ->
            val item = arr.optJSONObject(index) ?: return@mapNotNull null
            val time = item.optString("time").trim()
            val prompt = item.optString("prompt").trim()
            val state = item.optString("state", "success").ifBlank { "success" }

            if (time.isBlank() && prompt.isBlank()) {
                return@mapNotNull null
            }

            HistoryItem(
                time = time,
                mode = item.optString("mode"),
                model = item.optString("model"),
                prompt = prompt,
                path = item.optString("path"),
                state = state,
                error = item.optString("error", "").truncateHistoryError()
            )
        }
    }.getOrElse {
        emptyList()
    }
}

fun saveHistory(prefs: SharedPreferences, items: List<HistoryItem>) {
    val arr = JSONArray()
    items.take(MAX_HISTORY_ITEMS).forEach { item ->
        arr.put(
            JSONObject()
                .put("time", item.time)
                .put("mode", item.mode)
                .put("model", item.model)
                .put("prompt", item.prompt)
                .put("path", item.path)
                .put("state", item.state.ifBlank { "success" })
                .put("error", item.error.truncateHistoryError())
        )
    }
    prefs.edit().putString(HISTORY_KEY, arr.toString()).apply()
}

private fun createEncryptedPreferencesOrNull(context: Context): SharedPreferences? {
    return runCatching {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            SECURE_CONFIG_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }.getOrNull()
}

private fun migrateLegacyConfigIfNeeded(
    legacyPrefs: SharedPreferences,
    securePrefs: SharedPreferences
) {
    if (securePrefs.getBoolean(SECURE_MIGRATED_FROM_V16, false)) return

    val keys = listOf(
        "baseUrl",
        "apiKey",
        "apiMode",
        "generateModel",
        "editModel",
        "model",
        HISTORY_KEY
    )

    val editor = securePrefs.edit()
    keys.forEach { key ->
        val value = legacyPrefs.getString(key, null)
        if (value != null) {
            editor.putString(key, value)
        }
    }
    editor.putBoolean(SECURE_MIGRATED_FROM_V16, true).apply()

    legacyPrefs.edit().apply {
        keys.forEach { key -> remove(key) }
    }.apply()
}

private fun String.truncateHistoryError(): String {
    if (length <= MAX_HISTORY_ERROR_CHARS) return this
    return take(MAX_HISTORY_ERROR_CHARS) + "\n...（错误信息过长，已截断）"
}
