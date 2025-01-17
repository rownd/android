package io.rownd.android.util
import java.util.regex.Pattern

fun redactSensitiveKeys(jsonString: String?): String {
    // Return an empty string if the input is null
    if (jsonString == null) {
        return ""
    }

    // Compile the combined pattern to match both JSON-like and non-JSON structures
    val pattern = Pattern.compile(
        // Matches JSON-like tokens
        "\\\"(accessToken|refreshToken|refresh_token|access_token|id_token)\\\"\\s*:\\s*\\\"[^\"\\\\]*\\\"" +
                // Matches key-value token structures
                "|(accessToken=|refreshToken=)[^,\\)]+" +
                // Matches Authorization headers with JWT
                "|(Authorization: Bearer [A-Za-z0-9-._~+/]+=*)"
    )

    // Create a matcher for the combined pattern
    val matcher = pattern.matcher(jsonString)

    // Use StringBuffer to build the redacted string
    val stringBuffer = StringBuffer()

    // Iterate through all matches and replace them with the redacted pattern
    while (matcher.find()) {
        // Determine if the group index (1 or 2) is non-null and replace accordingly
        if (matcher.group(1) != null) {
            matcher.appendReplacement(stringBuffer, "\"${matcher.group(1)}\": \"[REDACTED]\"")
        } else if (matcher.group(2) != null) {
            matcher.appendReplacement(stringBuffer, "${matcher.group(2)}[REDACTED]")
        } else if (matcher.group(3) != null) {
            matcher.appendReplacement(stringBuffer, "Authorization: Bearer [REDACTED]")
        }
    }

    // Append the remaining part of the string
    matcher.appendTail(stringBuffer)

    // Return the redacted string
    return stringBuffer.toString()
}