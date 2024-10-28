package io.rownd.android.util

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import io.opentelemetry.api.trace.Tracer
import io.rownd.telemetry.Telemetry
import javax.inject.Inject

class Telemetry @Inject constructor(internal val rowndContext: RowndContext) {
    fun init() {
        try {
            var appIdentifier = "Unknown"

            rowndContext.client?.appHandleWrapper?.app?.get()?.let {
                appIdentifier = getAppIdentifierAndVersion(it)
            }

            Telemetry.init(appIdentifier)
        } catch (e: Exception) {
            Log.w("Telemetry", "Failed to initialize telemetry")
        }
    }

    internal fun getTracer(): Tracer? {
        return Telemetry.getTracer()
    }

    private fun getAppIdentifierAndVersion(context: Context): String {
        val packageName = context.packageName
        val versionName: String
        val versionCode: Long

        try {
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            versionName = packageInfo.versionName ?: "N/A"
            versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
        } catch (e: PackageManager.NameNotFoundException) {
            return "Unknown"
        }

        return "$packageName $versionName ($versionCode)"
    }
}