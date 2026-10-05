package com.yeromich.pokecollect.domain.model

import com.yeromich.pokecollect.data.remote.TcgdexCardmarketDto
import java.text.NumberFormat
import java.util.Locale

// Cardmarket is a euro market, so prices read the way German sellers write them: "7,30 €".
private val euro: NumberFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY)

fun formatEur(amount: Double): String = euro.format(amount)

/**
 * Cardmarket trend for one variant, in euros. Foil variants trade at their own price when
 * Cardmarket tracks it; TCGdex reports "no data" as 0, which is never a real price.
 */
fun TcgdexCardmarketDto?.trendEur(variant: String): Double? {
    if (this == null) return null
    val foil = variant.contains("holo", ignoreCase = true) || variant.contains("reverse", ignoreCase = true)
    val holo = trendHolo?.takeIf { it > 0.0 }
    return if (foil && holo != null) holo else trend?.takeIf { it > 0.0 }
}
