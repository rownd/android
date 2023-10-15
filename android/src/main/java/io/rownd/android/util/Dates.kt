package io.rownd.android.util

internal fun stringToSeconds(str: String?): Int? {
    if (str == null || str.isEmpty()) {
        return null;
    }

    val numberString = str.substring(0, str.length-1)
    val timeUnit = str.last()

    val number = numberString.toIntOrNull() ?: return null

    return when (timeUnit) {
        "s".first() -> { // seconds
            number
        }
        "m".first() -> { // minutes
            number * 60
        }
        "h".first() -> { // hours
            number * 3600
        }
        "d".first() -> { // days
            number * 86400
        }
        "w".first() -> { // weeks
            number * 604800
        }
        "y".first() -> { // years
            number * 31536000
        }
        else -> {
            null
        }
    }
}