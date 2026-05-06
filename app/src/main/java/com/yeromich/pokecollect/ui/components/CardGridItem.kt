package com.yeromich.pokecollect.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.yeromich.pokecollect.domain.model.Card

@Composable
fun CardGridItem(card: Card, onClick: () -> Unit) {
    AsyncImage(
        model = card.imageSmall,
        contentDescription = card.name,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .clickable(onClick = onClick),
        contentScale = ContentScale.Crop
    )
}
