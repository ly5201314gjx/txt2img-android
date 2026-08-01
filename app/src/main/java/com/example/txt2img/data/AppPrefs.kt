package com.example.txt2img.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore by preferencesDataStore(name = "app_prefs")

data class ProviderConfig(
    val id: String,
    val name: String,
    val url: String,
    val key: String,
    val models: List<String>,
    val shownModels: List<String>,
    val selectedModel: String,
)

data class CurrentSelection(val providerId: String, val model: String)

/**
 * 多供应商 / 多模型 / 分类 的 JSON 序列化工具。
 */
object PrefsJson {

    fun parseProviders(json: String): List<ProviderConfig> = try {
        val arr = JSONArray(json)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val models = mutableListOf<String>()
                val mArr = o.optJSONArray("models")
                if (mArr != null) {
                    for (j in 0 until mArr.length()) {
                        mArr.optString(j, "").takeIf { it.isNotEmpty() }?.let { models.add(it) }
                    }
                }
                val shown = mutableListOf<String>()
                val sArr = o.optJSONArray("shown")
                if (sArr != null) {
                    for (j in 0 until sArr.length()) {
                        sArr.optString(j, "").takeIf { it.isNotEmpty() }?.let { shown.add(it) }
                    }
                }
                add(
                    ProviderConfig(
                        id = o.optString("id", ""),
                        name = o.optString("name", ""),
                        url = o.optString("url", ""),
                        key = o.optString("key", ""),
                        models = models,
                        shownModels = shown,
                        selectedModel = o.optString("selected", ""),
                    ),
                )
            }
        }
    } catch (e: Exception) {
        emptyList()
    }

    fun providersToJson(list: List<ProviderConfig>): String {
        val arr = JSONArray()
        list.forEach { p ->
            val mArr = JSONArray()
            p.models.forEach { mArr.put(it) }
            val sArr = JSONArray()
            p.shownModels.forEach { sArr.put(it) }
            arr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("url", p.url)
                    .put("key", p.key)
                    .put("models", mArr)
                    .put("shown", sArr)
                    .put("selected", p.selectedModel),
            )
        }
        return arr.toString()
    }

    fun parseCurrent(json: String): CurrentSelection = try {
        val o = JSONObject(json)
        CurrentSelection(o.optString("provider", ""), o.optString("model", ""))
    } catch (e: Exception) {
        CurrentSelection("", "")
    }

    fun parseCats(json: String): List<String> = try {
        val arr = JSONArray(json)
        buildList {
            for (i in 0 until arr.length()) {
                arr.optString(i, "").takeIf { it.isNotEmpty() }?.let { add(it) }
            }
        }
    } catch (e: Exception) {
        emptyList()
    }

    fun catsToJson(list: List<String>): String {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        return arr.toString()
    }
}

/**
 * 全局持久化：多供应商配置、当前选用、作品分类、作品记录。
 */
class AppPrefs(private val context: Context) {

    private object Keys {
        val PROVIDERS = stringPreferencesKey("providers_json")
        val CURRENT = stringPreferencesKey("current_json")
        val CATS = stringPreferencesKey("cats_json")
        val IMAGES = stringPreferencesKey("images_json")
        val ASKED_BATTERY = booleanPreferencesKey("asked_battery")
        val AGENT = stringPreferencesKey("agent_json")
        val VISION = stringPreferencesKey("vision_json")
        val REVERSE = booleanPreferencesKey("reverse_enabled")
        val REVERSE_MODEL = stringPreferencesKey("reverse_json")
        val ALL_POS = intPreferencesKey("all_pos")
    }

    val providersJson: Flow<String> = context.dataStore.data.map { it[Keys.PROVIDERS] ?: "[]" }
    val currentJson: Flow<String> = context.dataStore.data.map { it[Keys.CURRENT] ?: "{}" }
    val catsJson: Flow<String> = context.dataStore.data.map { it[Keys.CATS] ?: "[]" }
    val imagesJson: Flow<String> = context.dataStore.data.map { it[Keys.IMAGES] ?: "[]" }
    val agentJson: Flow<String> = context.dataStore.data.map { it[Keys.AGENT] ?: "{}" }
    val visionJson: Flow<String> = context.dataStore.data.map { it[Keys.VISION] ?: "{}" }
    val reverseModelJson: Flow<String> = context.dataStore.data.map { it[Keys.REVERSE_MODEL] ?: "{}" }
    val reverseEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.REVERSE] ?: false }
    val allPos: Flow<Int> = context.dataStore.data.map { it[Keys.ALL_POS] ?: 0 }
    val askedBattery: Flow<Boolean> = context.dataStore.data.map { it[Keys.ASKED_BATTERY] ?: false }

    suspend fun markAskedBattery() {
        context.dataStore.edit { p -> p[Keys.ASKED_BATTERY] = true }
    }

    suspend fun saveAllPos(v: Int) {
        context.dataStore.edit { p -> p[Keys.ALL_POS] = v }
    }

    suspend fun saveReverseEnabled(v: Boolean) {
        context.dataStore.edit { p -> p[Keys.REVERSE] = v }
    }

    suspend fun saveAgent(providerId: String, model: String) {
        context.dataStore.edit { p ->
            p[Keys.AGENT] = JSONObject()
                .put("provider", providerId)
                .put("model", model)
                .toString()
        }
    }

    suspend fun saveReverseModel(providerId: String, model: String) {
        context.dataStore.edit { p ->
            p[Keys.REVERSE_MODEL] = JSONObject()
                .put("provider", providerId)
                .put("model", model)
                .toString()
        }
    }

    /** 视觉能力测试结果缓存：key = "providerId|model"，value = "yes" / "no"。 */
    suspend fun saveVisionResult(key: String, ok: Boolean) {
        context.dataStore.edit { p ->
            val cur = try { JSONObject(p[Keys.VISION] ?: "{}") } catch (e: Exception) { JSONObject() }
            cur.put(key, if (ok) "yes" else "no")
            p[Keys.VISION] = cur.toString()
        }
    }

    suspend fun saveProviders(list: List<ProviderConfig>) {
        context.dataStore.edit { p -> p[Keys.PROVIDERS] = PrefsJson.providersToJson(list) }
    }

    suspend fun saveCurrent(providerId: String, model: String) {
        context.dataStore.edit { p ->
            p[Keys.CURRENT] = JSONObject()
                .put("provider", providerId)
                .put("model", model)
                .toString()
        }
    }

    suspend fun saveCats(list: List<String>) {
        context.dataStore.edit { p -> p[Keys.CATS] = PrefsJson.catsToJson(list) }
    }

    suspend fun appendImage(
        prompt: String,
        time: Long,
        file: String,
        refFile: String? = null,
        durationMs: Long = 0L,
        ratio: String = "",
        type: String = "",
    ) {
        context.dataStore.edit { p ->
            val cur = try { JSONArray(p[Keys.IMAGES] ?: "[]") } catch (e: Exception) { JSONArray() }
            val entry = JSONObject()
                .put("prompt", prompt)
                .put("time", time)
                .put("file", file)
                .put("dur", durationMs)
                .put("ratio", ratio)
                .put("type", type)
            if (!refFile.isNullOrEmpty()) entry.put("ref", refFile)
            cur.put(entry)
            p[Keys.IMAGES] = cur.toString()
        }
    }

    suspend fun setImageCategory(file: String, cat: String) {
        context.dataStore.edit { p ->
            val cur = try { JSONArray(p[Keys.IMAGES] ?: "[]") } catch (e: Exception) { JSONArray() }
            val out = JSONArray()
            for (i in 0 until cur.length()) {
                val o = cur.optJSONObject(i) ?: continue
                if (o.optString("file", "") == file) o.put("cat", cat)
                out.put(o)
            }
            p[Keys.IMAGES] = out.toString()
        }
    }

    suspend fun removeImage(file: String) {
        context.dataStore.edit { p ->
            val cur = try { JSONArray(p[Keys.IMAGES] ?: "[]") } catch (e: Exception) { JSONArray() }
            val out = JSONArray()
            for (i in 0 until cur.length()) {
                val o = cur.optJSONObject(i) ?: continue
                if (o.optString("file", "") != file) out.put(o)
            }
            p[Keys.IMAGES] = out.toString()
        }
    }
}
