package com.yeromich.pokecollect.domain.usecase

import androidx.exifinterface.media.ExifInterface
import android.graphics.Matrix
import android.graphics.Bitmap
import com.yeromich.pokecollect.data.remote.CardNameIndex
import com.yeromich.pokecollect.domain.model.nameWords
import com.yeromich.pokecollect.domain.model.nameQueries
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
    /** [query] is the name that was read, offered for a manual search if none of the candidates fit. */
    data class Success(val candidates: List<Card>, val query: String) : RecognitionResult()
    data class NotFound(val extractedName: String?, val extractedNumber: String?) : RecognitionResult()
    data class Error(val message: String) : RecognitionResult()
}

/** What OCR could read off the photo. Any field but the name may be missing. */
private data class Reading(
    /** Name candidates, most likely first: OCR sometimes ranks a nearby line above the stylised name. */
    val names: List<String>,
    val hp: Int?,
    val number: String?,
    val megaHint: Boolean,
    val exHint: Boolean,
    val compactText: String,
    /** Digits read near the collector number (O and l fixed), checked against each print's number. */
    val numberDigits: String = ""
)

// Small print near the name that is never the name itself.
private val notAName = Regex("(?i)^\\s*(entwickelt|evolves|die mega|the mega|phase|basis|basic|stage|stufe)")
// Promo numbering: set code, language, number, e.g. "MEP DE 035".
private val promoNumber = Regex("""\b(?:DE|EN|FR|IT|ES|PT|NL)\s+(\d{2,3})\b""")
private val formOfLine = Regex("(?i)form\\s+(?:von|of)\\s+([\\p{L}][\\p{L}-]{2,})")

class RecognizeCardUseCase @Inject constructor(
    private val cardRepository: CardRepository,
    @param:Named("tcgdex") private val tcgdexApi: TcgdexApi,
    private val detailsCache: CardDetailsCache,
    private val nameIndex: CardNameIndex,
    @param:ApplicationContext private val context: Context
) {
    operator fun invoke(imageFile: File): Flow<RecognitionResult> = flow {
        try {
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                ?: run { emit(RecognitionResult.Error("Could not decode photo")); return@flow }
            val image = InputImage.fromFilePath(context, Uri.fromFile(imageFile))
            val visionText = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).process(image).await()

            val firstPass = read(visionText, bitmap.height)
            if (firstPass == null) {
                emit(RecognitionResult.NotFound(null, null))
                return@flow
            }
            // The collector number is tiny; read it again from an enlarged crop of the card's corner.
            val reading = if (firstPass.number != null) firstPass
            else readNumberUpClose(imageFile, visionText)?.let { closeUp ->
                firstPass.copy(number = collectorNumber(closeUp), numberDigits = closeUp)
            } ?: firstPass
            android.util.Log.d("Recognition", "reading=${reading.copy(compactText = "")}")

            var result: Result<List<Card>> = Result.success(emptyList())
            var usedName = reading.names.first()
            for (name in reading.names) {
                result = searchWithFallbacks(name)
                usedName = name
                if (result.isFailure || !result.getOrNull().isNullOrEmpty()) break
            }
            result.fold(
                onSuccess = { cards ->
                    val ranked = rank(cards, reading)
                    android.util.Log.d("Recognition", "name=$usedName found=${cards.size} kept=${ranked.size}")
                    if (ranked.isEmpty()) emit(RecognitionResult.NotFound(reading.names.first(), reading.number))
                    else emit(RecognitionResult.Success(ranked, usedName))
                },
                onFailure = { emit(RecognitionResult.Error(it.message ?: "Search failed")) }
            )
        } catch (e: Exception) {
            emit(RecognitionResult.Error(e.message ?: "Recognition failed"))
        }
    }.flowOn(Dispatchers.IO)

    private fun read(text: Text, imageHeight: Int): Reading? {
        val blocks = text.textBlocks.filter { it.boundingBox != null }

        // Name candidates: wordy one-line blocks in the top part, tallest first, minus the small
        // print around the name ("Entwickelt sich aus …", "Phase 2") that OCR sometimes prefers.
        val candidates = blocks
            .filter { it.boundingBox!!.top < imageHeight * NAME_ZONE && it.lines.size == 1 }
            .map { it to cleanName(it.text) }
            .filter { (block, cleaned) -> cleaned.length > 2 && !notAName.containsMatchIn(block.text) }
            .sortedByDescending { (block, _) -> block.boundingBox!!.height() }
            .take(MAX_NAME_CANDIDATES)
        // "Die Mega-entwickelte Form von Fennexis" names the Pokémon even when the title is unreadable.
        val formOf = formOfLine.find(text.text)?.groupValues?.get(1)?.let { base ->
            if (text.text.contains("mega", ignoreCase = true)) "Mega $base" else base
        }
        val names = (candidates.map { it.second } + listOfNotNull(formOf)).distinct()
        if (names.isEmpty()) return null
        val first = names.first()
        val nameTop = candidates.firstOrNull()?.first?.boundingBox?.top

        // HP sits on the same line as the name, often read without its "KP" label ("„70").
        val hp = nameTop?.let { top ->
            blocks
                .filter { it != candidates.first().first && abs(it.boundingBox!!.top - top) < imageHeight * SAME_LINE }
                .firstNotNullOfOrNull { block ->
                    Regex("""\d{2,3}""").findAll(block.text).map { it.value.toInt() }
                        .firstOrNull { it in HP_RANGE && it % 10 == 0 }
                }
        }

        val number = blocks
            .filter { it.boundingBox!!.top > imageHeight * NUMBER_ZONE }
            .firstNotNullOfOrNull { collectorNumber(it.text) }

        return Reading(
            names = names,
            hp = hp,
            number = number,
            // The Mega symbol often reads as a lone "m"; newer cards spell "Mega" out.
            megaHint = nameWords(first).firstOrNull()?.lowercase() in setOf("m", "mega"),
            exHint = (hp != null && hp >= EX_MIN_HP) || nameWords(first).any { it.equals("ex", ignoreCase = true) },
            compactText = compact(text.text)
        )
    }

    /**
     * Second look at the collector number ("022/132"): crop the bottom-left corner of the card,
     * found from where the first pass saw text, enlarge it and run OCR on just that.
     */
    private suspend fun readNumberUpClose(imageFile: File, text: Text): String? = runCatching {
        val boxes = text.textBlocks.mapNotNull { it.boundingBox }
        if (boxes.isEmpty()) return null
        val photo = uprightBitmap(imageFile) ?: return null
        val left = boxes.minOf { it.left }
        val right = boxes.maxOf { it.right }
        val bottom = boxes.maxOf { it.bottom }
        val top = boxes.minOf { it.top }
        val cardWidth = right - left
        val cardHeight = bottom - top
        val crop = android.graphics.Rect(
            (left - cardWidth * 0.04f).toInt().coerceAtLeast(0),
            // Generous on purpose: when the copyright line isn't read, "bottom" lands a little high.
            (bottom - cardHeight * 0.22f).toInt().coerceAtLeast(0),
            (left + cardWidth * 0.6f).toInt().coerceAtMost(photo.width),
            (bottom + cardHeight * 0.10f).toInt().coerceAtMost(photo.height)
        )
        if (crop.width() <= 0 || crop.height() <= 0) return null
        val corner = Bitmap.createBitmap(photo, crop.left, crop.top, crop.width(), crop.height())
        val enlarged = Bitmap.createScaledBitmap(corner, corner.width * NUMBER_ZOOM, corner.height * NUMBER_ZOOM, true)
        val closeUp = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            .process(InputImage.fromBitmap(enlarged, 0)).await()
        fixDigits(closeUp.text).also { android.util.Log.d("Recognition", "closeUp='${it.take(80)}'") }
    }.getOrNull()

    private fun fixDigits(raw: String): String =
        raw.replace(Regex("(?<=[0-9OolI])[Oo](?=[0-9OolI/])|(?<=/)[Oo]|[Oo](?=[0-9]{1,2}\\s*/)|\\b[Oo](?=[0-9]{2})"), "0")
            .replace(Regex("(?<=[0-9])[lI]|[lI](?=[0-9])"), "1")

    /** The photo turned the way it was taken; ML Kit's text boxes use this orientation. */
    private fun uprightBitmap(file: File): Bitmap? {
        val raw = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        val rotation = when (ExifInterface(file.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return raw
        }
        return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(rotation) }, true)
    }

    /**
     * The collector number: "022/132" → "022", or for promos without a set size, the number after
     * the language code ("MEP DE O35" → "035"). Forgives OCR reading O for 0 and l or I for 1.
     */
    private fun collectorNumber(raw: String): String? {
        val fixed = raw.replace(Regex("(?<=[0-9OolI])[Oo](?=[0-9OolI/])|(?<=/)[Oo]|[Oo](?=[0-9]{1,2}\\s*/)|\\b[Oo](?=[0-9]{2}\\b)"), "0")
            .replace(Regex("(?<=[0-9])[lI]|[lI](?=[0-9])"), "1")
        return Regex("""(\d{1,3})\s*/\s*(\d{2,3})""").find(fixed)?.groupValues?.get(1)
            ?: promoNumber.find(fixed)?.groupValues?.get(1)
    }

    /**
     * OCR often adds stray letters (symbols, energy icons) around the name. Try the full text,
     * then without 1–2 letter fragments, then the longest word, and keep the first hit.
     */
    private suspend fun searchWithFallbacks(name: String): Result<List<Card>> {
        var last: Result<List<Card>> = Result.success(emptyList())
        for (query in nameQueries(name)) {
            last = cardRepository.searchCards(query, SEARCH_LIMIT)
            if (last.isFailure || !last.getOrNull().isNullOrEmpty()) return last
        }
        // OCR slipped a letter ("Fenneis" for "Fennexis"): look up the closest real names instead.
        val near = nameIndex.closest(name)
        android.util.Log.d("Recognition", "fuzzy=$near")
        if (near.isEmpty()) return last
        return runCatching {
            near.flatMap { cardRepository.searchCards(it, SEARCH_LIMIT).getOrThrow() }.distinctBy { it.id }
        }
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
            .map { (card, _) -> details[card.id]?.set?.name?.let { card.copy(setName = it) } ?: card }
            .take(MAX_CANDIDATES)
    }

    private fun score(card: Card, details: TcgdexCardDto?, reading: Reading): Int {
        var score = 0
        val exactNumber = reading.number != null && card.number.trimStart('0') == reading.number.trimStart('0')
        // Promo corners read messily ("MEPD0 0352"); the print's own number showing up there is enough.
        val numberSeen = card.number.length >= 3 && card.number.all { it.isDigit() } && card.number in reading.numberDigits
        if (exactNumber || numberSeen) score += NUMBER_WEIGHT
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

    private fun cleanName(raw: String): String = nameWords(raw).joinToString(" ")
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
        const val MAX_NAME_CANDIDATES = 3
        const val NUMBER_ZOOM = 3
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
