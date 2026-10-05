package com.yeromich.pokecollect.domain.usecase

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.yeromich.pokecollect.data.remote.CardDetailsCache
import com.yeromich.pokecollect.data.remote.TcgdexApi
import com.yeromich.pokecollect.data.remote.TcgdexCardDto
import com.yeromich.pokecollect.data.repository.CardRepository
import com.yeromich.pokecollect.domain.model.Card
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject
import javax.inject.Named
import kotlin.math.abs

sealed class RecognitionResult {
    data class Success(val candidates: List<Card>) : RecognitionResult()
    data class NotFound(val extractedName: String?, val extractedNumber: String?) : RecognitionResult()
    data class Error(val message: String) : RecognitionResult()
}

/** What OCR could read off the photo. Any field but the name may be missing. */
private data class Reading(
    val name: String,
    val hp: Int?,
    val number: String?,
    val megaHint: Boolean,
    val exHint: Boolean,
    val compactText: String
)

class RecognizeCardUseCase @Inject constructor(
    private val cardRepository: CardRepository,
    @param:Named("tcgdex") private val tcgdexApi: TcgdexApi,
    private val detailsCache: CardDetailsCache,
    @param:ApplicationContext private val context: Context
) {
    operator fun invoke(imageFile: File): Flow<RecognitionResult> = flow {
        try {
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                ?: run { emit(RecognitionResult.Error("Could not decode photo")); return@flow }
            val image = InputImage.fromFilePath(context, Uri.fromFile(imageFile))
            val visionText = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image).await()

            val reading = read(visionText, bitmap.height)
            android.util.Log.d("Recognition", "reading=${reading?.copy(compactText = "")}")
            if (reading == null) {
                emit(RecognitionResult.NotFound(null, null))
                return@flow
            }

            searchWithFallbacks(reading.name).fold(
                onSuccess = { cards ->
                    val ranked = rank(cards, reading)
                    android.util.Log.d("Recognition", "found=${cards.size} kept=${ranked.size}")
                    if (ranked.isEmpty()) emit(RecognitionResult.NotFound(reading.name, reading.number))
                    else emit(RecognitionResult.Success(ranked))
                },
                onFailure = { emit(RecognitionResult.Error(it.message ?: "Search failed")) }
            )
        } catch (e: Exception) {
            emit(RecognitionResult.Error(e.message ?: "Recognition failed"))
        }
    }.flowOn(Dispatchers.IO)

    private fun read(text: Text, imageHeight: Int): Reading? {
        val blocks = text.textBlocks.filter { it.boundingBox != null }

        // The name is the tallest wordy block in the top part of the photo.
        val (nameBlock, rawName) = blocks
            .filter { it.boundingBox!!.top < imageHeight * NAME_ZONE }
            .map { it to cleanName(it.text) }
            .filter { (block, cleaned) -> cleaned.length > 2 && block.lines.size == 1 }
            .maxByOrNull { (block, _) -> block.boundingBox!!.height() }
            ?: return null
        val nameTop = nameBlock.boundingBox!!.top

        // HP sits on the same line as the name, often read without its "KP" label ("„70").
        val hp = blocks
            .filter { it != nameBlock && abs(it.boundingBox!!.top - nameTop) < imageHeight * SAME_LINE }
            .firstNotNullOfOrNull { block ->
                Regex("""\d{2,3}""").findAll(block.text).map { it.value.toInt() }
                    .firstOrNull { it in HP_RANGE && it % 10 == 0 }
            }

        val number = blocks
            .filter { it.boundingBox!!.top > imageHeight * NUMBER_ZONE }
            .firstNotNullOfOrNull { Regex("""(\d{1,3})\s*/\s*(\d{1,3})""").find(it.text)?.groupValues?.get(1) }

        return Reading(
            name = rawName.replace(Regex("(?i)\\s*(ex|GX|V|VMAX|VSTAR|X)$"), "").trim(),
            hp = hp,
            number = number,
            // The Mega symbol in front of the name reads as a lone "m".
            megaHint = rawName.split(Regex("\\s+")).firstOrNull().equals("m", ignoreCase = true),
            exHint = hp != null && hp >= EX_MIN_HP,
            compactText = compact(text.text)
        )
    }

    /**
     * OCR often adds stray letters (symbols, energy icons) around the name. Try the full text,
     * then without 1–2 letter fragments, then the longest word, and keep the first hit.
     */
    private suspend fun searchWithFallbacks(name: String): Result<List<Card>> {
        val words = name.split(Regex("\\s+")).filter { it.isNotBlank() }
        val queries = listOfNotNull(
            name,
            words.filter { it.length > 2 }.joinToString(" ").ifBlank { null },
            words.maxByOrNull { it.length }
        ).distinct()
        var last: Result<List<Card>> = Result.success(emptyList())
        for (query in queries) {
            last = cardRepository.searchCards(query, SEARCH_LIMIT)
            if (last.isFailure || !last.getOrNull().isNullOrEmpty()) return last
        }
        return last
    }

    /**
     * One Pokémon has many prints, so the name alone is ambiguous. Score each print by how well its
     * HP, attack names and collector number agree with the photo, and keep only the best matches.
     */
    private suspend fun rank(cards: List<Card>, reading: Reading): List<Card> {
        if (cards.size <= 1) return cards
        val details = fetchDetails(cards.take(MAX_DETAILED))
        val scored = cards.map { card -> card to score(card, details[card.id], reading) }
        val best = scored.maxOf { it.second }
        val kept = if (best > 0) scored.filter { it.second == best } else scored
        return kept
            .sortedByDescending { (card, _) -> nameHints(card.name, reading) }
            .map { it.first }
            .take(MAX_CANDIDATES)
    }

    private fun score(card: Card, details: TcgdexCardDto?, reading: Reading): Int {
        var score = 0
        if (reading.number != null && card.number.trimStart('0') == reading.number.trimStart('0')) score += NUMBER_WEIGHT
        if (details == null) return score
        if (reading.hp != null && details.hp == reading.hp) score += HP_WEIGHT
        details.attacks.orEmpty().forEach { attack ->
            if (fuzzyContains(reading.compactText, compact(attack.name))) score += ATTACK_WEIGHT
        }
        return score
    }

    private fun nameHints(name: String, reading: Reading): Int {
        // TCGdex /de names look like "Mega-Absol-ex", "Absol-ex", "Absol".
        val isMega = name.startsWith("Mega", ignoreCase = true)
        val isEx = name.endsWith("ex", ignoreCase = true)
        return (if (isMega == reading.megaHint) 2 else 0) + (if (isEx == reading.exHint) 1 else 0)
    }

    private suspend fun fetchDetails(cards: List<Card>): Map<String, TcgdexCardDto> = coroutineScope {
        cards.map { card ->
            async {
                detailsCache[card.id]
                    ?: runCatching { tcgdexApi.getCard(card.id) }.getOrNull()?.also { detailsCache[card.id] = it }
            }
        }.awaitAll().filterNotNull().associateBy { it.id }
    }

    private fun cleanName(raw: String): String = raw
        .replace(Regex("(?i)(PHASE\\s*\\d?|BASIS|BASIC|STAGE\\s*\\d?|KP|HP|\\d+|[^\\p{L}\\s])"), "")
        .trim()

    private fun compact(text: String): String = text.lowercase().filter { it.isLetter() }

    /** True when [needle] appears in [haystack] allowing roughly one OCR slip per six letters. */
    private fun fuzzyContains(haystack: String, needle: String): Boolean {
        if (needle.length < MIN_ATTACK_LETTERS) return false
        if (needle in haystack) return true
        val allowed = needle.length / LETTERS_PER_TYPO
        if (allowed == 0 || haystack.length < needle.length) return false
        return (0..haystack.length - needle.length).any { start ->
            var mismatches = 0
            for (i in needle.indices) {
                if (haystack[start + i] != needle[i] && ++mismatches > allowed) break
            }
            mismatches <= allowed
        }
    }

    private companion object {
        const val NAME_ZONE = 0.45f
        const val NUMBER_ZONE = 0.65f
        const val SAME_LINE = 0.04f
        val HP_RANGE = 30..340
        const val EX_MIN_HP = 180
        const val SEARCH_LIMIT = 100
        const val MAX_DETAILED = 30
        const val MAX_CANDIDATES = 20
        const val NUMBER_WEIGHT = 5
        const val ATTACK_WEIGHT = 3
        const val HP_WEIGHT = 2
        const val MIN_ATTACK_LETTERS = 4
        const val LETTERS_PER_TYPO = 6
    }
}
