package com.yeromich.pokecollect.data.remote

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardDetailsCache @Inject constructor() {

    private val entries = object : LinkedHashMap<String, TcgdexCardDto>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, TcgdexCardDto>) =
            size > MAX_ENTRIES
    }

    @Synchronized
    operator fun get(id: String): TcgdexCardDto? = entries[id]

    @Synchronized
    operator fun set(id: String, details: TcgdexCardDto) {
        entries[id] = details
    }

    private companion object {
        const val MAX_ENTRIES = 100
    }
}
