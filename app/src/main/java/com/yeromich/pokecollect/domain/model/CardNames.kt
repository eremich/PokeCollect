package com.yeromich.pokecollect.domain.model

private val dashes = Regex("[‐-―−-]")
private val gluedWords = Regex("(?<=\\p{Ll})(?=\\p{Lu})")
private val spaces = Regex("\\s+")
private val tagWords = setOf("mega", "ex", "gx", "v", "vmax", "vstar")

/** Splits a loosely written card name into words: "Mega-Camerupt ex" and "MegaCamerupt" both become [Mega, Camerupt, …]. */
fun nameWords(raw: String): List<String> =
    raw.replace(dashes, " ").replace(gluedWords, " ").split(spaces).filter { it.isNotBlank() }

/**
 * Searches to try, best first, for a name read by OCR or typed by hand. TCGdex /de stores names
 * hyphenated ("Mega-Camerupt-ex") and only matches substrings, so "Mega Camerupt" finds nothing.
 * Try the hyphenated form, then the plain Pokémon name without Mega/ex tags or stray letters.
 */
fun nameQueries(raw: String): List<String> {
    val words = nameWords(raw)
    if (words.isEmpty()) return emptyList()
    val core = words.filter { it.length > 2 && it.lowercase() !in tagWords }
    return listOfNotNull(
        words.joinToString("-"),
        words.joinToString(" "),
        core.joinToString("-").ifBlank { null },
        core.joinToString(" ").ifBlank { null },
        core.maxByOrNull { it.length }
    ).distinct()
}
