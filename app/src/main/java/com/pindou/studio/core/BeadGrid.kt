package com.pindou.studio.core

/**
 * 稠密拼豆网格：width × height 个格子，每格存 1-based 颜色索引，0 表示空。
 *
 * 与参考项目一样，画布尺寸上限 1024×1024（约 100 万格）。
 */
class BeadGrid(
    val width: Int,
    val height: Int,
    val cells: ShortArray = ShortArray(width * height),
) {
    init {
        require(width in 1..MAX_SIZE) { "width out of range: $width" }
        require(height in 1..MAX_SIZE) { "height out of range: $height" }
    }

    val size: Int get() = width * height

    fun inBounds(c: Int, r: Int): Boolean = c in 0 until width && r in 0 until height

    fun get(c: Int, r: Int): Int {
        if (!inBounds(c, r)) return 0
        return cells[r * width + c].toInt()
    }

    fun set(c: Int, r: Int, value: Int) {
        if (!inBounds(c, r)) return
        cells[r * width + c] = value.toShort()
    }

    fun isEmptyAt(c: Int, r: Int): Boolean = get(c, r) == 0

    fun clear() {
        cells.fill(0)
    }

    fun copy(): BeadGrid = BeadGrid(width, height, cells.copyOf())

    /** 用另一张同尺寸网格的内容覆盖自身（保持对象标识不变）。 */
    fun restoreFrom(other: BeadGrid) {
        if (other.width == width && other.height == height) {
            other.cells.copyInto(cells)
        }
    }

    /** 已绘制格子的数量。 */
    fun paintedCount(): Int {
        var n = 0
        for (v in cells) if (v.toInt() != 0) n++
        return n
    }

    /** 已绘制区域的包围盒（含端点），没有绘制内容返回 null。 */
    fun bounds(): IntArray? {
        var minC = width
        var maxC = -1
        var minR = height
        var maxR = -1
        for (r in 0 until height) {
            val base = r * width
            for (c in 0 until width) {
                if (cells[base + c].toInt() != 0) {
                    if (c < minC) minC = c
                    if (c > maxC) maxC = c
                    if (r < minR) minR = r
                    if (r > maxR) maxR = r
                }
            }
        }
        if (maxC < 0) return null
        return intArrayOf(minC, minR, maxC, maxR)
    }

    /** 每种颜色的用量（1-based 索引 → 数量），按数量降序。 */
    fun colorUsage(): List<Pair<Int, Int>> {
        val counts = HashMap<Int, Int>()
        for (v in cells) {
            val idx = v.toInt()
            if (idx != 0) counts[idx] = (counts[idx] ?: 0) + 1
        }
        return counts.entries
            .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
            .map { it.key to it.value }
    }

    /** 用量最多的颜色索引，空画布返回 0。 */
    fun dominantColor(): Int = colorUsage().firstOrNull()?.first ?: 0

    /** 4 邻域泛洪填充；[replaceIndex] 为 0 表示擦除。返回被改动的格子数。 */
    fun floodFill(startC: Int, startR: Int, replaceIndex: Int): Int {
        if (!inBounds(startC, startR)) return 0
        val target = get(startC, startR)
        if (target == replaceIndex) return 0
        var changed = 0
        val stack = IntArray(size)
        var top = 0
        stack[top++] = startR * width + startC
        val visited = BooleanArray(size)
        visited[startR * width + startC] = true
        while (top > 0) {
            val idx = stack[--top]
            val r = idx / width
            val c = idx % width
            if (cells[idx].toInt() != target) continue
            cells[idx] = replaceIndex.toShort()
            changed++
            if (c > 0) {
                val n = idx - 1
                if (!visited[n] && cells[n].toInt() == target) { visited[n] = true; stack[top++] = n }
            }
            if (c < width - 1) {
                val n = idx + 1
                if (!visited[n] && cells[n].toInt() == target) { visited[n] = true; stack[top++] = n }
            }
            if (r > 0) {
                val n = idx - width
                if (!visited[n] && cells[n].toInt() == target) { visited[n] = true; stack[top++] = n }
            }
            if (r < height - 1) {
                val n = idx + width
                if (!visited[n] && cells[n].toInt() == target) { visited[n] = true; stack[top++] = n }
            }
        }
        return changed
    }

    /** 水平翻转。 */
    fun flipHorizontal() {
        for (r in 0 until height) {
            val base = r * width
            var i = base
            var j = base + width - 1
            while (i < j) {
                val t = cells[i]; cells[i] = cells[j]; cells[j] = t
                i++; j--
            }
        }
    }

    /** 垂直翻转。 */
    fun flipVertical() {
        for (r in 0 until height / 2) {
            val a = r * width
            val b = (height - 1 - r) * width
            for (c in 0 until width) {
                val t = cells[a + c]; cells[a + c] = cells[b + c]; cells[b + c] = t
            }
        }
    }

    /** 顺时针旋转 90°，返回新网格（宽高互换）。 */
    fun rotatedClockwise(): BeadGrid {
        val out = BeadGrid(height, width)
        for (r in 0 until height) {
            for (c in 0 until width) {
                out.set(height - 1 - r, c, get(c, r))
            }
        }
        return out
    }

    /** 逆时针旋转 90°，返回新网格。 */
    fun rotatedCounterClockwise(): BeadGrid {
        val out = BeadGrid(height, width)
        for (r in 0 until height) {
            for (c in 0 until width) {
                out.set(r, width - 1 - c, get(c, r))
            }
        }
        return out
    }

    /**
     * 扩裁画布：返回新尺寸的网格，原内容按 [offsetX] / [offsetY] 放置。
     */
    fun resized(newWidth: Int, newHeight: Int, offsetX: Int, offsetY: Int): BeadGrid {
        val out = BeadGrid(newWidth, newHeight)
        for (r in 0 until height) {
            val nr = r + offsetY
            if (nr < 0 || nr >= newHeight) continue
            for (c in 0 until width) {
                val nc = c + offsetX
                if (nc < 0 || nc >= newWidth) continue
                out.set(nc, nr, get(c, r))
            }
        }
        return out
    }

    /** 导出为色号二维数组（"" 表示空）。 */
    fun toCodeGrid(palette: Palette): Array<Array<String>> {
        return Array(height) { r ->
            Array(width) { c ->
                val idx = get(c, r)
                if (idx == 0) "" else (palette.colorAt(idx)?.code ?: "")
            }
        }
    }

    companion object {
        const val MAX_SIZE = 1024

        /** 从色号二维数组建立网格。 */
        fun fromCodeGrid(grid: Array<Array<String>>, palette: Palette): BeadGrid {
            val h = grid.size.coerceIn(1, MAX_SIZE)
            val w = (grid.firstOrNull()?.size ?: 1).coerceIn(1, MAX_SIZE)
            val out = BeadGrid(w, h)
            for (r in 0 until minOf(h, grid.size)) {
                val row = grid[r]
                for (c in 0 until minOf(w, row.size)) {
                    val code = row[c]
                    if (code.isBlank()) continue
                    val idx = palette.indexOf(code)
                    if (idx > 0) out.set(c, r, idx)
                }
            }
            return out
        }
    }
}
