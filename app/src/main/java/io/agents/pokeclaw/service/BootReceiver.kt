// Copyright 2026 PokeClaw (agents.io). All rights reserved.
// Licensed under the Apache License, Version 2.0.

package io.agents.pokeclaw.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.agents.pokeclaw.utils.XLog

/**
 * Boot broadcast receiver for auto-start on device boot
 * Starts the foreground service upon receiving BOOT_COMPLETED to keep the app running in the background
 * Does not launch an Activity, to avoid conflicting with the user manually opening the app
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            XLog.i(TAG, "Boot broadcast received, starting foreground service")
            ForegroundService.start(context)
        }
    }
}
