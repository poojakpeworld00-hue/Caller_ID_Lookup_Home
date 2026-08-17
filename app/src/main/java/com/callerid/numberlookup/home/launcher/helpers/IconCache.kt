package com.callerid.numberlookup.home.launcher.helpers

import com.callerid.numberlookup.home.launcher.models.AppLauncher

object IconCache {
    @Volatile
    private var cachedLaunchers = emptyList<AppLauncher>()

    var launchers: List<AppLauncher>
        get() = cachedLaunchers
        set(value) {
            synchronized(this) {
                cachedLaunchers = value
            }
        }

    fun clear() {
        launchers = emptyList()
    }
}