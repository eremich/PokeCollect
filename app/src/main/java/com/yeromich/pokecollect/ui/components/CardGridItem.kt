package com.yeromich.pokecollect.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.yeromich.pokecollect.domain.model.Card
import com.yeromich.pokecollect.ui.theme.Ink
import com.yeromich.pokecollect.ui.theme.Paper

private const val PRICE_TAG_ALPHA = 0.78f

/** A card tile; [priceLabel], when given, sits as a small tag in the bottom corner. */
@Composable
fun CardGridItem(card: Card, onClick: () -> Unit, modifier: Modifier = Modifier, priceLabel: String? = null) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .clip(CardShape)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = card.imageSmall,
            contentDescription = card.name,
            modifier = Modifier
                .fillMaxSize()
                .gridShimmer(enabled = isFoilVariant(card.variant)),
            contentScale = ContentScale.Crop
        )
        if (priceLabel != null) {
            Text(
                text = priceLabel,
                style = MaterialTheme.typography.labelMedium,
                color = Paper,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Ink.copy(alpha = PRICE_TAG_ALPHA), CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}
