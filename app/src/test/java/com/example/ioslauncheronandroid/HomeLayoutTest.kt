package com.example.ioslauncheronandroid

import com.example.ioslauncheronandroid.data.AppItem
import com.example.ioslauncheronandroid.data.FolderItem
import com.example.ioslauncheronandroid.data.HomeLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HomeLayoutTest {
    @Before
    fun reset() {
        HomeLayout.items.clear()
    }

    private fun keys() = HomeLayout.items.map { it.id }

    @Test
    fun sync_appendsNewAppsInDefaultOrder() {
        HomeLayout.sync(listOf("a", "b", "c"))
        assertEquals(listOf("a", "b", "c"), keys())
        HomeLayout.sync(listOf("a", "b", "c", "d"))
        assertEquals(listOf("a", "b", "c", "d"), keys())
    }

    @Test
    fun sync_prunesUninstalledApps_andKeepsUserOrder() {
        HomeLayout.sync(listOf("a", "b", "c"))
        HomeLayout.move(0, 2)
        assertEquals(listOf("b", "c", "a"), keys())
        HomeLayout.sync(listOf("a", "c"))
        assertEquals(listOf("c", "a"), keys())
    }

    @Test
    fun move_reordersAndClampsTheTarget() {
        HomeLayout.sync(listOf("a", "b", "c"))
        HomeLayout.move(2, 0)
        assertEquals(listOf("c", "a", "b"), keys())
        HomeLayout.move(0, 99)
        assertEquals(listOf("a", "b", "c"), keys())
    }

    @Test
    fun mergeInto_appMakesFolder_thenFolderTakesMore() {
        HomeLayout.sync(listOf("a", "b", "c"))
        assertTrue(HomeLayout.mergeInto("a", "b"))
        val folder = HomeLayout.items[0] as FolderItem
        assertEquals(listOf("a", "b"), folder.keys)
        assertEquals(2, HomeLayout.items.size)

        assertTrue(HomeLayout.mergeInto(folder.id, "c"))
        assertEquals(listOf("a", "b", "c"), (HomeLayout.items.single() as FolderItem).keys)
    }

    @Test
    fun mergeInto_rejectsSelfAndDuplicates() {
        HomeLayout.sync(listOf("a", "b"))
        assertFalse(HomeLayout.mergeInto("a", "a"))
        assertFalse(HomeLayout.mergeInto("missing", "a"))
    }

    @Test
    fun removeFromFolder_dissolvesFoldersThatShrinkBelowTwo() {
        HomeLayout.sync(listOf("a", "b"))
        HomeLayout.mergeInto("a", "b")
        val id = HomeLayout.items[0].id
        HomeLayout.removeFromFolder(id, "b")
        assertEquals(listOf("a", "b"), keys())
        assertTrue(HomeLayout.items.all { it is AppItem })
    }

    @Test
    fun removeFolder_unfoldsAppsInPlace() {
        HomeLayout.sync(listOf("x", "a", "b", "y"))
        HomeLayout.mergeInto("a", "b")
        HomeLayout.removeFolder(HomeLayout.items[1].id)
        assertEquals(listOf("x", "a", "b", "y"), keys())
        assertNull(HomeLayout.folderOf("a"))
    }

    @Test
    fun sync_dissolvesFoldersWhoseAppsWereUninstalled() {
        HomeLayout.sync(listOf("a", "b", "c"))
        HomeLayout.mergeInto("a", "b")
        HomeLayout.sync(listOf("a", "c"))
        assertEquals(listOf("a", "c"), keys())
        assertTrue(HomeLayout.items.all { it is AppItem })
    }
}
