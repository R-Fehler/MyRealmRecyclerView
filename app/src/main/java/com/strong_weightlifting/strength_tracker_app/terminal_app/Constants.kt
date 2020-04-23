package com.strong_weightlifting.strength_tracker_app.terminal_app

import com.strong_weightlifting.strength_tracker_app.BuildConfig

internal object Constants {
    // values have to be globally unique
    const val INTENT_ACTION_DISCONNECT =
        BuildConfig.APPLICATION_ID + ".Disconnect"
    const val NOTIFICATION_CHANNEL =
        BuildConfig.APPLICATION_ID + ".Channel"
    const val INTENT_CLASS_MAIN_ACTIVITY =
        BuildConfig.APPLICATION_ID + ".MainActivity"

    // values have to be unique within each app
    const val NOTIFY_MANAGER_START_FOREGROUND_SERVICE = 1001
}