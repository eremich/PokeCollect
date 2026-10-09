package com.yeromich.pokecollect.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// Dark only: cards and foil read best against a near-black backdrop.
private val HoloColorScheme = darkColorScheme(
    primary = Coral,
    onPrimary = CoralDeep,
    primaryContainer = CoralContainer,
    onPrimaryContainer = OnCoralContainer,
    secondary = Teal,
    onSecondary = TealDeep,
    secondaryContainer = TealContainer,
    onSecondaryContainer = Teal,
    tertiary = Teal,
    onTertiary = TealDeep,
    background = Ink,
    onBackground = Paper,
    surface = Ink,
    onSurface = Paper,
    surfaceVariant = InkHigh,
    onSurfaceVariant = PaperMuted,
    surfaceDim = Ink,
    surfaceBright = InkHighest,
    surfaceContainerLowest = Ink,
    surfaceContainerLow = InkRaised,
    surfaceContainer = InkRaised,
    surfaceContainerHigh = InkHigh,
    surfaceContainerHighest = InkHighest,
    outline = Hairline,
    outlineVariant = InkHighest,
    error = Danger,
    onError = DangerDeep,
    errorContainer = DangerContainer,
    onErrorContainer = Paper,
    inverseSurface = Paper,
    inverseOnSurface = Ink,
    inversePrimary = CoralContainer,
    scrim = Ink
)

private val HoloShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PokeCollectTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HoloColorScheme,
        typography = Typography,
        shapes = HoloShapes,
        content = content
    )
}
