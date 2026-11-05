package com.yeromich.pokecollect.ui.components

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.stringResource
import com.yeromich.pokecollect.R
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
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
private const val SELECTED_SCALE = 0.92f
private const val SELECT_MS = 160
private const val UNSELECTED_FILL_ALPHA = 0.45f

/** A card tile; [priceLabel], when given, sits as a small tag in the bottom corner. */
@Composable
fun CardGridItem(
    card: Card,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    priceLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    /** null outside selection mode; otherwise whether this card is ticked. */
    selected: Boolean? = null,
    /** Shows a small tick: the card is already in the collection. */
    owned: Boolean = false
) {
    val scale by animateFloatAsState(if (selected == true) SELECTED_SCALE else 1f, tween(SELECT_MS), label = "selectScale")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CardShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
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
        if (owned && selected == null) {
            OwnedCorner(modifier = Modifier.align(Alignment.BottomEnd))
        }
        if (selected != null) {
            SelectionMark(selected = selected, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
        }
    }
}

@Composable
private fun SelectionMark(selected: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val fill by animateColorAsState(if (selected) colors.primary else Ink.copy(alpha = UNSELECTED_FILL_ALPHA), tween(SELECT_MS), label = "markFill")
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(26.dp)
            .background(fill, CircleShape)
            .border(1.5.dp, if (selected) colors.primary else Paper, CircleShape)
    ) {
        if (selected) {
            Icon(painterResource(R.drawable.ic_check_bold), contentDescription = null, tint = Ink, modifier = Modifier.size(14.dp))
        }
    }
}

/**
 * A coral folded corner with a tick, bottom-right, on cards the user owns. The card itself keeps
 * full colour, so owned and missing cards look equally tappable.
 */
@Composable
private fun OwnedCorner(modifier: Modifier = Modifier) {
    val label = stringResource(R.string.card_owned)
    val coral = MaterialTheme.colorScheme.primary
    Box(
        contentAlignment = Alignment.BottomEnd,
        modifier = modifier
            .size(34.dp)
            .semantics { contentDescription = label }
            .drawBehind {
                val corner = Path().apply {
                    moveTo(size.width, 0f)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(corner, coral)
            }
    ) {
        Icon(
            painterResource(R.drawable.ic_check_bold),
            contentDescription = null,
            tint = Ink,
            modifier = Modifier
                .padding(end = 4.dp, bottom = 4.dp)
                .size(12.dp)
        )
    }
}
