package com.pindou.studio.core

/**
 * 一个拼豆工程。
 */
class Project(
    var id: String,
    var name: String,
    var createdAt: Long,
    var updatedAt: Long,
    var paletteId: String,
    var paletteName: String,
    var grid: BeadGrid,
    var baseImagePath: String? = null,
    var note: String = "",
) {
    val width: Int get() = grid.width
    val height: Int get() = grid.height

    fun touch() {
        updatedAt = System.currentTimeMillis()
    }
}

/** 工程列表项。 */
data class ProjectSummary(
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val width: Int,
    val height: Int,
    val paletteName: String,
    val beadCount: Int,
    val thumbnailPath: String?,
)
