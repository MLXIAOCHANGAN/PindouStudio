package com.pindou.studio.ui.home

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.pindou.studio.PindouApp
import com.pindou.studio.core.BeadGrid
import com.pindou.studio.core.Palette
import com.pindou.studio.core.Project
import com.pindou.studio.core.ProjectSummary
import com.pindou.studio.data.PdFormat
import java.io.File

/** 首页状态。 */
class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val appRef = PindouApp.from(app)
    private val projectRepo = appRef.projectRepository
    private val paletteRepo = appRef.paletteRepository
    private val settings = appRef.settings

    var projects by mutableStateOf<List<ProjectSummary>>(emptyList())
        private set
    var palettes by mutableStateOf<List<Palette>>(emptyList())
        private set
    var message by mutableStateOf<String?>(null)

    init {
        refresh()
    }

    fun refresh() {
        projects = projectRepo.list()
        palettes = paletteRepo.all()
    }

    fun defaultPaletteId(): String =
        palettes.firstOrNull { it.id == settings.lastPaletteId }?.id
            ?: palettes.firstOrNull()?.id
            ?: ""

    /** 新建工程，返回工程 id。 */
    fun createProject(name: String, width: Int, height: Int, paletteId: String): String {
        val palette = paletteRepo.byId(paletteId) ?: paletteRepo.default()
        val id = projectRepo.newId()
        val now = System.currentTimeMillis()
        val project = Project(
            id = id,
            name = name.ifBlank { "未命名作品" },
            createdAt = now,
            updatedAt = now,
            paletteId = palette.id,
            paletteName = palette.name,
            grid = BeadGrid(width.coerceIn(1, BeadGrid.MAX_SIZE), height.coerceIn(1, BeadGrid.MAX_SIZE)),
        )
        projectRepo.save(project, palette)
        settings.lastPaletteId = palette.id
        refresh()
        return id
    }

    fun delete(id: String) {
        projectRepo.delete(id)
        refresh()
    }

    fun rename(id: String, newName: String) {
        projectRepo.rename(id, newName, paletteRepo)
        refresh()
    }

    fun duplicate(id: String) {
        val loaded = projectRepo.load(id, paletteRepo) ?: return
        val (project, palette) = loaded
        val copy = Project(
            id = projectRepo.newId(),
            name = project.name + " 副本",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            paletteId = palette.id,
            paletteName = palette.name,
            grid = project.grid.copy(),
            note = project.note,
        )
        projectRepo.save(copy, palette)
        refresh()
    }

    fun exportProject(id: String, target: File, asPd: Boolean): Boolean {
        val loaded = projectRepo.load(id, paletteRepo) ?: return false
        val (project, palette) = loaded
        return if (asPd) PdFormat.write(project, palette, target)
        else projectRepo.exportToFile(project, palette, target)
    }

    /**
     * 导入工程文件（.pd / .pdp / .json）。
     * @return 新工程 id，失败返回 null。
     */
    fun importProject(file: File): String? {
        val text = runCatching { file.readText() }.getOrNull()
        if (text == null) {
            message = "读取文件失败"
            return null
        }
        val normalized = text.trimStart()
        val looksLikePd = normalized.startsWith("{") &&
            normalized.contains("\"canvas\"") &&
            normalized.contains("\"grid\"")
        val name = file.name.lowercase()
        val fallback = paletteRepo.byId(settings.lastPaletteId) ?: paletteRepo.default()

        if (looksLikePd || name.endsWith(".pd")) {
            val result = PdFormat.read(file, projectRepo.newId(), fallback)
            if (result != null) {
                val (project, palette, hasPalette) = result
                projectRepo.save(project, palette)
                refresh()
                message = if (hasPalette) "已导入 PD 工程（含色卡）" else "已导入 PD 工程（使用当前色卡）"
                return project.id
            }
        }
        val imported = projectRepo.importFromFile(file, fallback)
        if (imported != null) {
            val (project, palette, _) = imported
            projectRepo.save(project, palette)
            refresh()
            return project.id
        }
        message = "无法识别的工程文件"
        return null
    }
}
