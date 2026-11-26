package dev.cluvex.zedsecure.ui.zx

import androidx.compose.ui.graphics.Color
import dev.cluvex.zedsecure.ui.bento.BentoColors
import dev.cluvex.zedsecure.ui.bento.BentoTone

/**
 * Dedicated visual language for AppDesign.Zx.
 *
 * The rest of the app keeps its own palettes; this palette is intentionally
 * scoped to ZX so the redesign does not leak into other appearance modes.
 */
fun zxFutureColors(dark: Boolean, amoled: Boolean = false): BentoColors {
    if (dark) {
        val ground = if (amoled) Color.Black else Color(0xFF070A16)
        return BentoColors(
            dark = true,
            ground = ground,
            surface = Color(0xFF10172A),
            surface2 = Color(0xFF1A2440),
            line = Color(0xFF3C4A70),
            ink = Color(0xFFF2F4FF),
            muted = Color(0xFFAAB5D2),
            stage = Color(0xFF2B205F),
            onStage = Color(0xFFF1ECFF),
            chipOnStage = Color(0x24FFFFFF),
            goodOnStage = Color(0xFF9CF3D6),
            lime = Color(0xFF9DE8F2),
            onLime = Color(0xFF002B36),
            blue = BentoTone(Color(0xFF142D50), Color(0xFFD0E7FF)),
            violet = BentoTone(Color(0xFF2B2058), Color(0xFFE0D0FF)),
            cyan = BentoTone(Color(0xFF123D49), Color(0xFFB8F0FA)),
            pink = BentoTone(Color(0xFF48233F), Color(0xFFFFD0EE)),
            coral = BentoTone(Color(0xFF482B36), Color(0xFFFFD8E5)),
            good = Color(0xFF60DDB7),
            mid = Color(0xFFFFC878),
            bad = Color(0xFFFF8DA8),
            selected = Color(0xFF292451),
            dock = Color(0xFF121A30),
            dockLine = Color(0x45FFFFFF),
            danger = Color(0xFFFFB4C6),
            onDanger = Color(0xFF5C102C),
            colorful = true,
        )
    }

    return BentoColors(
        dark = false,
        ground = Color(0xFFF2F4FB),
        surface = Color(0xFFFCFDFF),
        surface2 = Color(0xFFE8ECF7),
        line = Color(0xFFC9D2E7),
        ink = Color(0xFF151A31),
        muted = Color(0xFF5B6685),
        stage = Color(0xFFE6E0FF),
        onStage = Color(0xFF30226B),
        chipOnStage = Color(0x1AFFFFFF),
        goodOnStage = Color(0xFF075D48),
        lime = Color(0xFFBCECF0),
        onLime = Color(0xFF00313A),
        blue = BentoTone(Color(0xFFDCEAFF), Color(0xFF174273)),
        violet = BentoTone(Color(0xFFE8E0FF), Color(0xFF44318C)),
        cyan = BentoTone(Color(0xFFD4F2F5), Color(0xFF155665)),
        pink = BentoTone(Color(0xFFFFE0F1), Color(0xFF76365F)),
        coral = BentoTone(Color(0xFFFFE1E9), Color(0xFF7A3D52)),
        good = Color(0xFF087C66),
        mid = Color(0xFF865700),
        bad = Color(0xFFB32655),
        selected = Color(0xFFE0E7FF),
        dock = Color(0xFFFDFEFF),
        dockLine = Color(0x26151A31),
        danger = Color(0xFFFFD9E2),
        onDanger = Color(0xFF650D33),
        colorful = true,
    )
}
