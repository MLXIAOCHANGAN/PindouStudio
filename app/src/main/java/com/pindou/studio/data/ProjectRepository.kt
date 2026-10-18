package com.pindou.studio.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import com.pindou.studio.core.BeadGrid
import com.pindou.studio.core.Palette
import com.pindou.studio.core.Project
import com.pindou.studio.core.ProjectSummary
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/**
 * 工程仓库：工程以 JSON + 缩略图 PNG 保存在应用私有目录。
 */
class ProjectRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val projectDir: File by lazy { File(context.filesDir, "projects").apply { mkdirs() } }
    private val thumbDir: File by lazy { File(context.filesDir, "thumbs").apply { mkdirs() } }
    private val baseDir: File by lazy { File(context.filesDir, "base").apply { mkdirs() } }

    fun baseDir(): File = baseDir

    fun newId(): String = "p" + System.currentTimeMillis() + "-" + (1000..9999).random()

    fun thumbnailFile(id: String): File = File(thumbDir, "$id.png")

    private fun projectFile(id: String): File = File(projectDir, "$id.json")

    /** 保存工程（含色卡快照 + 缩略图）。 */
    fun save(project: Project, palette: Palette, writeThumbnail: Boolean = true): Boolean {
        return try {
            project.updatedAt = System.currentTimeMillis()
            val dto = ProjectDto(
                id = project.id,
                name = project.name,
                createdAt = project.createdAt,
                updatedAt = project.updatedAt,
                paletteId = project.paletteId,
                paletteName = project.paletteName,
                paletteColors = palette.colors.map { PaletteColorDto.fromModel(it) },
                width = project.width,
                height = project.height,
                cells = Codec.encodeCells(project.grid),
                baseImagePath = project.baseImagePath,
                note = project.note,
                beadCount = project.grid.paintedCount(),
            )
            projectFile(project.id).writeText(json.encodeToString(ProjectDto.serializer(), dto))
            if (writeThumbnail) writeThumbnail(project.id, project.grid, palette)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** 把工程导出为可移植文件（.pdp）。 */
    fun exportToFile(project: Project, palette: Palette, target: File): Boolean = try {
        val dto = ProjectDto(
            id = project.id,
            name = project.name,
            createdAt = project.createdAt,
            updatedAt = project.updatedAt,
            paletteId = project.paletteId,
            paletteName = project.paletteName,
            paletteColors = palette.colors.map { PaletteColorDto.fromModel(it) },
            width = project.width,
            height = project.height,
            cells = Codec.encodeCells(project.grid),
            baseImagePath = null,
            note = project.note,
        )
        target.writeText(json.encodeToString(ProjectDto.serializer(), dto))
        true
    } catch (e: Exception) {
        false
    }

    /** 从 .pdp 文件读取工程。 */
    fun importFromFile(
        file: File,
        fallbackPalette: Palette? = null,
    ): Triple<Project, Palette, Boolean>? = try {
        val dto = json.decodeFromString(ProjectDto.serializer(), file.readText())
        val palette = if (dto.paletteColors.isNotEmpty()) {
            Palette(dto.paletteId, dto.paletteName.ifBlank { "导入色卡" }, dto.paletteColors.map { it.toModel() }, builtin = false)
        } else {
            fallbackPalette
                ?: Palette(dto.paletteId, dto.paletteName.ifBlank { "导入色卡" }, emptyList(), builtin = false)
        }
        val grid = Codec.decodeCells(dto.cells, dto.width, dto.height)
        val now = System.currentTimeMillis()
        val project = Project(
            id = newId(),
            name = dto.name.ifBlank { file.nameWithoutExtension },
            createdAt = now,
            updatedAt = now,
            paletteId = dto.paletteId,
            paletteName = dto.paletteName,
            grid = grid,
            note = dto.note,
        )
        Triple(project, palette, dto.paletteColors.isNotEmpty())
    } catch (e: Exception) {
        null
    }

    fun list(): List<ProjectSummary> {
        val files = projectDir.listFiles { f -> f.extension == "json" } ?: return emptyList()
        val out = ArrayList<ProjectSummary>(files.size)
        for (file in files) {
            try {
                val dto = json.decodeFromString(ProjectDto.serializer(), file.readText())
                val thumb = thumbnailFile(dto.id)
                out.add(
                    ProjectSummary(
                        id = dto.id,
                        name = dto.name,
                        createdAt = dto.createdAt,
                        updatedAt = dto.updatedAt,
                        width = dto.width,
                        height = dto.height,
                        paletteName = dto.paletteName,
                        beadCount = dto.beadCount,
                        thumbnailPath = if (thumb.exists()) thumb.absolutePath else null,
                    )
                )
            } catch (_: Exception) {
                // 忽略损坏文件
            }
        }
        return out.sortedByDescending { it.updatedAt }
    }

    /**
     * 读取工程。
     * @return 工程与其色卡；色卡优先使用工程内快照。
     */
    fun load(id: String, paletteRepository: PaletteRepository): Pair<Project, Palette>? {
        val file = projectFile(id)
        if (!file.exists()) return null
        return try {
            val dto = json.decodeFromString(ProjectDto.serializer(), file.readText())
            val palette = when {
                dto.paletteColors.isNotEmpty() -> Palette(
                    dto.paletteId,
                    dto.paletteName.ifBlank { "工程色卡" },
                    dto.paletteColors.map { it.toModel() },
                    builtin = false,
                )
                else -> paletteRepository.byId(dto.paletteId) ?: paletteRepository.default()
            }
            val grid = Codec.decodeCells(dto.cells, dto.width, dto.height)
            val project = Project(
                id = dto.id,
                name = dto.name,
                createdAt = dto.createdAt,
                updatedAt = dto.updatedAt,
                paletteId = palette.id,
                paletteName = palette.name,
                grid = grid,
                baseImagePath = dto.baseImagePath,
                note = dto.note,
            )
            project to palette
        } catch (e: Exception) {
            null
        }
    }

    fun delete(id: String): Boolean {
        thumbnailFile(id).delete()
        return projectFile(id).delete()
    }

    fun rename(id: String, newName: String, paletteRepository: PaletteRepository): Boolean {
        val loaded = load(id, paletteRepository) ?: return false
        loaded.first.name = newName
        return save(loaded.first, loaded.second)
    }

    /** 写入缩略图。 */
    fun writeThumbnail(id: String, grid: BeadGrid, palette: Palette, maxSide: Int = 420) {
        try {
            val bounds = grid.bounds()
            val bmp = renderThumbnail(grid, palette, maxSide, bounds)
            FileOutputStream(thumbnailFile(id)).use { bmp.compress(Bitmap.CompressFormat.PNG, 90, it) }
            bmp.recycle()
        } catch (_: Exception) {
        }
    }

    companion object {
        /** 渲染缩略图（保持比例，透明处为白）。 */
        fun renderThumbnail(grid: BeadGrid, palette: Palette, maxSide: Int = 420, bounds: IntArray? = null): Bitmap {
            val b = bounds ?: grid.bounds()
            val minC: Int
            val minR: Int
            val cols: Int
            val rows: Int
            if (b == null) {
                minC = 0; minR = 0; cols = grid.width; rows = grid.height
            } else {
                minC = b[0]; minR = b[1]; cols = b[2] - b[0] + 1; rows = b[3] - b[1] + 1
            }
            val scale = maxSide.toFloat() / max(cols, rows)
            val cw = max(1, (cols * scale).toInt())
            val ch = max(1, (rows * scale).toInt())
            val bmp = Bitmap.createBitmap(cw, ch, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.drawColor(0xFFFFFFFF.toInt())
            val paint = Paint().apply { isAntiAlias = false }
            val cellW = cw.toFloat() / cols
            val cellH = ch.toFloat() / rows
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val idx = grid.get(minC + c, minR + r)
                    if (idx == 0) continue
                    val color = palette.colorAt(idx) ?: continue
                    paint.color = color.argb
                    canvas.drawRect(
                        c * cellW, r * cellH, (c + 1) * cellW + 0.5f, (r + 1) * cellH + 0.5f, paint,
                    )
                }
            }
            return bmp
        }
    }
}
