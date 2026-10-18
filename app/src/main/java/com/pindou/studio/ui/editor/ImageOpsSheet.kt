package com.pindou.studio.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pindou.studio.core.SymmetryMode
import com.pindou.studio.ui.components.ChipButton
import com.pindou.studio.ui.components.IconSquare
import com.pindou.studio.ui.components.PIcon
import com.pindou.studio.ui.components.SectionTitle

/** 图像操作面板：翻转 / 旋转 / 对称。 */
@Composable
fun ImageOpsSheetContent(vm: EditorViewModel, onDone: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
    ) {
        SectionTitle("变换")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconSquare(PIcon.FlipH, "水平翻转", size = 56) { vm.flipHorizontal() }
            IconSquare(PIcon.FlipV, "垂直翻转", size = 56) { vm.flipVertical() }
            IconSquare(PIcon.RotateCW, "顺时针", size = 56) { vm.rotateClockwise() }
            IconSquare(PIcon.RotateCCW, "逆时针", size = 56) { vm.rotateCounterClockwise() }
        }
        SectionTitle("对称绘制")
        Text(
            "开启后画笔会同时绘制镜像位置的豆子",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SymmetryMode.entries.forEach { mode ->
                ChipButton(mode.label, vm.symmetry == mode) { vm.symmetry = mode }
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChipButton("全选", false) { vm.selectAll() }
            ChipButton("清空画布", false) { vm.clearCanvas() }
            ChipButton("完成", false) { onDone() }
        }
        Spacer(Modifier.height(24.dp))
    }
}
