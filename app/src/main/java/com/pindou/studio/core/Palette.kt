package com.pindou.studio.core

/**
 * 一种拼豆颜色。
 *
 * @property code 色号，例如 "A1"（图纸与工程文件里用它作为稳定标识）
 * @property name 显示名称
 * @property hex 基础色 "#RRGGBB"
 * @property series 系列（A/B/C…），用于面板分组
 * @property type 颜色类型：normal / transparent / light / temperatrue / glow / pearlescent
 * @property hex2 双态色（光变 / 温变）的激活态颜色
 */
class PaletteColor(
    val code: String,
    val name: String,
    val hex: String,
    val series: String,
    val type: String,
    val hex2: String?,
) {
    /** 基础色 ARGB。 */
    val argb: Int by lazy(LazyThreadSafetyMode.NONE) { ColorMath.parseHex(hex) }

    /** 激活态 ARGB（无双态色时等于基础色）。 */
    val argb2: Int by lazy(LazyThreadSafetyMode.NONE) {
        hex2?.takeIf { it.isNotBlank() }?.let { ColorMath.parseHex(it) } ?: argb
    }

    /** OKLab 坐标，用于最近色匹配。 */
    val lab: FloatArray by lazy(LazyThreadSafetyMode.NONE) { ColorMath.rgbToOklab(argb) }

    val isSpecial: Boolean get() = type != TYPE_NORMAL
    val isDualState: Boolean get() = type == TYPE_LIGHT || type == TYPE_TEMPERATURE

    companion object {
        const val TYPE_NORMAL = "normal"
        const val TYPE_TRANSPARENT = "transparent"
        const val TYPE_LIGHT = "light"
        const val TYPE_TEMPERATURE = "temperatrue"
        const val TYPE_GLOW = "glow"
        const val TYPE_PEARLESCENT = "pearlescent"

        fun typeLabel(type: String): String = when (type) {
            TYPE_TRANSPARENT -> "透明"
            TYPE_LIGHT -> "光变"
            TYPE_TEMPERATURE -> "温变"
            TYPE_GLOW -> "夜光"
            TYPE_PEARLESCENT -> "珠光"
            else -> "普通"
        }
    }
}

/**
 * 一套色卡（品牌 / 店铺色号表）。
 *
 * [colors] 的下标 + 1 即画布单元格里存储的 1-based 颜色索引。
 */
class Palette(
    val id: String,
    val name: String,
    val colors: List<PaletteColor>,
    val builtin: Boolean = true,
) {
    /** 色号 → 1-based 索引。 */
    val indexByCode: Map<String, Int> = buildMap {
        colors.forEachIndexed { i, c -> putIfAbsent(c.code.uppercase(), i + 1) }
    }

    /** 色号 → 颜色。 */
    val colorByCode: Map<String, PaletteColor> = buildMap {
        colors.forEach { c -> putIfAbsent(c.code.uppercase(), c) }
    }

    val size: Int get() = colors.size

    /** 按 1-based 索引取颜色，越界返回 null。 */
    fun colorAt(index: Int): PaletteColor? =
        if (index in 1..colors.size) colors[index - 1] else null

    /** 按色号取 1-based 索引。 */
    fun indexOf(code: String): Int = indexByCode[code.uppercase()] ?: 0

    /** 按系列分组（保持首次出现顺序）。 */
    fun seriesGroups(): List<Pair<String, List<Pair<Int, PaletteColor>>>> {
        val order = LinkedHashMap<String, MutableList<Pair<Int, PaletteColor>>>()
        colors.forEachIndexed { i, c ->
            val key = c.series.ifBlank { "?" }
            order.getOrPut(key) { mutableListOf() }.add((i + 1) to c)
        }
        return order.map { it.key to it.value }
    }

    /** 复制成用户色卡（可编辑）。 */
    fun copyAsUser(id: String, name: String): Palette =
        Palette(id, name, colors.toList(), builtin = false)
}
