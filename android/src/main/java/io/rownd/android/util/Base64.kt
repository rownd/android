package io.rownd.android.util

import java.util.Base64

fun ByteArray.toBase64(): String = String(Base64.getEncoder().encode(this))