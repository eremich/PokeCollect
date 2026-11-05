package com.yeromich.pokecollect.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** A row of pill buttons where exactly one option is chosen, such as a sort order. */
@Composable
fun <T> ChoicePills(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.selectableGroup()) {
        options.forEach { (value, label) ->
            ChoicePill(label = label, selected = value == selected, onClick = { onSelect(value) })
        }
    }
}

@Composable
private fun ChoicePill(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(if (selected) colors.primaryContainer else colors.surfaceContainerHigh, label = "pillBg")
    val content by animateColorAsState(if (selected) colors.primary else colors.onSurfaceVariant, label = "pillFg")
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = content,
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    )
}
