package com.pindou.studio

import android.app.Application
import android.content.Context
import com.pindou.studio.data.AppSettings
import com.pindou.studio.data.PaletteRepository
import com.pindou.studio.data.ProjectRepository

/** 全局应用对象：持有各仓库单例。 */
class PindouApp : Application() {

    val paletteRepository: PaletteRepository by lazy { PaletteRepository(this) }
    val projectRepository: ProjectRepository by lazy { ProjectRepository(this) }
    val settings: AppSettings by lazy { AppSettings(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: PindouApp
            private set

        fun from(context: Context): PindouApp =
            context.applicationContext as PindouApp
    }
}
