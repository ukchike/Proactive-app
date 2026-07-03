package com.unifiedproductivity.app.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class ConflictResolverTest {

    private data class Item(
        override val id: String,
        override val modifiedAt: Long,
        override val deletedAt: Long? = null
    ) : ConflictResolver.Versioned

    @Test
    fun `newer remote wins`() {
        val local = Item("a", modifiedAt = 100)
        val remote = Item("a", modifiedAt = 200)
        assertEquals(ConflictResolver.Resolution.TAKE_REMOTE, ConflictResolver.resolve(local, remote))
    }

    @Test
    fun `newer local wins`() {
        val local = Item("a", modifiedAt = 300)
        val remote = Item("a", modifiedAt = 200)
        assertEquals(ConflictResolver.Resolution.KEEP_LOCAL, ConflictResolver.resolve(local, remote))
    }

    @Test
    fun `tie keeps local`() {
        val local = Item("a", modifiedAt = 200)
        val remote = Item("a", modifiedAt = 200)
        assertEquals(ConflictResolver.Resolution.KEEP_LOCAL, ConflictResolver.resolve(local, remote))
    }

    @Test
    fun `missing remote keeps local`() {
        val local = Item("a", modifiedAt = 200)
        assertEquals(ConflictResolver.Resolution.KEEP_LOCAL, ConflictResolver.resolve(local, null))
    }

    @Test
    fun `missing local takes remote`() {
        val remote = Item("a", modifiedAt = 200)
        assertEquals(ConflictResolver.Resolution.TAKE_REMOTE, ConflictResolver.resolve(null, remote))
    }

    @Test
    fun `merge combines by id and resolves per item`() {
        val local = listOf(Item("a", 100), Item("b", 500))
        val remote = listOf(Item("a", 200), Item("c", 50))
        val merged = ConflictResolver.merge(local, remote).associateBy { it.id }

        assertEquals(3, merged.size)
        assertEquals(200, merged.getValue("a").modifiedAt) // remote newer
        assertEquals(500, merged.getValue("b").modifiedAt) // only local
        assertEquals(50, merged.getValue("c").modifiedAt)  // only remote
    }
}
