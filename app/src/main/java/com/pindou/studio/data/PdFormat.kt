package com.pindou.studio.data

import com.pindou.studio.core.BeadGrid
import com.pindou.studio.core.ColorMath
import com.pindou.studio.core.Palette
import com.pindou.studio.core.PaletteColor
import com.pindou.studio.core.Project
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 与「拼豆草图制作网页」(pindou-draw) 的 .pd 工程文件互转。
 *
 * .pd 结构：
 * ```
 * { version, createdAt, canvas:{width,height,pixelRatio,cellSize},
 *   palette:{ id, label, entries:[{code,type,color,color1,color2}] },
 *   grid: [ [ "A1", null, ... ] ] }
 * ```
 */
object PdFormat {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    /**
     * 读取 .pd 文件。
     * @param fallbackPalette 当 .pd 没有携带色卡时用来解析色号的色卡。
     */
    fun read(file: File, newId: String, fallbackPalette: Palette? = null): Triple<Project, Palette, Boolean>? {
        return try {
            val dto = json.decodeFromString(PdProjectDto.serializer(), file.readText())
            val w = dto.canvas.width.coerceIn(1, BeadGrid.MAX_SIZE)
            val h = dto.canvas.height.coerceIn(1, BeadGrid.MAX_SIZE)
            val entries = dto.palette?.entries.orEmpty()
            val colors = ArrayList<PaletteColor>()
            for (e in entries) {
                if (e.code.isBlank()) continue
                val raw = e.color1?.takeIf { it.isNotBlank() }
                    ?: e.color?.takeIf { it.isNotBlank() }
                    ?: continue
                val hex = ColorMath.toHex(ColorMath.parseHex(raw))
                val hex2 = e.color2?.takeIf { it.isNotBlank() }?.let { ColorMath.toHex(ColorMath.parseHex(it)) }
                colors.add(
                    PaletteColor(
                        code = e.code.trim(),
                        name = e.code.trim(),
                        hex = hex,
                        series = e.code.trim().takeWhile { it.isLetter() }.uppercase().ifBlank { "?" },
                        type = e.type ?: PaletteColor.TYPE_NORMAL,
                        hex2 = hex2,
                    )
                )
            }
            val label = dto.palette?.label?.takeIf { it.isNotBlank() } ?: "PD 色卡"
            val palette = if (colors.isEmpty() && fallbackPalette != null) {
                fallbackPalette
            } else {
                Palette(
                    id = dto.palette?.id?.takeIf { it.isNotBlank() } ?: ("pd-" + System.currentTimeMillis()),
                    name = label,
                    colors = colors,
                    builtin = false,
                )
            }
            val grid = BeadGrid(w, h)
            for (r in 0 until minOf(h, dto.grid.size)) {
                val row = dto.grid[r]
                for (c in 0 until minOf(w, row.size)) {
                    val code = row[c]?.takeIf { it.isNotBlank() } ?: continue
                    val idx = palette.indexOf(code)
                    if (idx > 0) grid.set(c, r, idx)
                }
            }
            val created = parseIso(dto.createdAt) ?: System.currentTimeMillis()
            val project = Project(
                id = newId,
                name = file.nameWithoutExtension,
                createdAt = created,
                updatedAt = System.currentTimeMillis(),
                paletteId = palette.id,
                paletteName = palette.name,
                grid = grid,
            )
            Triple(project, palette, colors.isNotEmpty())
        } catch (e: Exception) {
            null
        }
    }

    /** 写出 .pd 文件。 */
    fun write(project: Project, palette: Palette, target: File): Boolean = try {
        val dto = PdProjectDto(
            version = 2,
            createdAt = iso.format(Date(project.createdAt)),
            canvas = PdCanvasDto(project.width, project.height, 10, 10),
            palette = PdPaletteDto(
                id = palette.id,
                label = palette.name,
                entries = palette.colors.map { c ->
                    PdColorDto(
                        code = c.code,
                        type = c.type,
                        color = rgbString(c.argb),
                        color1 = rgbString(c.argb),
                        color2 = c.hex2?.let { rgbString(ColorMath.parseHex(it)) },
                    )
                },
            ),
            grid = project.grid.toCodeGrid(palette).map { row ->
                row.map { code -> code.ifBlank { null } }
            },
        )
        target.writeText(json.encodeToString(PdProjectDto.serializer(), dto))
        true
    } catch (e: Exception) {
        false
    }

    private fun rgbString(argb: Int): String =
        "rgb(${ColorMath.red(argb)}, ${ColorMath.green(argb)}, ${ColorMath.blue(argb)})"

    private fun parseIso(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        return try {
            iso.parse(value)?.time
        } catch (e: Exception) {
            null
        }
    }
}
