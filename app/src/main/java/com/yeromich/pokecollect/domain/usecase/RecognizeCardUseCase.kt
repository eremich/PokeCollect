package com.yeromich.pokecollect.domain.usecase

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.yeromich.pokecollect.data.repository.CardRepository
import com.yeromich.pokecollect.domain.model.Card
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject

sealed class RecognitionResult {
    data class Success(val candidates: List<Card>) : RecognitionResult()
    data class NotFound(val extractedName: String?, val extractedNumber: String?) : RecognitionResult()
    data class Error(val message: String) : RecognitionResult()
}

class RecognizeCardUseCase @Inject constructor(
    private val cardRepository: CardRepository,
    @param:ApplicationContext private val context: Context
) {
    operator fun invoke(imageFile: File): Flow<RecognitionResult> = flow {
        try {
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                ?: run { emit(RecognitionResult.Error("Не удалось прочитать снимок")); return@flow }

            val image = InputImage.fromFilePath(context, Uri.fromFile(imageFile))
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val visionText = recognizer.process(image).await()

            val imageHeight = bitmap.height

            var extractedName: String? = null
            var maxWidth = 0f
            for (block in visionText.textBlocks) {
                val blockTop = block.boundingBox?.top ?: continue
                if (blockTop < imageHeight * 0.25f) {
                    val blockWidth = block.boundingBox?.width() ?: 0
                    val cleaned = block.text
                        .replace(Regex("(?i)(PHASE|BASIC|STAGE|KP|HP|\\d+)"), "")
                        .trim()
                    if (cleaned.length > 2 && blockWidth > maxWidth) {
                        maxWidth = blockWidth.toFloat()
                        extractedName = cleaned
                    }
                }
            }

            var extractedNumber: String? = null
            val numberRegex = Regex("""(\d{1,3})\s*/\s*(\d{1,3})""")
            for (block in visionText.textBlocks) {
                val blockTop = block.boundingBox?.top ?: continue
                if (blockTop > imageHeight * 0.65f) {
                    val match = numberRegex.find(block.text)
                    if (match != null) {
                        extractedNumber = match.groupValues[1]
                        break
                    }
                }
            }

            android.util.Log.d("Recognition", "name=$extractedName number=$extractedNumber")

            if (extractedName != null) {
                val result = cardRepository.searchCards(extractedName, 20)
                result.fold(
                    onSuccess = { cards ->
                        android.util.Log.d("Recognition", "found=${cards.size}")
                        val filtered = if (extractedNumber != null)
                            cards.filter {
                                it.number == extractedNumber ||
                                        it.number == extractedNumber.padStart(3, '0')
                            }
                        else cards
                        val final = if (filtered.isEmpty()) cards else filtered
                        if (final.isEmpty()) emit(RecognitionResult.NotFound(extractedName, extractedNumber))
                        else emit(RecognitionResult.Success(final))
                    },
                    onFailure = { emit(RecognitionResult.Error(it.message ?: "Ошибка")) }
                )
            } else {
                emit(RecognitionResult.NotFound(null, extractedNumber))
            }
        } catch (e: Exception) {
            emit(RecognitionResult.Error(e.message ?: "Неизвестная ошибка"))
        }
    }.flowOn(Dispatchers.IO)
}
