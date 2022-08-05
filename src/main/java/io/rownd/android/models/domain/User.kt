package io.rownd.android.models.domain

import io.rownd.android.util.AnyValueSerializer
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val data: Map<String, @Serializable(with = AnyValueSerializer::class) Any?> = HashMap<String, Any?>(),
    val redacted: PersistentList<String> = persistentListOf()
)