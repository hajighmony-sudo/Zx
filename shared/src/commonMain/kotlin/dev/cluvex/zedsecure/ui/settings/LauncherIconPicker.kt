package dev.cluvex.zedsecure.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import dev.cluvex.zedsecure.shared.resources.Res
import dev.cluvex.zedsecure.shared.resources.zx_original
import dev.cluvex.zedsecure.shared.resources.zx_blade
import dev.cluvex.zedsecure.shared.resources.zx_distressed
import dev.cluvex.zedsecure.shared.resources.zx_orange
import dev.cluvex.zedsecure.shared.resources.zx_silver
import dev.cluvex.zedsecure.shared.resources.zx_steel
import dev.cluvex.zedsecure.shared.resources.zx_white

private data class LauncherIconOption(
    val id: String,
    val title: String,
    val image: DrawableResource,
)

private val launcherIconOptions = listOf(
    LauncherIconOption("zx_original", "اصلی", Res.drawable.zx_original),
    LauncherIconOption("zx_orange", "نارنجی", Res.drawable.zx_orange),
    LauncherIconOption("zx_steel", "خراش‌خورده", Res.drawable.zx_steel),
    LauncherIconOption("zx_blade", "فرمی", Res.drawable.zx_blade),
    LauncherIconOption("zx_white", "سفید", Res.drawable.zx_white),
    LauncherIconOption("zx_silver", "فلزی", Res.drawable.zx_silver),
    LauncherIconOption("zx_distressed", "مخملی", Res.drawable.zx_distressed),
)

/** Displays the exact supplied artwork at full resolution instead of redrawing the ZX letters. */
@Composable
fun LauncherIconPicker(
    selected: String,
    onSelect: (String) -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("انتخاب آیکون", style = MaterialTheme.typography.titleMedium)
        Text(
            "پیش‌نمایش تصویر اصلی · با انتخاب هر طرح، آیکون صفحه اصلی تغییر می‌کند",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            launcherIconOptions.forEach { option ->
                val isSelected = selected == option.id
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onSelect(option.id) }
                        .padding(3.dp),
                ) {
                    Box(
                        Modifier
                            .size(112.dp)
                            .clip(RoundedCornerShape(25.dp))
                            .background(Color.Black)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.White.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(25.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(option.image),
                            contentDescription = option.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(108.dp)
                                .clip(RoundedCornerShape(23.dp)),
                        )
                        if (isSelected) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(7.dp)
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        option.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
