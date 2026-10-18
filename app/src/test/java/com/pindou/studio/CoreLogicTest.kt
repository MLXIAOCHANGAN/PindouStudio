package com.pindou.studio

import com.pindou.studio.core.BeadGrid
import com.pindou.studio.core.ColorMath
import com.pindou.studio.core.GridHistory
import com.pindou.studio.core.Palette
import com.pindou.studio.core.PaletteColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreLogicTest {

    private fun testPalette(): Palette = Palette(
        id = "test",
        name = "测试色卡",
        colors = listOf(
            PaletteColor("A1", "A1", "#FF0000", "A", PaletteColor.TYPE_NORMAL, null),
            PaletteColor("A2", "A2", "#00FF00", "A", PaletteColor.TYPE_NORMAL, null),
            PaletteColor("B1", "B1", "#0000FF", "B", PaletteColor.TYPE_NORMAL, null),
            PaletteColor("L1", "L1", "#112233", "L", PaletteColor.TYPE_LIGHT, "#EEEEEE"),
        ),
    )

    @Test
    fun gridBasicSetGet() {
        val grid = BeadGrid(4, 3)
        assertEquals(0, grid.get(0, 0))
        grid.set(2, 1, 3)
        assertEquals(3, grid.get(2, 1))
        assertEquals(1, grid.paintedCount())
        // 越界安全
        grid.set(-1, 0, 2)
        grid.set(4, 0, 2)
        assertEquals(0, grid.get(-1, 0))
        assertEquals(1, grid.paintedCount())
    }

    @Test
    fun gridBoundsAndUsage() {
        val grid = BeadGrid(6, 6)
        grid.set(1, 1, 1)
        grid.set(3, 2, 1)
        grid.set(2, 4, 3)
        val b = grid.bounds()
        assertNotNull(b)
        assertEquals(1, b!![0])
        assertEquals(1, b[1])
        assertEquals(3, b[2])
        assertEquals(4, b[3])
        val usage = grid.colorUsage()
        assertEquals(2, usage.size)
        assertEquals(1, usage[0].first)
        assertEquals(2, usage[0].second)
    }

    @Test
    fun floodFillReplacesRegion() {
        val grid = BeadGrid(5, 5)
        grid.set(0, 0, 1)
        val changed = grid.floodFill(2, 2, 2)
        assertEquals(24, changed)
        // 已上色的 (0,0) 不属于目标区域，应保持不变
        assertEquals(1, grid.get(0, 0))
        assertEquals(2, grid.get(4, 4))
    }

    @Test
    fun floodFillStopsAtBoundary() {
        val grid = BeadGrid(3, 3)
        // 画一条竖线把画布分隔开
        for (r in 0 until 3) grid.set(1, r, 1)
        grid.floodFill(0, 0, 2)
        assertEquals(2, grid.get(0, 0))
        assertEquals(2, grid.get(0, 2))
        assertEquals(1, grid.get(1, 0))
        assertEquals(0, grid.get(2, 0))
    }

    @Test
    fun rotateAndFlip() {
        val grid = BeadGrid(3, 2)
        grid.set(0, 0, 1)
        grid.set(2, 1, 2)
        val cw = grid.rotatedClockwise()
        assertEquals(2, cw.width)
        assertEquals(3, cw.height)
        assertEquals(1, cw.get(1, 0))
        assertEquals(2, cw.get(0, 2))

        val flipped = grid.copy()
        flipped.flipHorizontal()
        assertEquals(1, flipped.get(2, 0))
        assertEquals(2, flipped.get(0, 1))
    }

    @Test
    fun resizeKeepsOffset() {
        val grid = BeadGrid(2, 2)
        grid.set(0, 0, 1)
        val bigger = grid.resized(4, 4, 1, 1)
        assertEquals(1, bigger.get(1, 1))
        val smaller = grid.resized(1, 1, 0, 0)
        assertEquals(1, smaller.get(0, 0))
    }

    @Test
    fun codeGridRoundTrip() {
        val palette = testPalette()
        val grid = BeadGrid(3, 3)
        grid.set(0, 0, 1)
        grid.set(2, 2, 4)
        val codes = grid.toCodeGrid(palette)
        assertEquals("A1", codes[0][0])
        assertEquals("", codes[1][1])
        assertEquals("L1", codes[2][2])
        val back = BeadGrid.fromCodeGrid(codes, palette)
        assertEquals(1, back.get(0, 0))
        assertEquals(4, back.get(2, 2))
    }

    @Test
    fun paletteLookups() {
        val palette = testPalette()
        assertEquals(4, palette.size)
        assertEquals(2, palette.indexOf("a2"))
        assertEquals(0, palette.indexOf("ZZ"))
        assertEquals("#00FF00", palette.colorAt(2)?.hex)
        assertNull(palette.colorAt(9))
        assertEquals(3, palette.seriesGroups().size)
        assertTrue(palette.colorAt(4)!!.isDualState)
    }

    @Test
    fun historyPatchUndoRedo() {
        val grid = BeadGrid(4, 4)
        val history = GridHistory()
        val recorder = GridHistory.StrokeRecorder(grid)
        recorder.record(0, 0, 1)
        recorder.record(1, 0, 2)
        val patch = recorder.build()
        assertNotNull(patch)
        history.push(patch!!)
        assertEquals(1, grid.get(0, 0))

        val afterUndo = history.undo(grid)
        assertNotNull(afterUndo)
        assertEquals(0, afterUndo!!.get(0, 0))
        assertEquals(0, afterUndo.get(1, 0))

        val afterRedo = history.redo(grid)
        assertNotNull(afterRedo)
        assertEquals(1, afterRedo!!.get(0, 0))
        assertEquals(2, afterRedo.get(1, 0))
    }

    @Test
    fun historySnapshotRestoresSize() {
        val grid = BeadGrid(2, 2)
        grid.set(0, 0, 1)
        val history = GridHistory()
        val before = grid.cells.copyOf()
        val resized = grid.resized(3, 3, 0, 0)
        history.push(GridHistory.Snapshot(2, 2, before))

        val undone = history.undo(resized)
        assertNotNull(undone)
        assertEquals(2, undone!!.width)
        assertEquals(1, undone.get(0, 0))

        val redone = history.redo(undone)
        assertNotNull(redone)
        assertEquals(3, redone!!.width)
    }

    @Test
    fun colorMathHexParsing() {
        assertEquals("#FF0000", ColorMath.toHex(ColorMath.parseHex("#FF0000")))
        assertEquals("#0A0B0C", ColorMath.toHex(ColorMath.parseHex("rgb(10, 11, 12)")))
        assertEquals("#FFFFFF", ColorMath.toHex(ColorMath.parseHex("#fff")))
    }

    @Test
    fun oklabNearestIsSane() {
        val red = ColorMath.rgbToOklab(255, 0, 0)
        val nearRed = ColorMath.rgbToOklab(240, 10, 10)
        val blue = ColorMath.rgbToOklab(0, 0, 255)
        assertTrue(ColorMath.oklabDistance(red, nearRed) < ColorMath.oklabDistance(red, blue))
    }

    @Test
    fun colorAdjustKeepsRange() {
        val adjusted = ColorMath.adjust(200, 100, 50, 60f, 40f, 30f)
        assertTrue(ColorMath.red(adjusted) in 0..255)
        assertTrue(ColorMath.green(adjusted) in 0..255)
        assertTrue(ColorMath.blue(adjusted) in 0..255)
    }

    @Test
    fun svgExportContainsShapes() {
        val palette = testPalette()
        val grid = BeadGrid(4, 2)
        // 一行连续两格同色，应合并成一个 rect
        grid.set(0, 0, 1)
        grid.set(1, 0, 1)
        grid.set(3, 1, 2)
        val svg = com.pindou.studio.core.SvgExporter.render(
            grid, palette,
            com.pindou.studio.core.SvgOptions(cellPx = 10, showCodes = false, showGrid = false),
        )
        assertTrue(svg.startsWith("<?xml"))
        assertTrue(svg.contains("<svg"))
        assertTrue(svg.trimEnd().endsWith("</svg>"))
        assertTrue(svg.contains("fill=\"#FF0000\""))
        assertTrue(svg.contains("fill=\"#00FF00\""))
        // 合并后同色两格 -> 一个 20 宽的矩形
        assertTrue(svg.contains("width=\"20\""))
    }

    @Test
    fun svgEscapesTitle() {
        val palette = testPalette()
        val grid = BeadGrid(2, 2)
        grid.set(0, 0, 1)
        val svg = com.pindou.studio.core.SvgExporter.render(
            grid, palette,
            com.pindou.studio.core.SvgOptions(cellPx = 10, title = "A & B <x>"),
        )
        assertTrue(svg.contains("A &amp; B &lt;x&gt;"))
    }

    @Test
    fun usageCsvListsCounts() {
        val palette = testPalette()
        val grid = BeadGrid(3, 3)
        grid.set(0, 0, 1)
        grid.set(1, 0, 1)
        grid.set(2, 2, 3)
        val csv = com.pindou.studio.core.SvgExporter.usageCsv(grid, palette)
        assertTrue(csv.startsWith("色号,数量"))
        assertTrue(csv.contains("A1,2"))
        assertTrue(csv.contains("B1,1"))
        assertTrue(csv.contains("合计,3"))
    }

    @Test
    fun pdRoundTripKeepsGridAndPalette() {
        val palette = testPalette()
        val grid = BeadGrid(5, 4)
        grid.set(0, 0, 1)
        grid.set(4, 3, 3)
        grid.set(2, 1, 4)
        val project = com.pindou.studio.core.Project(
            id = "t1",
            name = "PD 测试",
            createdAt = 1_700_000_000_000L,
            updatedAt = 1_700_000_000_000L,
            paletteId = palette.id,
            paletteName = palette.name,
            grid = grid,
        )
        val dir = java.io.File(System.getProperty("java.io.tmpdir"), "pindou-test")
        dir.mkdirs()
        val file = java.io.File(dir, "roundtrip.pd")
        assertTrue(com.pindou.studio.data.PdFormat.write(project, palette, file))
        assertTrue(file.length() > 0)

        val read = com.pindou.studio.data.PdFormat.read(file, "t2")
        assertNotNull(read)
        val (restored, restoredPalette, hasPalette) = read!!
        assertTrue(hasPalette)
        assertEquals(5, restored.width)
        assertEquals(4, restored.height)
        assertEquals(4, restoredPalette.size)
        assertEquals(1, restored.grid.get(0, 0))
        assertEquals(3, restored.grid.get(4, 3))
        assertEquals(4, restored.grid.get(2, 1))
        assertEquals(0, restored.grid.get(1, 1))
    }

    @Test
    fun pdWithoutPaletteUsesFallback() {
        val dir = java.io.File(System.getProperty("java.io.tmpdir"), "pindou-test")
        dir.mkdirs()
        val file = java.io.File(dir, "no-palette.pd")
        file.writeText(
            """
            {"version":2,"canvas":{"width":3,"height":2},
             "grid":[["A1",null,"B1"],[null,"A2",null]]}
            """.trimIndent()
        )
        val palette = testPalette()
        val read = com.pindou.studio.data.PdFormat.read(file, "t3", palette)
        assertNotNull(read)
        val (restored, restoredPalette, hasPalette) = read!!
        assertTrue(!hasPalette)
        assertEquals(3, restored.width)
        assertEquals(2, restored.height)
        // 回退到传入的色卡（4 色）
        assertEquals(4, restoredPalette.size)
        assertEquals(1, restored.grid.get(0, 0))
        assertEquals(3, restored.grid.get(2, 0))
        assertEquals(2, restored.grid.get(1, 1))
    }
}
