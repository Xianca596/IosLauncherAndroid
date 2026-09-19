package com.example.ioslauncheronandroid.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.snapshots.SnapshotStateList
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

sealed interface HomeItem { val id: String }
data class AppItem(val key: String) : HomeItem { override val id: String get() = key }
data class FolderItem(override val id: String, val name: String, val keys: List<String>) : HomeItem

object HomeLayout {
    val items = SnapshotStateList<HomeItem>()
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences("home_layout", Context.MODE_PRIVATE)
        runCatching {
            val arr = JSONArray(prefs.getString("layout", "[]"))
            val loaded = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                if (o.getString("t") == "a") AppItem(o.getString("k"))
                else FolderItem(
                    o.getString("id"), o.getString("n"),
                    o.getJSONArray("k").let { k -> (0 until k.length()).map { k.getString(it) } },
                )
            }
            items.clear(); items.addAll(loaded)
        }
    }

    fun save() {
        if (!::prefs.isInitialized) return
        val arr = JSONArray()
        items.forEach {
            arr.put(
                when (it) {
                    is AppItem -> JSONObject().put("t", "a").put("k", it.key)
                    is FolderItem -> JSONObject().put("t", "f").put("id", it.id).put("n", it.name).put("k", JSONArray(it.keys))
                },
            )
        }
        prefs.edit().putString("layout", arr.toString()).apply()
    }

    fun sync(available: List<String>) {
        val avail = available.toSet()
        val cleaned = items.mapNotNull { item ->
            when (item) {
                is AppItem -> item.takeIf { it.key in avail }
                is FolderItem -> item.keys.filter { it in avail }.let { k ->
                    when {
                        k.size >= 2 -> item.copy(keys = k)
                        k.size == 1 -> AppItem(k[0])
                        else -> null
                    }
                }
            }
        }
        val placed = cleaned.flatMap { if (it is AppItem) listOf(it.key) else (it as FolderItem).keys }.toSet()
        val next = cleaned + available.filter { it !in placed }.map { AppItem(it) }
        if (next != items.toList()) {
            items.clear(); items.addAll(next); save()
        }
    }

    fun move(from: Int, to: Int) {
        if (from == to || from !in items.indices) return
        val it = items.removeAt(from)
        items.add(to.coerceIn(0, items.size), it)
    }

    fun mergeInto(targetId: String, key: String): Boolean {
        val ti = items.indexOfFirst { it.id == targetId }
        val di = items.indexOfFirst { it is AppItem && it.key == key }
        if (ti < 0 || di < 0 || ti == di) return false
        val merged: HomeItem = when (val t = items[ti]) {
            is AppItem -> FolderItem(UUID.randomUUID().toString(), "Folder", listOf(t.key, key))
            is FolderItem -> if (t.keys.size >= 9 || key in t.keys) return false else t.copy(keys = t.keys + key)
        }
        items[ti] = merged
        items.removeAt(di)
        save()
        return true
    }

    fun folderOf(key: String): FolderItem? = items.filterIsInstance<FolderItem>().firstOrNull { key in it.keys }

    fun renameFolder(id: String, name: String) {
        val i = items.indexOfFirst { it.id == id }
        if (i >= 0) { items[i] = (items[i] as FolderItem).copy(name = name); save() }
    }

    fun removeFolder(id: String) {
        val i = items.indexOfFirst { it.id == id }
        if (i < 0) return
        val f = items[i] as FolderItem
        items.removeAt(i)
        items.addAll(i, f.keys.map { AppItem(it) })
        save()
    }

    fun removeFromFolder(folderId: String, key: String) {
        val i = items.indexOfFirst { it.id == folderId }
        if (i < 0) return
        val f = items[i] as FolderItem
        val rest = f.keys - key
        items.removeAt(i)
        val replacement: List<HomeItem> =
            if (rest.size >= 2) listOf(f.copy(keys = rest), AppItem(key)) else rest.map { AppItem(it) } + AppItem(key)
        items.addAll(i, replacement)
        save()
    }
}
