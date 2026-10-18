package com.pindou.studio.data

import android.content.Context
import com.pindou.studio.core.ColorMath
import com.pindou.studio.core.Palette
import com.pindou.studio.core.PaletteColor
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * 色卡仓库：内置色卡来自 assets，用户导入 / 自制的色卡保存在应用私有目录。
 */
class PaletteRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val userDir: File by lazy {
        File(context.filesDir, "palettes").apply { mkdirs() }
    }

    private var builtinCache: List<Palette>? = null
    private var userCache: List<Palette>? = null

    /** 全部色卡（内置在前）。 */
    fun all(): List<Palette> = builtin() + user()

    fun builtin(): List<Palette> {
        builtinCache?.let { return it }
        val loaded = try {
            val text = context.assets.open("palettes.json").bufferedReader().use { it.readText() }
            json.decodeFromString(PaletteAssetDto.serializer(), text)
                .palettes.map { it.copy(builtin = true).toModel() }
        } catch (e: Exception) {
            emptyList()
        }
        builtinCache = loaded
        return loaded
    }

    fun user(): List<Palette> {
        userCache?.let { return it }
        val list = ArrayList<Palette>()
        userDir.listFiles { f -> f.extension == "json" }?.forEach { file ->
            try {
                val dto = json.decodeFromString(PaletteDto.serializer(), file.readText())
                list.add(dto.copy(builtin = false).toModel())
            } catch (_: Exception) {
                // 损坏文件忽略
            }
        }
        val sorted = list.sortedBy { it.name }
        userCache = sorted
        return sorted
    }

    fun invalidate() {
        userCache = null
    }

    fun byId(id: String): Palette? = all().firstOrNull { it.id == id }

    /** 默认色卡。 */
    fun default(): Palette {
        val list = all()
        return list.firstOrNull { it.id == "brand-mard" }
            ?: list.firstOrNull { it.id == "draw-MARD-221" }
            ?: list.firstOrNull()
            ?: Palette("empty", "空色卡", emptyList())
    }

    fun saveUser(palette: Palette) {
        val dto = PaletteDto.fromModel(palette)
        File(userDir, palette.id + ".json")
            .writeText(json.encodeToString(PaletteDto.serializer(), dto))
        invalidate()
    }

    fun deleteUser(id: String): Boolean {
        val file = File(userDir, id + ".json")
        val ok = file.exists() && file.delete()
        invalidate()
        return ok
    }

    fun newUserId(): String = "user-" + System.currentTimeMillis()

    /**
     * 解析色卡文本（JSON 或 CSV），返回解析出的色卡列表。
     */
    fun parsePaletteText(text: String, fallbackName: String): List<Palette> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return emptyList()
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            val colors = parseCsv(trimmed)
            if (colors.isEmpty()) return emptyList()
            return listOf(Palette(newUserId(), fallbackName.ifBlank { "导入色卡" }, colors, builtin = false))
        }
        return try {
            parseJsonPalettes(json.parseToJsonElement(trimmed), fallbackName)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** 解析各类 JSON 色卡结构。 */
    fun parseJsonPalettes(root: JsonElement, fallbackName: String): List<Palette> {
        if (root is JsonObject) {
            val palettesEl = root["palettes"]
            if (palettesEl is JsonArray) {
                return palettesEl.mapNotNull { el ->
                    runCatching {
                        json.decodeFromJsonElement(PaletteDto.serializer(), el)
                    }.getOrNull()
                }.map { it.copy(id = newUserId(), builtin = false).toModel() }
                    .filter { it.colors.isNotEmpty() }
            }
            val colorsEl = root["colors"] ?: root["entries"]
            if (colorsEl is JsonArray) {
                val colors = parseColorArray(colorsEl)
                if (colors.isNotEmpty()) {
                    val name = str(root["name"]) ?: fallbackName
                    return listOf(
                        Palette(newUserId(), name.ifBlank { "导入色卡" }, colors, builtin = false)
                    )
                }
            }
            // pindou-draw 的 { "A1": {...}, "A2": {...} }
            val colors = ArrayList<PaletteColor>()
            for ((key, value) in root) {
                if (value !is JsonObject) continue
                val code = str(value["num"]) ?: str(value["code"]) ?: key
                val hex = pickColor(value["color1"], value["color"], value["hex"]) ?: continue
                val hex2 = pickColor(value["color2"], value["hex2"])
                val type = str(value["type"]) ?: PaletteColor.TYPE_NORMAL
                colors.add(PaletteColor(code, code, hex, seriesOf(code), type, hex2))
            }
            if (colors.isNotEmpty()) {
                return listOf(
                    Palette(newUserId(), fallbackName.ifBlank { "导入色卡" }, colors, builtin = false)
                )
            }
        }
        if (root is JsonArray) {
            val colors = parseColorArray(root)
            if (colors.isNotEmpty()) {
                return listOf(
                    Palette(newUserId(), fallbackName.ifBlank { "导入色卡" }, colors, builtin = false)
                )
            }
        }
        return emptyList()
    }

    private fun parseColorArray(array: JsonArray): List<PaletteColor> {
        val out = ArrayList<PaletteColor>()
        for (el in array) {
            if (el !is JsonObject) continue
            val code = str(el["code"]) ?: str(el["num"]) ?: continue
            val hex = pickColor(el["hex"], el["color"], el["color1"]) ?: continue
            val hex2 = pickColor(el["hex2"], el["color2"])
            val type = str(el["type"]) ?: PaletteColor.TYPE_NORMAL
            val name = str(el["name"]) ?: code
            out.add(PaletteColor(code, name, hex, seriesOf(code), type, hex2))
        }
        return out
    }

    private fun str(el: JsonElement?): String? {
        if (el == null) return null
        val v = runCatching { el.jsonPrimitive.content }.getOrNull() ?: return null
        return v.takeIf { it.isNotBlank() && it != "null" }
    }

    private fun JsonPrimitive.strValue(): String? {
        val v = runCatching { content }.getOrNull() ?: return null
        return v.takeIf { it.isNotBlank() && it != "null" }
    }

    private fun pickColor(vararg elements: JsonElement?): String? {
        for (el in elements) {
            if (el == null) continue
            val p = runCatching { el.jsonPrimitive }.getOrNull() ?: continue
            val raw = p.strValue() ?: continue
            return ColorMath.toHex(ColorMath.parseHex(raw))
        }
        return null
    }

    private fun seriesOf(code: String): String {
        val letters = code.takeWhile { it.isLetter() }
        return if (letters.isNotEmpty()) letters.uppercase() else "?"
    }

    /** 解析 pindou-draw 的 CSV 色卡。 */
    fun parseCsv(text: String): List<PaletteColor> {
        val lines = text.split("\r\n", "\n", "\r").filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()
        val out = ArrayList<PaletteColor>()
        var start = 0
        val header = lines.first().split(",").map { it.trim().lowercase() }
        var codeIdx = 0
        var typeIdx = 1
        var colorIdx = 2
        var color2Idx = -1
        if (header.contains("num") || header.contains("code")) {
            codeIdx = header.indexOfFirst { it == "num" || it == "code" }.coerceAtLeast(0)
            typeIdx = header.indexOf("type").takeIf { it >= 0 } ?: 1
            colorIdx = header.indexOfFirst { it == "color1" || it == "color" || it == "hex" }
                .takeIf { it >= 0 } ?: 2
            color2Idx = header.indexOfFirst { it == "color2" || it == "hex2" }
            start = 1
        }
        for (i in start until lines.size) {
            val cols = lines[i].split(",")
            if (cols.size <= codeIdx) continue
            val code = cols[codeIdx].trim()
            if (code.isEmpty()) continue
            val colorRaw = cols.getOrNull(colorIdx)?.trim().orEmpty()
            if (colorRaw.isEmpty()) continue
            val type = cols.getOrNull(typeIdx)?.trim()?.takeIf { it.isNotEmpty() }
                ?: PaletteColor.TYPE_NORMAL
            val color2 = cols.getOrNull(color2Idx)?.trim()?.takeIf { it.isNotEmpty() }
            out.add(
                PaletteColor(
                    code = code,
                    name = code,
                    hex = ColorMath.toHex(ColorMath.parseHex(colorRaw)),
                    series = seriesOf(code),
                    type = type,
                    hex2 = color2?.let { ColorMath.toHex(ColorMath.parseHex(it)) },
                )
            )
        }
        return out
    }
}
