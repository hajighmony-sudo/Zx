package dev.cluvex.zedsecure.ui.zx

import androidx.compose.ui.graphics.Color
import dev.cluvex.zedsecure.ui.bento.BentoColors
import dev.cluvex.zedsecure.ui.bento.BentoTone

/**
 * Dedicated visual language for AppDesign.Zx — redesigned for a liquid-glass,
 * claymorphism and subtle holographic feel while remaining compatible with
 * the existing BentoColors contract.
 *
 * Goals:
 *  - Deep, soft atmospheric ground (not flat black)
 *  - Translucent surfaces that feel like frosted / liquid glass
 *  - Soft clay depth on cards and chips
 *  - Holographic-leaning accents (cyan ↔ violet)
 *  - Apple-HIG inspired clarity and calm hierarchy
 *
 * Scoped only to ZX so other themes stay untouched.
 */
fun zxFutureColors(dark: Boolean, amoled: Boolean = false): BentoColors {
    if (dark) {
        // Deep atmospheric ground with a tiny blue-violet bias
        val ground = if (amoled) Color(0xFF02040A) else Color(0xFF070B16)

        return BentoColors(
            dark = true,
            ground = ground,
            // Frosted glass surfaces
            surface = Color(0xFF111827).copy(alpha = 0.92f),
            surface2 = Color(0xFF1A2438),
            line = Color(0xFF3D4F73).copy(alpha = 0.65f),
            ink = Color(0xFFF4F6FF),
            muted = Color(0xFF9AA8C7),

            // Stage / primary elevated area – soft violet glass
            stage = Color(0xFF25204A),
            onStage = Color(0xFFF2EEFF),
            chipOnStage = Color(0x28FFFFFF),
            goodOnStage = Color(0xFF8EF5D4),

            // Accent lime → soft cyan-glass
            lime = Color(0xFF7DE8F0),
            onLime = Color(0xFF002A32),

            // Tonal families – more holographic range
            blue = BentoTone(Color(0xFF132F4D), Color(0xFFD4EBFF)),
            violet = BentoTone(Color(0xFF2A2158), Color(0xFFE8D9FF)),
            cyan = BentoTone(Color(0xFF0F3C47), Color(0xFFB8F2F8)),
            pink = BentoTone(Color(0xFF3F1F3A), Color(0xFFFFD4EF)),
            coral = BentoTone(Color(0xFF3F2530), Color(0xFFFFD6E4)),

            // Semantic
            good = Color(0xFF5CDCB4),
            mid = Color(0xFFFFC46B),
            bad = Color(0xFFFF8BA3),

            selected = Color(0xFF25204A),
            dock = Color(0xFF0E1528),
            dockLine = Color(0x55FFFFFF),
            danger = Color(0xFFFFB4C8),
            onDanger = Color(0xFF4F0E28),
            colorful = true,
        )
    }

    // Light mode – clean, soft clay + liquid glass
    return BentoColors(
        dark = false,
        ground = Color(0xFFF3F5FB),
        surface = Color(0xFFFCFDFF),
        surface2 = Color(0xFFE8EDF7),
        line = Color(0xFFC0CCE4),
        ink = Color(0xFF141A2E),
        muted = Color(0xFF5A6584),

        stage = Color(0xFFE4DEFF),
        onStage = Color(0xFF2C2168),
        chipOnStage = Color(0x1AFFFFFF),
        goodOnStage = Color(0xFF066B54),

        lime = Color(0xFFA8E4EC),
        onLime = Color(0xFF002F38),

        blue = BentoTone(Color(0xFFD8E9FF), Color(0xFF163E6E)),
        violet = BentoTone(Color(0xFFE6E0FF), Color(0xFF3F2F82)),
        cyan = BentoTone(Color(0xFFCFF0F4), Color(0xFF134E5C)),
        pink = BentoTone(Color(0xFFFFDCEF), Color(0xFF6E2F58)),
        coral = BentoTone(Color(0xFFFFDEE7), Color(0xFF72384C)),

        good = Color(0xFF077A64),
        mid = Color(0xFF7D5200),
        bad = Color(0xFFA81F48),

        selected = Color(0xFFDDE4FF),
        dock = Color(0xFFFCFDFF),
        dockLine = Color(0x22141A2E),
        danger = Color(0xFFFFD6E0),
        onDanger = Color(0xFF5C0C2E),
        colorful = true,
    )
}
