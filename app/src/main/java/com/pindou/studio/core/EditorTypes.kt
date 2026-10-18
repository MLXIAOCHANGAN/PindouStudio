package com.pindou.studio.core

/** 绘制工具。 */
enum class Tool(val label: String) {
    PEN("画笔"),
    ERASER("橡皮"),
    FILL("油漆桶"),
    EYEDROPPER("吸管"),
    SELECT("选区"),
    PAN("移动画布"),
}

/** 对称绘制模式。 */
enum class SymmetryMode(val label: String) {
    NONE("无"),
    HORIZONTAL("左右"),
    VERTICAL("上下"),
    QUAD("四向"),
    DIAGONAL("对角"),
}

/** 画布光效显示模式。 */
enum class DisplayMode(val key: String, val label: String) {
    STANDARD("standard", "标准"),
    NIGHT("night", "夜间"),
    TEMPERATURE("temperatrue", "温变"),
    LIGHT("light", "光变"),
    SPECIAL("special", "特殊");

    companion object {
        fun of(key: String): DisplayMode = entries.firstOrNull { it.key == key } ?: STANDARD
    }
}

/** 根据显示模式解析某个颜色最终应显示的颜色。 */
object DisplayResolver {

    fun resolve(color: PaletteColor, mode: DisplayMode): Int {
        val base = color.argb
        val activated = color.argb2
        var out = when (mode) {
            DisplayMode.STANDARD -> base
            DisplayMode.NIGHT -> if (color.type == PaletteColor.TYPE_GLOW) base else ColorMath.scale(base, 0.5f)
            DisplayMode.LIGHT -> if (color.type == PaletteColor.TYPE_LIGHT) activated else base
            DisplayMode.TEMPERATURE -> if (color.type == PaletteColor.TYPE_TEMPERATURE) activated else base
            DisplayMode.SPECIAL -> if (color.isDualState) activated else base
        }
        if (color.type == PaletteColor.TYPE_TRANSPARENT) {
            out = (out and 0x00FFFFFF) or (0x60 shl 24)
        } else if (color.type == PaletteColor.TYPE_PEARLESCENT && mode != DisplayMode.STANDARD) {
            out = ColorMath.mix(out, 0xFFFFFFFF.toInt(), 0.18f)
        }
        return out
    }
}

/** 选区（含端点，闭区间）。 */
data class SelectionRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = right - left + 1
    val height: Int get() = bottom - top + 1

    fun contains(c: Int, r: Int): Boolean = c in left..right && r in top..bottom

    fun normalized(): SelectionRect = SelectionRect(
        minOf(left, right), minOf(top, bottom), maxOf(left, right), maxOf(top, bottom),
    )

    companion object {
        fun fromPoints(c0: Int, r0: Int, c1: Int, r1: Int) =
            SelectionRect(minOf(c0, c1), minOf(r0, r1), maxOf(c0, c1), maxOf(r0, r1))
    }
}
