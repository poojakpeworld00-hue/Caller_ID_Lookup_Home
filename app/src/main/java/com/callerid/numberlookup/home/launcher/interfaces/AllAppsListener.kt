package com.callerid.numberlookup.home.launcher.interfaces

import com.callerid.numberlookup.home.launcher.models.AppLauncher

interface AllAppsListener {
    fun onAppLauncherLongPressed(x: Float, y: Float, appLauncher: AppLauncher)
}
