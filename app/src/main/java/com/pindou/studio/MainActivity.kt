package com.pindou.studio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import com.pindou.studio.ui.editor.EditorScreen
import com.pindou.studio.ui.home.HomeScreen
import com.pindou.studio.ui.palette.PaletteManagerScreen
import com.pindou.studio.ui.theme.PindouTheme

/** 页面。 */
sealed interface Screen {
    data object Home : Screen
    data class Editor(val projectId: String, val autoOpenImport: Boolean = false) : Screen
    data object Palettes : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        setContent {
            PindouTheme {
                var screen by remember { mutableStateOf<Screen>(Screen.Home) }
                BackHandler(enabled = screen !is Screen.Home) {
                    screen = Screen.Home
                }
                when (val current = screen) {
                    is Screen.Home -> HomeScreen(
                        onOpenProject = { id, openImport -> screen = Screen.Editor(id, openImport) },
                        onOpenPalettes = { screen = Screen.Palettes },
                    )
                    is Screen.Editor -> EditorScreen(
                        projectId = current.projectId,
                        autoOpenImport = current.autoOpenImport,
                        onBack = {
                            screen = Screen.Home
                        },
                    )
                    is Screen.Palettes -> PaletteManagerScreen(
                        onBack = { screen = Screen.Home },
                    )
                }
            }
        }
    }
}
