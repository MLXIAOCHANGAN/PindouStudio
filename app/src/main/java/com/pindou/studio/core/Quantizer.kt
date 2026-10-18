package com.pindou.studio.core

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** 单元格代表色的计算方式。 */
enum class QuantizeMode {
    /** 平均值：适合照片，过渡平滑。 */
    AVERAGE,

    /** 主色：适合插画，色块清晰。 */
    DOMINANT,
}

/** 抖动算法。 */
enum class DitherMode(val label: String) {
    NONE("无"),
    FLOYD_STEINBERG("误差扩散"),
    BLUE_NOISE("蓝噪声"),
}

/**
 * 以图生草图的参数。
 */
data class QuantizeOptions(
    /** 目标宽度（豆子数），高度按原图比例推导。 */
    val targetWidth: Int = 48,
    val mode: QuantizeMode = QuantizeMode.AVERAGE,
    val dither: DitherMode = DitherMode.NONE,
    /** 合并相近色阈值（OKLab 距离，0 表示关闭）。 */
    val mergeSimilarity: Float = 0f,
    /** 移除背景（从边缘泛洪清除主色）。 */
    val removeBackground: Boolean = false,
    /** 亮度 -100..100。 */
    val brightness: Float = 0f,
    /** 对比度 -100..100。 */
    val contrast: Float = 0f,
    /** 饱和度 -100..100。 */
    val saturation: Float = 0f,
    /** 排除的色号（大写）。 */
    val excludedCodes: Set<String> = emptySet(),
)

/**
 * 最近色匹配器：把 OKLab 样本预先算好，并对结果做精确 RGB 记忆化，
 * 因此每个唯一颜色只会做一次 O(N) 的搜寻。
 */
class PaletteMatcher(palette: Palette, excludedCodes: Set<String> = emptySet()) {

    private val indices: IntArray
    private val labs: Array<FloatArray>
    private val rgbOf: IntArray
    private val memo = HashMap<Int, Int>()

    init {
        val codes = ArrayList<Int>()
        val labList = ArrayList<FloatArray>()
        val rgbList = ArrayList<Int>()
        for ((i, color) in palette.colors.withIndex()) {
            if (color.code.uppercase() in excludedCodes) continue
            codes.add(i + 1)
            labList.add(color.lab)
            rgbList.add(color.argb)
        }
        indices = codes.toIntArray()
        labs = labList.toTypedArray()
        rgbOf = rgbList.toIntArray()
    }

    val isEmpty: Boolean get() = indices.isEmpty()

    /** 返回 1-based 调色板索引，无可用颜色时返回 0。 */
    fun nearest(r: Int, g: Int, b: Int): Int {
        if (indices.isEmpty()) return 0
        val key = (r shl 16) or (g shl 8) or b
        memo[key]?.let { return it }
        val target = ColorMath.rgbToOklab(r, g, b)
        var best = 0
        var bestD = Float.MAX_VALUE
        for (i in labs.indices) {
            val lab = labs[i]
            val dl = target[0] - lab[0]
            val da = target[1] - lab[1]
            val db = target[2] - lab[2]
            val d = dl * dl + da * da + db * db
            if (d < bestD) {
                bestD = d
                best = i
            }
        }
        val result = indices[best]
        memo[key] = result
        return result
    }

    /** 最近的调色板索引对应的实际 RGB（用于抖动误差计算）。 */
    fun nearestRgb(paletteIndex: Int): IntArray {
        for (i in indices.indices) {
            if (indices[i] == paletteIndex) {
                val rgb = rgbOf[i]
                return intArrayOf(
                    ColorMath.red(rgb),
                    ColorMath.green(rgb),
                    ColorMath.blue(rgb),
                )
            }
        }
        return intArrayOf(0, 0, 0)
    }
}

/**
 * 图形 → 拼豆网格的量化引擎。
 *
 * 流程与参考项目一致：先把整张图按目标格数降采样，每个格子内的像素收敛成
 * 一个代表色（平均值或主色），再映射到最近的色卡颜色；最后可选地合并相近色、
 * 移除背景、抖动。
 */
object Quantizer {

    private const val ALPHA_THRESHOLD = 128
    private const val MAX_SOURCE_SIDE = 2048
    private const val MIN_SOURCE_SIDE = 256
    private const val SAMPLES_PER_CELL = 12

    private val BLUE_NOISE = arrayOf(
        intArrayOf(0, 32, 8, 40, 2, 34, 10, 42),
        intArrayOf(48, 16, 56, 24, 50, 18, 58, 26),
        intArrayOf(12, 44, 4, 36, 14, 46, 6, 38),
        intArrayOf(60, 28, 52, 20, 62, 30, 54, 22),
        intArrayOf(3, 35, 11, 43, 1, 33, 9, 41),
        intArrayOf(51, 19, 59, 27, 49, 17, 57, 25),
        intArrayOf(15, 47, 7, 39, 13, 45, 5, 37),
        intArrayOf(63, 31, 55, 23, 61, 29, 53, 21),
    )

    /**
     * 按原图比例推导网格尺寸，并限制在画布上限内。
     */
    fun resolveGridSize(srcW: Int, srcH: Int, requestedWidth: Int): Pair<Int, Int> {
        var w = requestedWidth.coerceIn(1, BeadGrid.MAX_SIZE)
        var h = ((w.toFloat() * srcH) / srcW).roundToInt().coerceIn(1, BeadGrid.MAX_SIZE)
        if (w * h > BeadGrid.MAX_SIZE * BeadGrid.MAX_SIZE) {
            val scale = sqrt((BeadGrid.MAX_SIZE * BeadGrid.MAX_SIZE).toDouble() / (w * h)).toFloat()
            w = max(1, (w * scale).toInt())
            h = max(1, (h * scale).toInt())
        }
        return w to h
    }

    private fun resolvePrescaleCap(w: Int, h: Int): Int {
        val targetSide = max(w, h)
        return min(MAX_SOURCE_SIDE, max(MIN_SOURCE_SIDE, targetSide * SAMPLES_PER_CELL))
    }

    /**
     * 把位图量化为拼豆网格。
     */
    fun quantize(source: Bitmap, palette: Palette, options: QuantizeOptions): BeadGrid {
        val matcher = PaletteMatcher(palette, options.excludedCodes)
        val (w, h) = resolveGridSize(source.width, source.height, options.targetWidth)
        val grid = BeadGrid(w, h, ShortArray(w * h))
        if (matcher.isEmpty) return grid

        // 1) 降采样到合适大小，保证每格有足够的采样像素
        val cap = resolvePrescaleCap(w, h)
        val longSide = max(source.width, source.height)
        val scale = min(1f, cap.toFloat() / longSide)
        val sw = max(w, (source.width * scale).roundToInt())
        val sh = max(h, (source.height * scale).roundToInt())
        val small = if (sw == source.width && sh == source.height) {
            source
        } else {
            Bitmap.createScaledBitmap(source, sw, sh, true)
        }
        val pixels = IntArray(sw * sh)
        small.getPixels(pixels, 0, sw, 0, 0, sw, sh)

        // 2) 每格收敛为一个代表色
        val cellR = FloatArray(w * h)
        val cellG = FloatArray(w * h)
        val cellB = FloatArray(w * h)
        val hasCell = BooleanArray(w * h)

        val freq = HashMap<Int, Int>()
        for (r in 0 until h) {
            val y0 = r * sh / h
            val y1 = min(sh, max(y0 + 1, (r + 1) * sh / h))
            for (c in 0 until w) {
                val x0 = c * sw / w
                val x1 = min(sw, max(x0 + 1, (c + 1) * sw / w))
                var rSum = 0L
                var gSum = 0L
                var bSum = 0L
                var count = 0
                var domKey = 0
                var domCount = 0
                if (options.mode == QuantizeMode.DOMINANT) freq.clear()
                for (y in y0 until y1) {
                    var p = y * sw + x0
                    for (x in x0 until x1) {
                        val px = pixels[p++]
                        val a = (px ushr 24) and 0xFF
                        if (a < ALPHA_THRESHOLD) continue
                        val rr = (px ushr 16) and 0xFF
                        val gg = (px ushr 8) and 0xFF
                        val bb = px and 0xFF
                        count++
                        if (options.mode == QuantizeMode.DOMINANT) {
                            val key = (rr shl 16) or (gg shl 8) or bb
                            val n = (freq[key] ?: 0) + 1
                            freq[key] = n
                            if (n > domCount) { domCount = n; domKey = key }
                        } else {
                            rSum += rr; gSum += gg; bSum += bb
                        }
                    }
                }
                if (count == 0) continue
                val ci = r * w + c
                if (options.mode == QuantizeMode.DOMINANT) {
                    cellR[ci] = ((domKey ushr 16) and 0xFF).toFloat()
                    cellG[ci] = ((domKey ushr 8) and 0xFF).toFloat()
                    cellB[ci] = (domKey and 0xFF).toFloat()
                } else {
                    cellR[ci] = rSum.toFloat() / count
                    cellG[ci] = gSum.toFloat() / count
                    cellB[ci] = bSum.toFloat() / count
                }
                hasCell[ci] = true
            }
        }

        // 3) 亮度 / 对比度 / 饱和度调整
        if (options.brightness != 0f || options.contrast != 0f || options.saturation != 0f) {
            for (i in 0 until w * h) {
                if (!hasCell[i]) continue
                val adjusted = ColorMath.adjust(
                    cellR[i].toInt(), cellG[i].toInt(), cellB[i].toInt(),
                    options.brightness, options.contrast, options.saturation,
                )
                cellR[i] = ColorMath.red(adjusted).toFloat()
                cellG[i] = ColorMath.green(adjusted).toFloat()
                cellB[i] = ColorMath.blue(adjusted).toFloat()
            }
        }

        // 4) 抖动 + 最近色匹配
        for (r in 0 until h) {
            for (c in 0 until w) {
                val ci = r * w + c
                if (!hasCell[ci]) continue
                var rr = cellR[ci]
                var gg = cellG[ci]
                var bb = cellB[ci]

                if (options.dither == DitherMode.BLUE_NOISE) {
                    val noise = (BLUE_NOISE[r % 8][c % 8] / 64f) - 0.5f
                    rr = (rr + noise * 24f).coerceIn(0f, 255f)
                    gg = (gg + noise * 24f).coerceIn(0f, 255f)
                    bb = (bb + noise * 24f).coerceIn(0f, 255f)
                }

                val idx = matcher.nearest(rr.toInt(), gg.toInt(), bb.toInt())
                if (idx == 0) continue
                grid.set(c, r, idx)

                if (options.dither == DitherMode.FLOYD_STEINBERG) {
                    val target = matcher.nearestRgb(idx)
                    val errR = rr - target[0]
                    val errG = gg - target[1]
                    val errB = bb - target[2]
                    distributeError(cellR, cellG, cellB, hasCell, w, h, c, r, errR, errG, errB)
                }
            }
        }

        // 5) 合并相近色
        if (options.mergeSimilarity > 0f) {
            mergeSimilarColors(grid, palette, options.mergeSimilarity)
        }

        // 6) 移除背景
        if (options.removeBackground) {
            removeBackground(grid)
        }

        return grid
    }

    private fun distributeError(
        cellR: FloatArray, cellG: FloatArray, cellB: FloatArray,
        hasCell: BooleanArray, w: Int, h: Int, x: Int, y: Int,
        errR: Float, errG: Float, errB: Float,
    ) {
        fun apply(nx: Int, ny: Int, factor: Float) {
            if (nx < 0 || ny < 0 || nx >= w || ny >= h) return
            val i = ny * w + nx
            if (!hasCell[i]) return
            cellR[i] += errR * factor
            cellG[i] += errG * factor
            cellB[i] += errB * factor
        }
        apply(x + 1, y, 7f / 16f)
        apply(x - 1, y + 1, 3f / 16f)
        apply(x, y + 1, 5f / 16f)
        apply(x + 1, y + 1, 1f / 16f)
    }

    /** 把低频颜色合并到阈值内的高频颜色。 */
    fun mergeSimilarColors(grid: BeadGrid, palette: Palette, threshold: Float) {
        val usage = grid.colorUsage()
        if (usage.size < 2) return
        val replacement = HashMap<Int, Int>()
        for (i in usage.indices) {
            val highIdx = usage[i].first
            if (replacement.containsKey(highIdx)) continue
            val high = palette.colorAt(highIdx) ?: continue
            for (j in i + 1 until usage.size) {
                val lowIdx = usage[j].first
                if (replacement.containsKey(lowIdx)) continue
                val low = palette.colorAt(lowIdx) ?: continue
                if (ColorMath.oklabDistance(high.lab, low.lab) < threshold) {
                    replacement[lowIdx] = highIdx
                }
            }
        }
        if (replacement.isEmpty()) return
        for (i in grid.cells.indices) {
            val v = grid.cells[i].toInt()
            if (v == 0) continue
            val target = replacement[v]
            if (target != null) grid.cells[i] = target.toShort()
        }
    }

    /** 从四边泛洪移除背景主色。 */
    fun removeBackground(grid: BeadGrid) {
        val w = grid.width
        val h = grid.height
        if (w == 0 || h == 0) return
        val borderCounts = HashMap<Int, Int>()
        fun count(v: Int) {
            if (v == 0) return
            borderCounts[v] = (borderCounts[v] ?: 0) + 1
        }
        for (c in 0 until w) {
            count(grid.get(c, 0))
            count(grid.get(c, h - 1))
        }
        for (r in 1 until h - 1) {
            count(grid.get(0, r))
            count(grid.get(w - 1, r))
        }
        val bg = borderCounts.entries.maxByOrNull { it.value }?.key ?: return

        val visited = BooleanArray(w * h)
        val stack = IntArray(w * h)
        var top = 0
        for (r in 0 until h) {
            for (c in 0 until w) {
                val isBorder = r == 0 || r == h - 1 || c == 0 || c == w - 1
                if (isBorder && grid.get(c, r) == bg) {
                    val idx = r * w + c
                    if (!visited[idx]) { visited[idx] = true; stack[top++] = idx }
                }
            }
        }
        while (top > 0) {
            val idx = stack[--top]
            val r = idx / w
            val c = idx % w
            grid.cells[idx] = 0
            fun visit(nr: Int, nc: Int) {
                if (nr < 0 || nr >= h || nc < 0 || nc >= w) return
                val nIdx = nr * w + nc
                if (visited[nIdx]) return
                if (grid.cells[nIdx].toInt() != bg) return
                visited[nIdx] = true
                stack[top++] = nIdx
            }
            visit(r + 1, c); visit(r - 1, c); visit(r, c + 1); visit(r, c - 1)
        }
    }

    /** 把网格渲染成预览位图（每格 [cellPx] 像素）。 */
    fun renderPreview(grid: BeadGrid, palette: Palette, cellPx: Int = 4): Bitmap {
        val bmp = Bitmap.createBitmap(
            max(1, grid.width * cellPx),
            max(1, grid.height * cellPx),
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bmp)
        canvas.drawColor(0xFFFFFFFF.toInt())
        val paint = Paint().apply { isAntiAlias = false }
        for (r in 0 until grid.height) {
            for (c in 0 until grid.width) {
                val idx = grid.get(c, r)
                if (idx == 0) continue
                val color = palette.colorAt(idx) ?: continue
                paint.color = color.argb
                canvas.drawRect(
                    (c * cellPx).toFloat(), (r * cellPx).toFloat(),
                    ((c + 1) * cellPx).toFloat(), ((r + 1) * cellPx).toFloat(),
                    paint,
                )
            }
        }
        return bmp
    }
}
