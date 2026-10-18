package com.pindou.studio.data

import android.util.Base64
import com.pindou.studio.core.BeadGrid
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

/** 网格 ↔ 紧凑字符串（Base64 + GZIP）。 */
object Codec {

    fun encodeCells(grid: BeadGrid): String {
        val raw = ByteArray(grid.cells.size * 2)
        for (i in grid.cells.indices) {
            val v = grid.cells[i].toInt()
            raw[i * 2] = (v and 0xFF).toByte()
            raw[i * 2 + 1] = ((v ushr 8) and 0xFF).toByte()
        }
        val out = ByteArrayOutputStream()
        GZIPOutputStream(out).use { it.write(raw) }
        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }

    fun decodeCells(data: String, width: Int, height: Int): BeadGrid {
        val grid = BeadGrid(width.coerceIn(1, BeadGrid.MAX_SIZE), height.coerceIn(1, BeadGrid.MAX_SIZE))
        if (data.isBlank()) return grid
        return try {
            val compressed = Base64.decode(data, Base64.NO_WRAP)
            val raw = GZIPInputStream(ByteArrayInputStream(compressed)).use { it.readBytes() }
            val count = minOf(grid.cells.size, raw.size / 2)
            for (i in 0 until count) {
                val lo = raw[i * 2].toInt() and 0xFF
                val hi = raw[i * 2 + 1].toInt() and 0xFF
                grid.cells[i] = ((hi shl 8) or lo).toShort()
            }
            grid
        } catch (e: Exception) {
            grid
        }
    }
}
