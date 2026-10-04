package com.abuomar.sawti.ui.theme

import androidx.compose.ui.graphics.Color

/* ============================ لوحة الألوان ================================
   أخضر سوري هادئ + لمسة ذهبية. نفس الرموز في نسختي الويب وأندرويد.        */

val Brand900 = Color(0xFF04352A)
val Brand800 = Color(0xFF064C3B)
val Brand700 = Color(0xFF075E4A)
val Brand600 = Color(0xFF0B7A5B)
val Brand500 = Color(0xFF0E9F6E)
val Brand400 = Color(0xFF2FBF8F)
val Brand300 = Color(0xFF6BD8B2)
val Brand100 = Color(0xFFD6F5E7)
val Brand050 = Color(0xFFF0FBF6)

val Accent = Color(0xFFF59E0B)
val AccentSoft = Color(0xFFFFE9B0)
val Danger = Color(0xFFDC2626)
val DangerSoft = Color(0xFFFDECEC)
val Info = Color(0xFF3B82F6)

/* ------------------------------ فاتح ------------------------------ */
val LightBackground = Color(0xFFF3F6F5)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE9EFEC)
val LightOnBackground = Color(0xFF0B1A15)
val LightOnSurface = Color(0xFF0B1A15)
val LightOnSurfaceVariant = Color(0xFF40564D)
val LightOutline = Color(0xFFDCE5E1)
val LightOutlineVariant = Color(0xFFC6D3CD)

/* ------------------------------ داكن ------------------------------ */
val DarkBackground = Color(0xFF08120E)
val DarkSurface = Color(0xFF101C17)
val DarkSurfaceVariant = Color(0xFF0A1512)
val DarkOnBackground = Color(0xFFEAF5EF)
val DarkOnSurface = Color(0xFFEAF5EF)
val DarkOnSurfaceVariant = Color(0xFFA9C0B6)
val DarkOutline = Color(0xFF1D2C26)
val DarkOutlineVariant = Color(0xFF2A3D35)
val DarkBrand100 = Color(0xFF123428)

/* --------------------------- ألوان الفئات -------------------------- */
val CategoryColors: Map<String, Color> = mapOf(
    "food" to Color(0xFFF97316),
    "transport" to Color(0xFF3B82F6),
    "bills" to Color(0xFF8B5CF6),
    "health" to Color(0xFFEF4444),
    "education" to Color(0xFF14B8A6),
    "clothing" to Color(0xFFEC4899),
    "home" to Color(0xFFF59E0B),
    "family" to Color(0xFF22C55E),
    "gifts" to Color(0xFFA855F7),
    "personal" to Color(0xFF0EA5E9),
    "business" to Color(0xFF64748B),
    "debt" to Color(0xFFDC2626),
    "other" to Color(0xFF9CA3AF),
)

fun categoryColor(id: String): Color = CategoryColors[id] ?: CategoryColors.getValue("other")
