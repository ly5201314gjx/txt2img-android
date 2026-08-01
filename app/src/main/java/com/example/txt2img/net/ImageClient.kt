package com.example.txt2img.net

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class RefImage(val bytes: ByteArray, val mime: String) {
    fun dataUri(): String = "data:$mime;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
}

data class GenRequest(
    val baseUrl: String,
    val apiKey: String,
    val model: String,
    val prompt: String,
    val count: Int = 1,
    val size: String = "1024x1024",
    val qualityParam: String? = null,
    val steps: Int? = null,
    val refImages: List<RefImage> = emptyList(),
)

data class GenOutcome(val images: List<ByteArray>, val mode: String)

/**
 * 兼容第三方 gpt-image 转发生图协议（支持多参考图联动，上限 3 张）：
 *  - /images/generations（JSON：model/prompt/n/size/quality；兼容 SiliconFlow image/image2/image3 内联 base64）
 *  - /images/edits（gpt-image 新版 JSON images:[{image_url}]，及旧版 multipart image[]）
 *  - /chat/completions（OpenRouter 等 Chat 型返回 image_url）
 * 自动尝试降级链，任何一步成功即返回，全部失败时汇总各步原因。
 */
object ImageClient {

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    private fun base(raw: String): String = raw.trim().trimEnd('/')

    // ============ 模型列表 ============

    suspend fun fetchModels(baseUrl: String, apiKey: String): Result<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder()
                    .url(base(baseUrl) + "/models")
                    .header("Authorization", "Bearer $apiKey")
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) throw HttpException(resp.code, resp.body?.string().orEmpty())
                    val body = resp.body?.string() ?: error("空响应")
                    val root = JSONObject(body)
                    val ids = mutableListOf<String>()
                    val arr = root.optJSONArray("data") ?: root.optJSONArray("models")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val o = arr.optJSONObject(i) ?: continue
                            val id = o.optString("id", o.optString("name", ""))
                            if (id.isNotEmpty()) ids.add(id)
                        }
                    }
                    Result.success(ids)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ============ 生图主入口（多策略降级链） ============

    suspend fun generate(req: GenRequest): Result<GenOutcome> = withContext(Dispatchers.IO) {
        try {
            val errors = mutableListOf<String>()
            val withRefs = req.refImages.isNotEmpty()
            val attempts = buildList<Pair<String, () -> List<ByteArray>>> {
                if (withRefs) {
                    // gpt-image 转第三方：参考图优先走 /images/edits（JSON images[]）
                    add("edits-json" to { postEditsJson(req) })
                    add("generations+image" to { postGenerations(req, withImage = true, minimal = false) })
                    add("edits-multipart" to { postEditsMultipart(req) })
                } else {
                    add("generations" to { postGenerations(req, withImage = false, minimal = false) })
                }
                add("generations-min" to { postGenerations(req, withImage = withRefs, minimal = true) })
                add("chat" to { postChat(req) })
            }
            for ((mode, fn) in attempts) {
                try {
                    val imgs = fn()
                    if (imgs.isNotEmpty()) {
                        return@withContext Result.success(GenOutcome(imgs, mode))
                    }
                    errors.add("$mode：返回空")
                } catch (e: Exception) {
                    errors.add("$mode：${e.message?.take(160)}")
                }
            }
            Result.failure(Exception(errors.joinToString("；")))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ============ 各策略实现 ============

    private fun postGenerations(req: GenRequest, withImage: Boolean, minimal: Boolean): List<ByteArray> {
        val body = JSONObject()
            .put("model", req.model)
            .put("prompt", req.prompt)
        if (!minimal) {
            body.put("n", req.count)
            body.put("batch_size", req.count)
            req.qualityParam?.let { body.put("quality", it) }
            req.steps?.let { body.put("num_inference_steps", it) }
        }
        body.put("size", req.size)
        body.put("image_size", req.size)
        if (withImage) {
            // SiliconFlow 多参考图：image / image2 / image3
            req.refImages.getOrNull(0)?.let { body.put("image", it.dataUri()) }
            req.refImages.getOrNull(1)?.let { body.put("image2", it.dataUri()) }
            req.refImages.getOrNull(2)?.let { body.put("image3", it.dataUri()) }
        }
        return parseImages(execJson(req, "/images/generations", body), req.count)
    }

    private fun postEditsJson(req: GenRequest): List<ByteArray> {
        if (req.refImages.isEmpty()) error("缺少参考图")
        val imagesArr = JSONArray()
        req.refImages.forEach { imagesArr.put(JSONObject().put("image_url", it.dataUri())) }
        val body = JSONObject()
            .put("model", req.model)
            .put("prompt", req.prompt)
            .put("images", imagesArr)
            .put("n", req.count)
            .put("size", req.size)
        req.qualityParam?.let { body.put("quality", it) }
        return parseImages(execJson(req, "/images/edits", body), req.count)
    }

    private fun postEditsMultipart(req: GenRequest): List<ByteArray> {
        if (req.refImages.isEmpty()) error("缺少参考图")
        val mp = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("prompt", req.prompt)
            .addFormDataPart("model", req.model)
            .addFormDataPart("n", req.count.toString())
            .addFormDataPart("size", req.size)
        req.refImages.forEachIndexed { i, ref ->
            val ext = when {
                ref.mime.contains("png") -> "png"
                ref.mime.contains("webp") -> "webp"
                else -> "jpg"
            }
            mp.addFormDataPart(
                "image[]",
                "ref${i + 1}.$ext",
                ref.bytes.toRequestBody(ref.mime.toMediaTypeOrNull()),
            )
        }
        val r = Request.Builder()
            .url(base(req.baseUrl) + "/images/edits")
            .header("Authorization", "Bearer ${req.apiKey}")
            .post(mp.build())
            .build()
        val resp = client.newCall(r).execute().use { it ->
            if (!it.isSuccessful) throw HttpException(it.code, it.body?.string().orEmpty())
            it.body?.string() ?: error("空响应")
        }
        return parseImages(resp, req.count)
    }

    private fun postChat(req: GenRequest): List<ByteArray> {
        val content = JSONArray()
        content.put(JSONObject().put("type", "text").put("text", req.prompt))
        req.refImages.forEach {
            content.put(
                JSONObject()
                    .put("type", "image_url")
                    .put("image_url", JSONObject().put("url", it.dataUri())),
            )
        }
        val body = JSONObject()
            .put("model", req.model)
            .put("n", req.count)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", content)))
        return parseImages(execJson(req, "/chat/completions", body), req.count)
    }

    // ============ 响应解析（兼容多种返回格式） ============

    private fun parseImages(body: String, max: Int): List<ByteArray> {
        val out = mutableListOf<ByteArray>()
        val root = try {
            JSONObject(body)
        } catch (e: Exception) {
            return emptyList()
        }
        val arr = root.optJSONArray("data") ?: root.optJSONArray("images")
            ?: root.optJSONObject("output")?.optJSONArray("images")
            ?: root.optJSONObject("output")?.optJSONArray("data")
        if (arr != null) {
            for (i in 0 until arr.length()) {
                if (out.size >= max) break
                val item = arr.optJSONObject(i) ?: continue
                for (key in listOf("url", "b64_json", "output_url", "image")) {
                    val v = item.optString(key).orEmpty()
                    if (v.isNotEmpty()) {
                        decodeImage(v)?.let { out.add(it) }
                        break
                    }
                }
            }
        }
        if (out.isEmpty()) parseChatImages(root, max, out)
        return out
    }

    private fun parseChatImages(root: JSONObject, max: Int, out: MutableList<ByteArray>) {
        val choice = root.optJSONArray("choices")?.optJSONObject(0) ?: return
        val message = choice.optJSONObject("message") ?: return
        when (val content = message.opt("content")) {
            is JSONArray -> {
                for (i in 0 until content.length()) {
                    if (out.size >= max) break
                    val item = content.optJSONObject(i) ?: continue
                    if (item.optString("type") == "image_url") {
                        val url = item.optJSONObject("image_url")?.optString("url").orEmpty()
                        if (url.isNotEmpty()) decodeImage(url)?.let { out.add(it) }
                    }
                }
            }
            is String -> if (content.isNotEmpty()) decodeImage(content)?.let { out.add(it) }
            else -> {}
        }
        if (out.isEmpty()) {
            val images = message.optJSONArray("images")
            if (images != null) {
                for (i in 0 until images.length()) {
                    if (out.size >= max) break
                    val u = images.optJSONObject(i)?.optString("url").orEmpty()
                    if (u.isNotEmpty()) decodeImage(u)?.let { out.add(it) }
                }
            }
        }
    }

    private fun decodeImage(v: String): ByteArray? = try {
        when {
            v.startsWith("data:") -> {
                val idx = v.indexOf(',')
                if (idx < 0) null else Base64.decode(v.substring(idx + 1), Base64.DEFAULT)
            }
            v.startsWith("http://") || v.startsWith("https://") -> {
                val dl = Request.Builder().url(v).build()
                client.newCall(dl).execute().use { r ->
                    if (r.isSuccessful) r.body?.bytes() else null
                }
            }
            else -> Base64.decode(v, Base64.DEFAULT)
        }
    } catch (e: Exception) {
        null
    }

    private fun execJson(req: GenRequest, path: String, jsonBody: JSONObject): String {
        val r = Request.Builder()
            .url(base(req.baseUrl) + path)
            .header("Authorization", "Bearer ${req.apiKey}")
            .post(jsonBody.toString().toRequestBody(jsonMedia))
            .build()
        return client.newCall(r).execute().use { resp ->
            if (!resp.isSuccessful) throw HttpException(resp.code, resp.body?.string().orEmpty())
            resp.body?.string() ?: error("空响应")
        }
    }

    private fun extractMessage(root: JSONObject): String {
        when (val m = root.opt("message")) {
            is JSONObject -> return m.optString("message", m.optString("data", ""))
            is String -> if (m.isNotEmpty()) return m
            else -> {}
        }
        root.optJSONObject("error")?.optString("message", "")?.takeIf { it.isNotEmpty() }?.let { return it }
        return ""
    }

    private class HttpException(code: Int, body: String) : Exception(describeError(code, body)) {
        private companion object {
            fun describeError(code: Int, body: String): String {
                val msg = try {
                    extractMessage(JSONObject(body))
                } catch (e: Exception) {
                    ""
                }
                return if (msg.isNotEmpty()) "HTTP $code：$msg" else "HTTP $code：${body.take(140)}"
            }
        }
    }
}
