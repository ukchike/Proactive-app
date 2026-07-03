package com.unifiedproductivity.app.sync

/**
 * Resolves sync conflicts between a local and remote copy of the same entity using
 * last-write-wins (the spec's chosen strategy). A soft delete always wins over an
 * older edit so deletions propagate across devices.
 */
object ConflictResolver {

    /** Minimal view of a syncable entity the resolver needs to compare versions. */
    interface Versioned {
        val id: String
        val modifiedAt: Long
        val deletedAt: Long?
    }

    enum class Resolution { KEEP_LOCAL, TAKE_REMOTE }

    fun <T : Versioned> resolve(local: T?, remote: T?): Resolution {
        if (remote == null) return Resolution.KEEP_LOCAL
        if (local == null) return Resolution.TAKE_REMOTE

        // A deletion is represented by a later modifiedAt already (soft delete bumps
        // modifiedAt), so a straight timestamp comparison is sufficient. Ties keep local
        // to avoid pointless writes.
        return if (remote.modifiedAt > local.modifiedAt) Resolution.TAKE_REMOTE
        else Resolution.KEEP_LOCAL
    }

    /** Merge two collections keyed by id, resolving each pair independently. */
    fun <T : Versioned> merge(local: List<T>, remote: List<T>): List<T> {
        val byId = LinkedHashMap<String, T>()
        local.forEach { byId[it.id] = it }
        remote.forEach { remoteItem ->
            val localItem = byId[remoteItem.id]
            byId[remoteItem.id] = when (resolve(localItem, remoteItem)) {
                Resolution.TAKE_REMOTE -> remoteItem
                Resolution.KEEP_LOCAL -> localItem ?: remoteItem
            }
        }
        return byId.values.toList()
    }
}
