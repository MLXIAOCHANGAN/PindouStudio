package com.pindou.studio.core

/**
 * 撤销 / 重做历史。
 *
 * - 笔画（拖动绘制）记录成补丁（只存被改动的格子），非常省内存；
 * - 一次性操作（填充 / 清空 / 图片导入 / 旋转 / 扩裁）记录整张快照。
 */
class GridHistory(private val limit: Int = 80) {

    /** 一条历史记录。 */
    sealed class Entry {
        abstract val width: Int
        abstract val height: Int
    }

    /** 补丁型记录：只包含被修改的格子。 */
    class Patch(
        override val width: Int,
        override val height: Int,
        val index: IntArray,
        val before: ShortArray,
        val after: ShortArray,
    ) : Entry()

    /** 快照型记录：把整张网格还原到 [state]。 */
    class Snapshot(
        override val width: Int,
        override val height: Int,
        val state: ShortArray,
    ) : Entry()

    private val undoStack = ArrayList<Entry>()
    private val redoStack = ArrayList<Entry>()

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()
    val undoCount: Int get() = undoStack.size
    val redoCount: Int get() = redoStack.size

    fun clear() {
        undoStack.clear()
        redoStack.clear()
    }

    fun push(entry: Entry) {
        undoStack.add(entry)
        redoStack.clear()
        while (undoStack.size > limit) undoStack.removeAt(0)
    }

    /**
     * 撤销一步。
     * @return 需要恢复到的网格，null 表示没有可撤销的内容。
     */
    fun undo(current: BeadGrid): BeadGrid? {
        if (undoStack.isEmpty()) return null
        val entry = undoStack.removeAt(undoStack.size - 1)
        return when (entry) {
            is Patch -> {
                redoStack.add(entry)
                current.applyPatch(entry.index, entry.before)
                current
            }
            is Snapshot -> {
                redoStack.add(Snapshot(current.width, current.height, current.cells.copyOf()))
                BeadGrid(entry.width, entry.height, entry.state.copyOf())
            }
        }
    }

    /**
     * 重做一步。
     * @return 需要恢复到的网格，null 表示没有可重做的内容。
     */
    fun redo(current: BeadGrid): BeadGrid? {
        if (redoStack.isEmpty()) return null
        val entry = redoStack.removeAt(redoStack.size - 1)
        return when (entry) {
            is Patch -> {
                undoStack.add(entry)
                current.applyPatch(entry.index, entry.after)
                current
            }
            is Snapshot -> {
                undoStack.add(Snapshot(current.width, current.height, current.cells.copyOf()))
                BeadGrid(entry.width, entry.height, entry.state.copyOf())
            }
        }
    }

    /** 累加一次笔画产生的改动。 */
    class StrokeRecorder(private val grid: BeadGrid) {
        private val indexBuf = ArrayList<Int>()
        private val beforeBuf = ArrayList<Short>()
        private val afterBuf = ArrayList<Short>()
        private val touched = HashMap<Int, Int>() // cellIndex -> position in buffers

        fun record(c: Int, r: Int, newValue: Int) {
            if (!grid.inBounds(c, r)) return
            val idx = r * grid.width + c
            val old = grid.cells[idx]
            if (old.toInt() == newValue) return
            val existing = touched[idx]
            if (existing != null) {
                afterBuf[existing] = newValue.toShort()
                grid.cells[idx] = newValue.toShort()
                if (beforeBuf[existing] == afterBuf[existing]) {
                    // 恢复原状，从补丁里剔除
                    touched.remove(idx)
                }
                return
            }
            touched[idx] = indexBuf.size
            indexBuf.add(idx)
            beforeBuf.add(old)
            afterBuf.add(newValue.toShort())
            grid.cells[idx] = newValue.toShort()
        }

        fun hasChanges(): Boolean = touched.isNotEmpty()

        fun build(): Patch? {
            if (touched.isEmpty()) return null
            val keep = indexBuf.indices.filter { i ->
                beforeBuf[i] != afterBuf[i]
            }
            if (keep.isEmpty()) return null
            return Patch(
                grid.width,
                grid.height,
                IntArray(keep.size) { indexBuf[keep[it]] },
                ShortArray(keep.size) { beforeBuf[keep[it]] },
                ShortArray(keep.size) { afterBuf[keep[it]] },
            )
        }
    }
}

/** 把补丁应用到网格。 */
fun BeadGrid.applyPatch(index: IntArray, values: ShortArray) {
    for (i in index.indices) {
        val idx = index[i]
        if (idx in cells.indices) cells[idx] = values[i]
    }
}
