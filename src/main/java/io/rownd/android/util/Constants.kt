package io.rownd.android.util

import android.os.Build
import io.rownd.android.BuildConfig

object Constants {
    val DEFAULT_API_USER_AGENT = "Rownd SDK for Android/${BuildConfig.VERSION_CODE} (Language: Kotlin; Platform=Android ${Build.VERSION.BASE_OS} ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT});)"

    val DEFAULT_WEB_USER_AGENT = "Rownd SDK for Android/${BuildConfig.VERSION_CODE} (Language: Kotlin; Platform=Android ${Build.VERSION.BASE_OS} ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT});)"

}