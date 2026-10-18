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
import com.pindou.studio.core.DisplayMode
import com.pindou.studio.ui.components.ChipButton
import com.pindou.studio.ui.components.LabeledSlider
import com.pindou.studio.ui.components.SectionTitle
import com.pindou.studio.ui.components.SwitchRow

/** 显示设置面板。 */
@Composable
fun DisplaySheetContent(vm: EditorViewModel) {
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
    ) {
        SectionTitle("画布光效")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DisplayMode.entries.forEach { mode ->
                ChipButton(mode.label, vm.displayMode == mode) { vm.changeDisplayMode(mode) }
            }
        }
        Text(
            "光变 / 温变 / 夜光 / 珠光 色号会按模式显示对应的效果",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        SectionTitle("显示选项")
        SwitchRow("显示网格", checked = vm.showGrid) { vm.toggleShowGrid() }
        SwitchRow("加粗主网格（每 8 格）", checked = vm.showMajorGrid) { vm.toggleMajorGrid() }
        SwitchRow("显示色号", "放大到一定程度才显示", checked = vm.showCodes) { vm.toggleShowCodes() }
        SwitchRow("显示坐标尺", checked = vm.showRuler) { vm.toggleRuler() }
        SwitchRow("豆子显示为圆形", checked = vm.roundBeads) { vm.toggleRoundBeads() }

        SectionTitle("底图")
        SwitchRow(
            "显示底图",
            if (vm.baseImage == null) "还没有导入底图" else null,
            checked = vm.baseVisible,
        ) { vm.baseVisible = it }
        LabeledSlider(
            title = "底图不透明度",
            value = vm.baseOpacity,
            valueRange = 0f..1f,
            valueText = "${(vm.baseOpacity * 100).toInt()}%",
        ) { vm.baseOpacity = it }
        LabeledSlider(
            title = "底图缩放",
            value = vm.baseScale,
            valueRange = 0.2f..3f,
            valueText = "${(vm.baseScale * 100).toInt()}%",
        ) { vm.baseScale = it }
        Spacer(Modifier.height(24.dp))
    }
}
