package com.yeromich.pokecollect.data.remote

import android.content.Context
import com.yeromich.pokecollect.domain.model.nameWords
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

private val MAX_AGE_MS = TimeUnit.DAYS.toMillis(7)
private const val MIN_SIMILARITY = 0.7
private val tagWords = setOf("mega", "ex", "gx", "v", "vmax", "vstar")

/**
 * Every distinct card name on TCGdex (about 4,300), kept in a small file and refreshed weekly.
 * TCGdex only matches exact substrings, so one OCR slip ("Fenneis" for "Fennexis") finds nothing;
 * this index finds the closest real names instead.
 */
@Singleton
class CardNameIndex @Inject constructor(
    @param:Named("tcgdex") private val api: TcgdexApi,
    @param:ApplicationContext private val context: Context
) {
    private val file get() = File(context.filesDir, "card_names.txt")
    private val lock = Mutex()
    private var names: List<String>? = null

    /** Up to [limit] real card names closest to [raw], best first; empty when nothing is close. */
    suspend fun closest(raw: String, limit: Int = 3): List<String> {
        val all = load()
        val query = normalise(raw)
        val queryCore = core(raw)
        if (query.isEmpty()) return emptyList()
        return withContext(Dispatchers.Default) {
            all.asSequence()
                .map { name ->
                    // Compare whole names and bare Pokémon names, so stray tags like "exa" don't sink a match.
                    name to maxOf(similarity(query, normalise(name)), if (queryCore.isEmpty()) 0.0 else similarity(queryCore, core(name)))
                }
                .filter { it.second >= MIN_SIMILARITY }
                .sortedByDescending { it.second }
                .take(limit)
                .map { it.first }
                .toList()
        }
    }

    private suspend fun load(): List<String> = lock.withLock {
        names?.let { return it }
        val cached = file.takeIf { it.exists() }
        val fresh = cached != null && System.currentTimeMillis() - cached.lastModified() < MAX_AGE_MS
        val loaded = if (fresh) {
            withContext(Dispatchers.IO) { cached!!.readLines() }
        } else {
            runCatching {
                val list = api.searchCards().map { it.name }.distinct().sorted()
                withContext(Dispatchers.IO) { file.writeText(list.joinToString("\n")) }
                list
            }.getOrElse { cached?.let { withContext(Dispatchers.IO) { it.readLines() } }.orEmpty() }
        }
        loaded.filter { it.isNotBlank() }.also { if (it.isNotEmpty()) names = it }
    }

    private fun normalise(s: String) = nameWords(s).joinToString(" ").lowercase()

    private fun core(s: String) = nameWords(s).filter { it.length > 2 && it.lowercase() !in tagWords }.joinToString(" ").lowercase()

    private fun similarity(a: String, b: String): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        return 1.0 - levenshtein(a, b).toDouble() / maxOf(a.length, b.length)
    }

    private fun levenshtein(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + cost)
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.length]
    }
}
