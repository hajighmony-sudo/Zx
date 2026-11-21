package dev.cluvex.zedsecure.ui.zx

import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import dev.cluvex.zedsecure.domain.model.ZxAnim
import dev.cluvex.zedsecure.ui.theme.LocalMotionBudget
import dev.cluvex.zedsecure.ui.theme.MotionBudget

/** Effective animation level after the user's setting and the device's motion budget are combined. */
enum class ZxFx { Min, Balanced, Max }

@Immutable
class ZxColors(
    val dark: Boolean,
    val ground: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val ink: Color,
    val muted: Color,
    val neonA: Color,
    val neonB: Color,
    val neonC: Color,
    val onA: Color,
    val onB: Color,
    val onC: Color,
    val danger: Color,
    val good: Color,
    val grid: Color,
)

object ZxPalettes {
    /** Neon Night: deep violet-black with cyan, magenta and cyber yellow. */
    val Night = ZxColors(
        dark = true,
        ground = Color(0xFF050711),
        surface = Color(0xFF0B1020),
        surface2 = Color(0xFF121C32),
        line = Color(0xFF263A58),
        ink = Color(0xFFEAF4FF),
        muted = Color(0xFF8195B4),
        neonA = Color(0xFF62E7FF),
        neonB = Color(0xFFB56BFF),
        neonC = Color(0xFFFFC857),
        onA = Color(0xFF031018),
        onB = Color(0xFF160A24),
        onC = Color(0xFF241800),
        danger = Color(0xFFFF5578),
        good = Color(0xFF55F0B0),
        grid = Color(0xFF62E7FF),
    )

    /** Neon Day: cool industrial paper with electric blue, hot magenta and caution yellow. */
    val Day = ZxColors(
        dark = false,
        ground = Color(0xFFF0F4FA),
        surface = Color(0xFFFAFCFF),
        surface2 = Color(0xFFE3ECF7),
        line = Color(0xFFB7C8DD),
        ink = Color(0xFF101A2A),
        muted = Color(0xFF53657E),
        neonA = Color(0xFF087FA8),
        neonB = Color(0xFF7540B8),
        neonC = Color(0xFFB66A00),
        onA = Color(0xFFFFFFFF),
        onB = Color(0xFFFFFFFF),
        onC = Color(0xFFFFFFFF),
        danger = Color(0xFFC62E53),
        good = Color(0xFF087E58),
        grid = Color(0xFF087FA8),
    )

    fun of(dark: Boolean): ZxColors = if (dark) Night else Day
}

@Immutable
class ZxStyle(val colors: ZxColors, val anim: ZxAnim)

val LocalZxStyle = staticCompositionLocalOf { ZxStyle(ZxPalettes.Night, ZxAnim.Auto) }

object Zx {
    val colors: ZxColors
        @Composable get() = LocalZxStyle.current.colors

    val fx: ZxFx
        @Composable get() {
            val budget = LocalMotionBudget.current
            return when (LocalZxStyle.current.anim) {
                ZxAnim.Min -> ZxFx.Min
                ZxAnim.Balanced -> if (budget == MotionBudget.Paused) ZxFx.Min else ZxFx.Balanced
                ZxAnim.Max -> if (budget == MotionBudget.Paused) ZxFx.Min else ZxFx.Max
                ZxAnim.Auto -> when (budget) {
                    MotionBudget.Full -> ZxFx.Max
                    MotionBudget.Throttled -> ZxFx.Balanced
                    MotionBudget.Paused -> ZxFx.Min
                }
            }
        }
}

val ZxShapes = Shapes(
    extraSmall = CutCornerShape(4.dp),
    small = CutCornerShape(8.dp),
    medium = CutCornerShape(12.dp),
    large = CutCornerShape(16.dp),
    largeIncreased = CutCornerShape(20.dp),
    extraLarge = CutCornerShape(24.dp),
    extraLargeIncreased = CutCornerShape(28.dp),
    extraExtraLarge = CutCornerShape(32.dp),
)

fun zxColorScheme(c: ZxColors): ColorScheme {
    fun mix(neon: Color, f: Float) = lerp(c.ground, neon, f)
    val low = lerp(c.ground, c.surface, 0.5f)
    val high = lerp(c.surface, c.surface2, 0.5f)
    return if (c.dark) {
        darkColorScheme(
            primary = c.neonA,
            onPrimary = c.onA,
            primaryContainer = mix(c.neonA, 0.22f),
            onPrimaryContainer = c.neonA,
            inversePrimary = c.neonB,
            secondary = c.neonB,
            onSecondary = c.onB,
            secondaryContainer = mix(c.neonB, 0.22f),
            onSecondaryContainer = c.neonB,
            tertiary = c.neonC,
            onTertiary = c.onC,
            tertiaryContainer = mix(c.neonC, 0.18f),
            onTertiaryContainer = c.neonC,
            background = c.ground,
            onBackground = c.ink,
            surface = c.ground,
            onSurface = c.ink,
            surfaceVariant = c.surface2,
            onSurfaceVariant = c.muted,
            surfaceTint = c.neonA,
            inverseSurface = c.ink,
            inverseOnSurface = c.ground,
            error = c.danger,
            onError = Color.Black,
            errorContainer = mix(c.danger, 0.25f),
            onErrorContainer = c.danger,
            outline = c.muted,
            outlineVariant = c.line,
            scrim = Color.Black,
            surfaceBright = c.surface2,
            surfaceDim = c.ground,
            surfaceContainerLowest = c.ground,
            surfaceContainerLow = low,
            surfaceContainer = c.surface,
            surfaceContainerHigh = high,
            surfaceContainerHighest = c.surface2,
        )
    } else {
        lightColorScheme(
            primary = c.neonA,
            onPrimary = c.onA,
            primaryContainer = lerp(c.surface, c.neonA, 0.16f),
            onPrimaryContainer = c.neonA,
            inversePrimary = c.neonB,
            secondary = c.neonB,
            onSecondary = c.onB,
            secondaryContainer = lerp(c.surface, c.neonB, 0.14f),
            onSecondaryContainer = c.neonB,
            tertiary = c.neonC,
            onTertiary = c.onC,
            tertiaryContainer = lerp(c.surface, c.neonC, 0.45f),
            onTertiaryContainer = c.onC,
            background = c.ground,
            onBackground = c.ink,
            surface = c.ground,
            onSurface = c.ink,
            surfaceVariant = c.surface2,
            onSurfaceVariant = c.muted,
            surfaceTint = c.neonA,
            inverseSurface = c.ink,
            inverseOnSurface = c.ground,
            error = c.danger,
            onError = Color.White,
            errorContainer = lerp(c.surface, c.danger, 0.16f),
            onErrorContainer = c.danger,
            outline = c.muted,
            outlineVariant = c.line,
            scrim = Color.Black,
            surfaceBright = c.surface,
            surfaceDim = c.surface2,
            surfaceContainerLowest = Color.White,
            surfaceContainerLow = low,
            surfaceContainer = c.surface,
            surfaceContainerHigh = high,
            surfaceContainerHighest = c.surface2,
        )
    }
}
