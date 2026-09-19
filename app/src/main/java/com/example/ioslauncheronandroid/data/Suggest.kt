package com.example.ioslauncheronandroid.data

fun closestMatch(query: String, candidates: Collection<String>): String? {
    val q = query.trim().lowercase()
    if (q.length < 3) return null
    val limit = maxOf(1, q.length / 3)
    var best: String? = null
    var bestDistance = Int.MAX_VALUE
    for (c in candidates) {
        val word = c.lowercase()

        val d = (word.split(' ', '-', '_') + word).minOf { editDistance(q, it) }
        if (d < bestDistance && d in 1..limit) { bestDistance = d; best = c }
    }
    return best
}

internal fun editDistance(a: String, b: String): Int {
    if (a == b) return 0
    var prev = IntArray(b.length + 1) { it }
    var cur = IntArray(b.length + 1)
    for (i in 1..a.length) {
        cur[0] = i
        for (j in 1..b.length) {
            val cost = if (a[i - 1] == b[j - 1]) 0 else 1
            cur[j] = minOf(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + cost)
        }
        val t = prev; prev = cur; cur = t
    }
    return prev[b.length]
}
