package com.pindou.studio.core

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** 图纸导出设置。 */
data class ChartOptions(
    /** 每颗豆子的像素数。 */
    val cellPx: Int = 22,
    /** 显示色号。 */
    val showCodes: Boolean = false,
    /** 显示坐标轴（四周行列编号）。 */
    val showCoordinates: Boolean = true,
    /** 显示网格线。 */
    val showGrid: Boolean = true,
    /** 显示加粗主网格。 */
    val showMajorGrid: Boolean = true,
    /** 主网格间隔（格）。 */
    val majorGridStep: Int = 8,
    /** 附加用量列表。 */
    val showBeadStats: Boolean = true,
    /** 顶部标题。 */
    val title: String = "",
    /** 背景色。 */
    val backgroundColor: Int = 0xFFFFFFFF.toInt(),
    /** 分块导出数量：1 / 4 / 9 / 16。 */
    val tileCount: Int = 1,
    /** 水印文字，空表示不加水印。 */
    val watermark: String = "",
    /** 是否把彩色像素画成圆形豆子。 */
    val roundBeads: Boolean = false,
)

/** 一张导出的图纸。 */
data class ChartTile(
    /** 第几张（1 开始）。 */
    val index: Int,
    val count: Int,
    val bitmap: Bitmap,
)

/**
 * 把拼豆网格渲染成可打印的 PNG 图纸：白色背景 + 网格线 + 四周坐标编号 +
 * 可选色号标签 + 可选用量清单，并支持 1/4/9/16 张分块导出。
 */
object ChartExporter {

    /** 单张图纸的最大边长（像素），避免超大位图 OOM。 */
    private const val MAX_DIM = 3200
    private const val GRID_COLOR = 0xFFBFC4CB.toInt()
    private const val MAJOR_GRID_COLOR = 0xFF7A8390.toInt()
    private const val LABEL_COLOR = 0xFF3F4652.toInt()
    private const val HEADER_BG = 0xFFF1F3F5.toInt()

    fun render(grid: BeadGrid, palette: Palette, options: ChartOptions): List<ChartTile> {
        val count = when (options.tileCount) {
            4, 9, 16 -> options.tileCount
            else -> 1
        }
        val cols = sqrt(count.toDouble()).roundToInt().coerceAtLeast(1)
        val rows = (count + cols - 1) / cols
        val tiles = ArrayList<ChartTile>(count)
        for (ty in 0 until rows) {
            for (tx in 0 until cols) {
                val index = ty * cols + tx + 1
                if (index > count) continue
                val c0 = grid.width * tx / cols
                val c1 = grid.width * (tx + 1) / cols
                val r0 = grid.height * ty / rows
                val r1 = grid.height * (ty + 1) / rows
                val w = max(1, c1 - c0)
                val h = max(1, r1 - r0)
                val bitmap = renderWindow(grid, palette, options, c0, r0, w, h)
                tiles.add(ChartTile(index, count, bitmap))
            }
        }
        return tiles
    }

    /** 渲染其中一个窗口区域。 */
    fun renderWindow(
        grid: BeadGrid,
        palette: Palette,
        options: ChartOptions,
        colStart: Int,
        rowStart: Int,
        cols: Int,
        rows: Int,
    ): Bitmap {
        // 根据画布上限反推每颗豆子的像素数
        val headerFont = 12f
        val statsRows = if (options.showBeadStats) {
            val used = usedColors(grid, palette)
            max(1, ceil(used.size / 6.0).toInt())
        } else 0
        val statsHeight = if (options.showBeadStats) 34 + statsRows * 22 else 0
        val titleHeight = if (options.title.isNotBlank()) 34 else 0

        fun bandOf(cp: Int) = if (options.showCoordinates) max(16, (cp * 0.9f).roundToInt()) else 0
        fun totalWidth(cp: Int) = bandOf(cp) * 2 + cols * cp
        fun totalHeight(cp: Int) = titleHeight + bandOf(cp) * 2 + rows * cp + statsHeight

        var cellPx = options.cellPx.coerceAtLeast(2)
        while ((totalWidth(cellPx) > MAX_DIM || totalHeight(cellPx) > MAX_DIM) && cellPx > 2) {
            val next = (cellPx * 0.9f).toInt()
            cellPx = if (next >= cellPx) cellPx - 1 else next
            if (cellPx < 2) { cellPx = 2; break }
        }

        val headerBand = bandOf(cellPx)
        val boardW = cols * cellPx
        val boardH = rows * cellPx
        val width = boardW + headerBand * 2
        val height = boardH + headerBand * 2 + statsHeight + titleHeight

        val bmp = Bitmap.createBitmap(max(1, width), max(1, height), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(options.backgroundColor)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val boardLeft = headerBand.toFloat()
        val boardTop = (headerBand + titleHeight).toFloat()

        // 标题
        if (options.title.isNotBlank()) {
            paint.color = LABEL_COLOR
            paint.textSize = 18f
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(options.title, width / 2f, 24f, paint)
        }

        // 坐标带背景
        if (options.showCoordinates && headerBand > 0) {
            paint.color = HEADER_BG
            paint.style = Paint.Style.FILL
            canvas.drawRect(0f, boardTop - headerBand, width.toFloat(), boardTop, paint)
            canvas.drawRect(0f, boardTop + boardH, width.toFloat(), boardTop + boardH + headerBand, paint)
            canvas.drawRect(0f, boardTop, boardLeft, boardTop + boardH, paint)
            canvas.drawRect(boardLeft + boardW, boardTop, (boardLeft + boardW + headerBand), boardTop + boardH, paint)
        }

        // 豆子
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = options.roundBeads
        val radius = cellPx / 2f - 0.5f
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val idx = grid.get(colStart + c, rowStart + r)
                if (idx == 0) continue
                val color = palette.colorAt(idx) ?: continue
                paint.color = color.argb
                val left = boardLeft + c * cellPx
                val top = boardTop + r * cellPx
                if (options.roundBeads) {
                    canvas.drawCircle(left + cellPx / 2f, top + cellPx / 2f, radius, paint)
                } else {
                    canvas.drawRect(left, top, left + cellPx, top + cellPx, paint)
                }
            }
        }

        // 网格线
        if (options.showGrid && cellPx >= 4) {
            paint.style = Paint.Style.STROKE
            paint.isAntiAlias = false
            paint.strokeWidth = if (cellPx >= 12) 1f else 0.6f
            paint.color = GRID_COLOR
            for (c in 0..cols) {
                val x = boardLeft + c * cellPx
                canvas.drawLine(x, boardTop, x, boardTop + boardH, paint)
            }
            for (r in 0..rows) {
                val y = boardTop + r * cellPx
                canvas.drawLine(boardLeft, y, boardLeft + boardW, y, paint)
            }
            if (options.showMajorGrid && options.majorGridStep > 0 && cellPx >= 6) {
                paint.color = MAJOR_GRID_COLOR
                paint.strokeWidth = if (cellPx >= 16) 2f else 1.4f
                var c = 0
                while (c <= cols) {
                    if (colStart + c > 0 && (colStart + c) % options.majorGridStep == 0) {
                        val x = boardLeft + c * cellPx
                        canvas.drawLine(x, boardTop, x, boardTop + boardH, paint)
                    }
                    c++
                }
                var r = 0
                while (r <= rows) {
                    if (rowStart + r > 0 && (rowStart + r) % options.majorGridStep == 0) {
                        val y = boardTop + r * cellPx
                        canvas.drawLine(boardLeft, y, boardLeft + boardW, y, paint)
                    }
                    r++
                }
            }
            paint.isAntiAlias = true
        }

        // 色号标签
        if (options.showCodes && cellPx >= 11) {
            paint.style = Paint.Style.FILL
            paint.typeface = Typeface.MONOSPACE
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = min(cellPx * 0.55f, cellPx - 2f).coerceAtLeast(6f)
            val metrics = paint.fontMetrics
            val offset = -(metrics.ascent + metrics.descent) / 2f
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val idx = grid.get(colStart + c, rowStart + r)
                    if (idx == 0) continue
                    val color = palette.colorAt(idx) ?: continue
                    val text = color.code
                    if (text.length > 4 && cellPx < 20) continue
                    paint.color = if (ColorMath.luminance(color.argb) > 0.5f) 0xFF111111.toInt()
                    else 0xFFFFFFFF.toInt()
                    canvas.drawText(
                        text,
                        boardLeft + c * cellPx + cellPx / 2f,
                        boardTop + r * cellPx + cellPx / 2f + offset,
                        paint,
                    )
                }
            }
        }

        // 坐标编号
        if (options.showCoordinates && headerBand > 0) {
            paint.style = Paint.Style.FILL
            paint.typeface = Typeface.MONOSPACE
            paint.color = LABEL_COLOR
            paint.textAlign = Paint.Align.CENTER
            val step = if (cellPx >= 14) 1 else if (cellPx >= 8) 2 else 5
            paint.textSize = min(headerFont, cellPx * 0.8f).coerceIn(7f, 15f)
            val metrics = paint.fontMetrics
            val voffset = -(metrics.ascent + metrics.descent) / 2f
            for (c in 0 until cols) {
                val globalCol = colStart + c + 1
                if ((globalCol - 1) % step != 0 && globalCol != colStart + cols) continue
                val x = boardLeft + c * cellPx + cellPx / 2f
                canvas.drawText(globalCol.toString(), x, boardTop - headerBand / 2f + voffset, paint)
                canvas.drawText(globalCol.toString(), x, boardTop + boardH + headerBand / 2f + voffset, paint)
            }
            for (r in 0 until rows) {
                val globalRow = rowStart + r + 1
                if ((globalRow - 1) % step != 0 && globalRow != rowStart + rows) continue
                val y = boardTop + r * cellPx + cellPx / 2f + voffset
                canvas.drawText(globalRow.toString(), boardLeft - headerBand / 2f, y, paint)
                canvas.drawText(globalRow.toString(), boardLeft + boardW + headerBand / 2f, y, paint)
            }
        }

        // 用量统计
        if (options.showBeadStats) {
            drawStats(canvas, paint, grid, palette, boardLeft, boardTop + boardH + headerBand + 8f, width.toFloat())
        }

        // 水印
        if (options.watermark.isNotBlank()) {
            paint.style = Paint.Style.FILL
            paint.color = 0x22000000
            paint.textSize = max(24f, width / 22f)
            paint.typeface = Typeface.DEFAULT_BOLD
            paint.textAlign = Paint.Align.CENTER
            canvas.save()
            canvas.rotate(-30f, width / 2f, height / 2f)
            canvas.drawText(options.watermark, width / 2f, height / 2f, paint)
            canvas.restore()
        }

        return bmp
    }

    private data class UsedColor(val code: String, val argb: Int, val count: Int)

    private fun usedColors(grid: BeadGrid, palette: Palette): List<UsedColor> =
        grid.colorUsage().mapNotNull { (idx, count) ->
            val c = palette.colorAt(idx) ?: return@mapNotNull null
            UsedColor(c.code, c.argb, count)
        }.sortedBy { palette.indexOf(it.code) }

    private fun drawStats(
        canvas: Canvas,
        paint: Paint,
        grid: BeadGrid,
        palette: Palette,
        left: Float,
        top: Float,
        width: Float,
    ) {
        val used = usedColors(grid, palette)
        if (used.isEmpty()) return
        paint.textAlign = Paint.Align.LEFT
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.color = LABEL_COLOR
        paint.textSize = 15f
        paint.style = Paint.Style.FILL
        canvas.drawText("用量统计  共 ${grid.paintedCount()} 颗 / ${used.size} 色", left, top + 13f, paint)

        val itemW = 108f
        val perRow = max(1, ((width - left * 2) / itemW).toInt())
        paint.typeface = Typeface.MONOSPACE
        paint.textSize = 12f
        var i = 0
        for (item in used) {
            val row = i / perRow
            val col = i % perRow
            val x = left + col * itemW
            val y = top + 26f + row * 22f
            paint.color = item.argb
            canvas.drawRect(x, y, x + 14f, y + 14f, paint)
            paint.color = LABEL_COLOR
            canvas.drawRect(x, y, x + 14f, y + 14f, paint.apply { style = Paint.Style.STROKE; strokeWidth = 1f })
            paint.style = Paint.Style.FILL
            canvas.drawText("${item.code} ×${item.count}", x + 20f, y + 12f, paint)
            i++
        }
    }
}
