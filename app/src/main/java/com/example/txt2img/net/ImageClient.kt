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

    // ============ Agent 提示词扶正 / 视觉测试 / 图片反推 ============

    private fun chatRequest(baseUrl: String, apiKey: String, body: JSONObject): String {
        val r = Request.Builder()
            .url(base(baseUrl) + "/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(body.toString().toRequestBody(jsonMedia))
            .build()
        return client.newCall(r).execute().use { resp ->
            if (!resp.isSuccessful) throw HttpException(resp.code, resp.body?.string().orEmpty())
            resp.body?.string() ?: error("空响应")
        }
    }

    private fun parseChatText(resp: String): String = try {
        val msg = JSONObject(resp).optJSONArray("choices")
            ?.optJSONObject(0)?.optJSONObject("message")
        when (val content = msg?.opt("content")) {
            is String -> content
            is JSONArray -> {
                val sb = StringBuilder()
                for (i in 0 until content.length()) {
                    val item = content.optJSONObject(i) ?: continue
                    val t = item.optString("text", "")
                    if (t.isNotEmpty()) sb.append(t)
                }
                sb.toString()
            }
            else -> ""
        }
    } catch (e: Exception) {
        ""
    }

    /** Agent：把用户提示词扶正优化为更细腻完整的生图提示词。 */
    suspend fun optimizePrompt(
        baseUrl: String,
        apiKey: String,
        model: String,
        prompt: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sys = "你是一位专业的 AI 绘画提示词优化师。用户会给你一段图片生成提示词，请将其优化为更细腻、更具体、可直接用于文生图模型的完整提示词。" +
                "要求：1) 完全保留用户原意与核心描述；2) 补充光影、构图、色彩、质感等细节，但不要过度堆砌；" +
                "3) 只输出提示词本体，不要任何解释、前缀、引号或编号。"
            val body = JSONObject()
                .put("model", model)
                .put("messages", JSONArray()
                    .put(JSONObject().put("role", "system").put("content", sys))
                    .put(JSONObject().put("role", "user").put("content", prompt)))
            val text = parseChatText(chatRequest(baseUrl, apiKey, body))
            if (text.isBlank()) error("返回为空") else Result.success(text.trim())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun tinyTestImage(): String {
        val bmp = android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
        bmp.setPixel(0, 0, android.graphics.Color.RED)
        val baos = java.io.ByteArrayOutputStream()
        bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, baos)
        bmp.recycle()
        return "data:image/png;base64," + Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }

    /** 视觉能力测试：发送 1×1 测试图，能正常返回即视为视觉模型。 */
    suspend fun testVision(baseUrl: String, apiKey: String, model: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val content = JSONArray()
                    .put(JSONObject().put("type", "text").put("text", "这张图片是什么颜色？请只回答：红色 或 蓝色。"))
                    .put(
                        JSONObject()
                            .put("type", "image_url")
                            .put("image_url", JSONObject().put("url", tinyTestImage())),
                    )
                val body = JSONObject()
                    .put("model", model)
                    .put("messages", JSONArray()
                        .put(JSONObject().put("role", "user").put("content", content)))
                val text = parseChatText(chatRequest(baseUrl, apiKey, body))
                if (text.isBlank()) error("返回为空") else Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    data class ReverseResult(val category: String, val prompt: String)

    private val SYS_REVERSE = "你是图片理解与提示词反推专家。用户会发送一张图片，请：\n" +
        "1. 先判断图片类型，从以下选择：UI设计、人物、风景、产品、插画、其他\n" +
        "2. 根据类型输出一段可直接用于 AI 生图的完整提示词（中文），覆盖该类型的关键要素：\n" +
        "   - UI设计：整体风格、材质、主色调与辅助色、组件大小比例、间距节奏、圆角与阴影、字体层级、图标风格、布局结构\n" +
        "   - 人物：面部细节、发型、服饰、姿态、光线方向、背景氛围、镜头景深\n" +
        "   - 风景：时间与光线、天气、构图、色彩基调、环境细节、镜头焦段\n" +
        "   - 产品：材质与表面处理、机位角度、布光方案、背景与道具、质感细节\n" +
        "   - 插画与其他：风格定义、线条配色、构图、氛围\n" +
        "3. 输出格式：第一行输出\"类型：XXX\"，第二行起输出提示词本体。提示词要细腻、具体、可复现，禁止输出任何解释性废话。"

    /** 图片反推提示词（多模态模型）。 */
    suspend fun reversePrompt(
        baseUrl: String,
        apiKey: String,
        model: String,
        imageDataUri: String,
    ): Result<ReverseResult> = withContext(Dispatchers.IO) {
        try {
            val content = JSONArray()
                .put(JSONObject().put("type", "text").put("text", "请根据这张图片反推生成提示词。"))
                .put(
                    JSONObject()
                        .put("type", "image_url")
                        .put("image_url", JSONObject().put("url", imageDataUri)),
                )
            val body = JSONObject()
                .put("model", model)
                .put("messages", JSONArray()
                    .put(JSONObject().put("role", "system").put("content", SYS_REVERSE))
                    .put(JSONObject().put("role", "user").put("content", content)))
            val text = parseChatText(chatRequest(baseUrl, apiKey, body))
            if (text.isBlank()) error("返回为空")
            val trimmed = text.trim()
            var category = "其他"
            var promptBody = trimmed
            val first = trimmed.lines().firstOrNull().orEmpty()
            if (first.contains("类型")) {
                category = first.substringAfter("类型")
                    .trim().removePrefix("：").removePrefix(":").trim().ifEmpty { "其他" }
                promptBody = trimmed.lines().drop(1).joinToString("\n").trim()
            }
            Result.success(ReverseResult(category, promptBody.ifEmpty { trimmed }))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** AI 解析上游报错：分析原因与解决办法。 */
    suspend fun explainError(
        baseUrl: String,
        apiKey: String,
        model: String,
        errorText: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val sys = "你是 API 报错排查专家。用户会粘贴一段调用 AI 生图或对话接口时上游返回的错误信息，请分析：1) 可能的原因；2) 对应的解决办法；3) 需要检查的配置项（接口地址/API Key/模型名/参数等）。" +
                "用中文简洁分点输出，只基于错误信息本身分析，不要臆测超出错误信息的内容。"
            val body = JSONObject()
                .put("model", model)
                .put("messages", JSONArray()
                    .put(JSONObject().put("role", "system").put("content", sys))
                    .put(JSONObject().put("role", "user").put("content", errorText)))
            val text = parseChatText(chatRequest(baseUrl, apiKey, body))
            if (text.isBlank()) error("解析返回为空") else Result.success(text.trim())
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
