package com.pindou.studio.core

import kotlin.math.max

/** SVG 导出设置。 */
data class SvgOptions(
    val cellPx: Int = 20,
    val showCodes: Boolean = false,
    val showGrid: Boolean = true,
    val showCoordinates: Boolean = true,
    val showMajorGrid: Boolean = true,
    val majorGridStep: Int = 8,
    val title: String = "",
)

/**
 * 把拼豆网格导出为矢量 SVG 图纸（同色相邻格合并成长条，避免文件过大）。
 */
object SvgExporter {

    private const val MAX_DIM = 20000

    fun render(grid: BeadGrid, palette: Palette, options: SvgOptions): String {
        val headerBand = if (options.showCoordinates) max(16, (options.cellPx * 0.9f).toInt()) else 0
        val titleHeight = if (options.title.isNotBlank()) 34 else 0
        var cell = options.cellPx.coerceAtLeast(2)
        while (headerBand * 2 + grid.width * cell > MAX_DIM || headerBand * 2 + grid.height * cell > MAX_DIM) {
            if (cell <= 2) break
            cell = (cell * 0.9f).toInt().coerceAtLeast(2)
        }
        val boardW = grid.width * cell
        val boardH = grid.height * cell
        val width = boardW + headerBand * 2
        val height = boardH + headerBand * 2 + titleHeight
        val originX = headerBand
        val originY = headerBand + titleHeight

        val sb = StringBuilder(1 shl 16)
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"$width\" height=\"$height\" ")
        sb.append("viewBox=\"0 0 $width $height\">\n")
        sb.append("<rect width=\"100%\" height=\"100%\" fill=\"#FFFFFF\"/>\n")
        if (options.title.isNotBlank()) {
            sb.append("<text x=\"${width / 2}\" y=\"24\" font-family=\"sans-serif\" font-size=\"18\" ")
            sb.append("font-weight=\"bold\" fill=\"#3F4652\" text-anchor=\"middle\">")
            sb.append(escape(options.title)).append("</text>\n")
        }

        // 同色横向合并
        val runs = StringBuilder()
        for (r in 0 until grid.height) {
            var c = 0
            while (c < grid.width) {
                val idx = grid.get(c, r)
                if (idx == 0) { c++; continue }
                var end = c + 1
                while (end < grid.width && grid.get(end, r) == idx) end++
                val color = palette.colorAt(idx)
                if (color != null) {
                    val x = originX + c * cell
                    val y = originY + r * cell
                    runs.append("<rect x=\"$x\" y=\"$y\" width=\"${(end - c) * cell}\" height=\"$cell\" fill=\"")
                    runs.append(ColorMath.toHex(color.argb)).append("\"/>\n")
                }
                c = end
            }
        }
        sb.append(runs)

        if (options.showGrid) {
            sb.append("<g stroke=\"#BFC4CB\" stroke-width=\"0.6\" shape-rendering=\"crispEdges\">\n")
            for (c in 0..grid.width) {
                val x = originX + c * cell
                sb.append("<line x1=\"$x\" y1=\"$originY\" x2=\"$x\" y2=\"${originY + boardH}\"/>\n")
            }
            for (r in 0..grid.height) {
                val y = originY + r * cell
                sb.append("<line x1=\"$originX\" y1=\"$y\" x2=\"${originX + boardW}\" y2=\"$y\"/>\n")
            }
            sb.append("</g>\n")
            if (options.showMajorGrid && options.majorGridStep > 0) {
                sb.append("<g stroke=\"#7A8390\" stroke-width=\"1.4\" shape-rendering=\"crispEdges\">\n")
                var c = 0
                while (c <= grid.width) {
                    if (c > 0 && c % options.majorGridStep == 0) {
                        val x = originX + c * cell
                        sb.append("<line x1=\"$x\" y1=\"$originY\" x2=\"$x\" y2=\"${originY + boardH}\"/>\n")
                    }
                    c++
                }
                var r = 0
                while (r <= grid.height) {
                    if (r > 0 && r % options.majorGridStep == 0) {
                        val y = originY + r * cell
                        sb.append("<line x1=\"$originX\" y1=\"$y\" x2=\"${originX + boardW}\" y2=\"$y\"/>\n")
                    }
                    r++
                }
                sb.append("</g>\n")
            }
        }

        sb.append("<rect x=\"$originX\" y=\"$originY\" width=\"$boardW\" height=\"$boardH\" fill=\"none\" stroke=\"#9AA3AE\" stroke-width=\"2\"/>\n")

        if (options.showCodes && cell >= 11) {
            sb.append("<g font-family=\"monospace\" font-size=\"${(cell * 0.5f).toInt().coerceAtLeast(6)}\" ")
            sb.append("text-anchor=\"middle\" dominant-baseline=\"central\">\n")
            for (r in 0 until grid.height) {
                for (c in 0 until grid.width) {
                    val idx = grid.get(c, r)
                    if (idx == 0) continue
                    val color = palette.colorAt(idx) ?: continue
                    val fill = if (ColorMath.luminance(color.argb) > 0.45f) "#111111" else "#FFFFFF"
                    val x = originX + c * cell + cell / 2f
                    val y = originY + r * cell + cell / 2f
                    sb.append("<text x=\"${x.toInt()}\" y=\"${y.toInt()}\" fill=\"$fill\">")
                    sb.append(escape(color.code)).append("</text>\n")
                }
            }
            sb.append("</g>\n")
        }

        if (options.showCoordinates && headerBand > 0) {
            val step = if (cell >= 14) 1 else if (cell >= 8) 2 else 5
            val fontSize = minOf(12, max(7, (cell * 0.8f).toInt()))
            sb.append("<g font-family=\"monospace\" font-size=\"$fontSize\" fill=\"#3F4652\" text-anchor=\"middle\">\n")
            for (c in 0 until grid.width) {
                val global = c + 1
                if ((global - 1) % step != 0 && global != grid.width) continue
                val x = originX + c * cell + cell / 2f
                sb.append("<text x=\"${x.toInt()}\" y=\"${originY - headerBand / 2 + 4}\">$global</text>\n")
                sb.append("<text x=\"${x.toInt()}\" y=\"${originY + boardH + headerBand / 2 + 4}\">$global</text>\n")
            }
            for (r in 0 until grid.height) {
                val global = r + 1
                if ((global - 1) % step != 0 && global != grid.height) continue
                val y = originY + r * cell + cell / 2f + 4
                sb.append("<text x=\"${originX - headerBand / 2}\" y=\"${y.toInt()}\">$global</text>\n")
                sb.append("<text x=\"${originX + boardW + headerBand / 2}\" y=\"${y.toInt()}\">$global</text>\n")
            }
            sb.append("</g>\n")
        }

        sb.append("</svg>\n")
        return sb.toString()
    }

    /** 导出色号用量清单（CSV，便于按需购买豆子）。 */
    fun usageCsv(grid: BeadGrid, palette: Palette): String {
        val sb = StringBuilder()
        sb.append("色号,数量\n")
        val rows = grid.colorUsage()
            .mapNotNull { (idx, count) ->
                val color = palette.colorAt(idx) ?: return@mapNotNull null
                Triple(color.code, palette.indexOf(color.code), count)
            }
            .sortedBy { it.second }
        for ((code, _, count) in rows) {
            sb.append(code).append(',').append(count).append('\n')
        }
        sb.append("合计,").append(grid.paintedCount()).append('\n')
        return sb.toString()
    }

    private fun escape(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
