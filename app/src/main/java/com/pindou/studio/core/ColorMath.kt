package com.pindou.studio.core

import kotlin.math.cbrt
import kotlin.math.pow

/**
 * 颜色数学工具：十六进制解析、sRGB / OKLab 转换与感知色差计算。
 *
 * OKLab 是感知均匀色彩空间，两种颜色在 OKLab 中的欧氏距离越小，
 * 人眼看起来越接近 —— 因此取最近色即可得到视觉上最相似的颜色。
 */
object ColorMath {

    /** 把 "#RRGGBB" / "#AARRGGBB" / "rgb(r,g,b)" 解析为 ARGB Int。失败返回黑。 */
    fun parseHex(value: String?): Int {
        if (value.isNullOrBlank()) return 0xFF000000.toInt()
        val v = value.trim()
        if (v.startsWith("#")) {
            val hex = v.substring(1)
            return when (hex.length) {
                3 -> {
                    val r = hex[0].digitToIntOrNull(16) ?: 0
                    val g = hex[1].digitToIntOrNull(16) ?: 0
                    val b = hex[2].digitToIntOrNull(16) ?: 0
                    argb(0xFF, r * 17, g * 17, b * 17)
                }
                6 -> ((0xFF shl 24) or (hex.toLong(16).toInt()))
                8 -> hex.toLong(16).toInt()
                else -> 0xFF000000.toInt()
            }
        }
        val m = RGB_REGEX.find(v)
        if (m != null) {
            val r = m.groupValues[1].toFloatOrNull()?.toInt() ?: 0
            val g = m.groupValues[2].toFloatOrNull()?.toInt() ?: 0
            val b = m.groupValues[3].toFloatOrNull()?.toInt() ?: 0
            return argb(0xFF, r, g, b)
        }
        return 0xFF000000.toInt()
    }

    private val RGB_REGEX = Regex("""rgba?\(\s*(\d+(?:\.\d+)?)\s*,\s*(\d+(?:\.\d+)?)\s*,\s*(\d+(?:\.\d+)?)""", RegexOption.IGNORE_CASE)

    /** 组装 ARGB。 */
    fun argb(a: Int, r: Int, g: Int, b: Int): Int =
        (clamp255(a) shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)

    fun clamp255(v: Int): Int = if (v < 0) 0 else if (v > 255) 255 else v

    fun alpha(argb: Int): Int = (argb ushr 24) and 0xFF
    fun red(argb: Int): Int = (argb ushr 16) and 0xFF
    fun green(argb: Int): Int = (argb ushr 8) and 0xFF
    fun blue(argb: Int): Int = argb and 0xFF

    /** 转成 "#RRGGBB" 文本。 */
    fun toHex(argb: Int): String {
        val r = red(argb)
        val g = green(argb)
        val b = blue(argb)
        return "#%02X%02X%02X".format(r, g, b)
    }

    private fun linearize(c: Double): Double =
        if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    /**
     * sRGB(0-255) → OKLab。
     * @return FloatArray(l, a, b)
     */
    fun rgbToOklab(r: Int, g: Int, b: Int): FloatArray {
        val lr = linearize(clamp255(r) / 255.0)
        val lg = linearize(clamp255(g) / 255.0)
        val lb = linearize(clamp255(b) / 255.0)

        val l = 0.4122214708 * lr + 0.5363325363 * lg + 0.0514459929 * lb
        val m = 0.2119034982 * lr + 0.6806995451 * lg + 0.1073969566 * lb
        val s = 0.0883024619 * lr + 0.2817188376 * lg + 0.6299787005 * lb

        val l_ = cbrt(l)
        val m_ = cbrt(m)
        val s_ = cbrt(s)

        return floatArrayOf(
            (0.2104542553 * l_ + 0.7936177850 * m_ - 0.0040720468 * s_).toFloat(),
            (1.9779984951 * l_ - 2.4285922050 * m_ + 0.4505937099 * s_).toFloat(),
            (0.0259040371 * l_ + 0.7827717662 * m_ - 0.8086757660 * s_).toFloat(),
        )
    }

    fun rgbToOklab(argb: Int): FloatArray = rgbToOklab(red(argb), green(argb), blue(argb))

    /** 两色在 OKLab 中的欧氏距离。 */
    fun oklabDistance(a: FloatArray, b: FloatArray): Float {
        val dl = a[0] - b[0]
        val da = a[1] - b[1]
        val db = a[2] - b[2]
        return kotlin.math.sqrt(dl * dl + da * da + db * db)
    }

    /** HSV 调整亮度 / 对比度 / 饱和度（-100..100），返回新的 ARGB。 */
    fun adjust(r: Int, g: Int, b: Int, brightness: Float, contrast: Float, saturation: Float): Int {
        var rf = r / 255f
        var gf = g / 255f
        var bf = b / 255f

        // 对比度（围绕 0.5 拉伸）
        val c = contrast / 100f
        val cf = (1f + c).let { if (it < 0f) 0f else it }
        rf = ((rf - 0.5f) * cf) + 0.5f
        gf = ((gf - 0.5f) * cf) + 0.5f
        bf = ((bf - 0.5f) * cf) + 0.5f

        // 亮度
        val bo = brightness / 100f
        rf += bo
        gf += bo
        bf += bo

        // 饱和度
        val s = saturation / 100f
        val sf = 1f + s
        val lum = 0.2126f * rf + 0.7152f * gf + 0.0722f * bf
        rf = lum + (rf - lum) * sf
        gf = lum + (gf - lum) * sf
        bf = lum + (bf - lum) * sf

        return argb(0xFF, (rf * 255f).toInt(), (gf * 255f).toInt(), (bf * 255f).toInt())
    }

    /** 计算一个颜色的“亮度”，用于判断黑字还是白字。 */
    fun luminance(argb: Int): Float {
        val r = linearize(red(argb) / 255.0)
        val g = linearize(green(argb) / 255.0)
        val b = linearize(blue(argb) / 255.0)
        return (0.2126 * r + 0.7152 * g + 0.0722 * b).toFloat()
    }

    /** 混合两个颜色（t = 0 返回 a，t = 1 返回 b）。 */
    fun mix(a: Int, b: Int, t: Float): Int {
        val tt = t.coerceIn(0f, 1f)
        return argb(
            0xFF,
            (red(a) + (red(b) - red(a)) * tt).toInt(),
            (green(a) + (green(b) - green(a)) * tt).toInt(),
            (blue(a) + (blue(b) - blue(a)) * tt).toInt(),
        )
    }

    /** 加深（factor < 1 变暗）。 */
    fun scale(argb: Int, factor: Float): Int = argb(
        alpha(argb),
        (red(argb) * factor).toInt(),
        (green(argb) * factor).toInt(),
        (blue(argb) * factor).toInt(),
    )
}
