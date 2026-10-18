package com.pindou.studio.data

import com.pindou.studio.core.Palette
import com.pindou.studio.core.PaletteColor
import kotlinx.serialization.Serializable

/** 色卡里的一个颜色（序列化用）。 */
@Serializable
data class PaletteColorDto(
    val code: String,
    val name: String = "",
    val hex: String,
    val series: String = "?",
    val type: String = "normal",
    val hex2: String? = null,
) {
    fun toModel(): PaletteColor = PaletteColor(
        code = code,
        name = name.ifBlank { code },
        hex = hex,
        series = series.ifBlank { "?" },
        type = type.ifBlank { "normal" },
        hex2 = hex2,
    )

    companion object {
        fun fromModel(c: PaletteColor): PaletteColorDto = PaletteColorDto(
            code = c.code,
            name = c.name,
            hex = c.hex,
            series = c.series,
            type = c.type,
            hex2 = c.hex2,
        )
    }
}

/** 一套色卡（序列化用）。 */
@Serializable
data class PaletteDto(
    val id: String,
    val name: String,
    val builtin: Boolean = false,
    val colors: List<PaletteColorDto> = emptyList(),
) {
    fun toModel(): Palette = Palette(
        id = id,
        name = name,
        colors = colors.map { it.toModel() },
        builtin = builtin,
    )

    companion object {
        fun fromModel(p: Palette): PaletteDto = PaletteDto(
            id = p.id,
            name = p.name,
            builtin = p.builtin,
            colors = p.colors.map { PaletteColorDto.fromModel(it) },
        )
    }
}

/** 内置色卡资源文件。 */
@Serializable
data class PaletteAssetDto(
    val version: Int = 1,
    val palettes: List<PaletteDto> = emptyList(),
)

/** 工程文件（应用原生格式）。 */
@Serializable
data class ProjectDto(
    val version: Int = 1,
    val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val paletteId: String,
    val paletteName: String = "",
    val paletteColors: List<PaletteColorDto> = emptyList(),
    val width: Int,
    val height: Int,
    /** Base64(GZIP(每格 2 字节小端)) */
    val cells: String,
    val baseImagePath: String? = null,
    val note: String = "",
    val beadCount: Int = 0,
)

/** pindou-draw 的 .pd 工程文件。 */
@Serializable
data class PdProjectDto(
    val version: Int = 2,
    val createdAt: String? = null,
    val canvas: PdCanvasDto,
    val palette: PdPaletteDto? = null,
    val grid: List<List<String?>> = emptyList(),
)

@Serializable
data class PdCanvasDto(
    val width: Int,
    val height: Int,
    val pixelRatio: Int? = null,
    val cellSize: Int? = null,
)

@Serializable
data class PdPaletteDto(
    val id: String? = null,
    val label: String? = null,
    val entries: List<PdColorDto> = emptyList(),
)

@Serializable
data class PdColorDto(
    val code: String,
    val type: String? = null,
    val color: String? = null,
    val color1: String? = null,
    val color2: String? = null,
)
