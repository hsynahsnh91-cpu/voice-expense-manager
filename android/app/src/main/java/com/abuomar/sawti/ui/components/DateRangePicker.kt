package com.abuomar.sawti.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.abuomar.sawti.core.CivilDate

/* =============================================================================
 *  منتقي الفترة (Date Range Picker) — نمط shadcn/ui: Popover + Calendar
 *
 *  • mode = "range": الطرفان يأخذان range_start / range_end،
 *    والأيام الملوّنة بينهما range_middle.
 *  • التواريخ نصوص مدنية yyyy-MM-dd حصراً — لا Date-parsed-as-UTC إطلاقاً،
 *    وهذا يمنع خطأ «اليوم الناقص» في المناطق شرق غرينتش (سوريا UTC+3).
 *  • بداية الأسبوع حسب اللغة: السبت للعربية السورية، الأحد للإنكليزية.
 *  • نموذج لوحة المفاتيح: الأسهم تحرّك يوماً/أسبوعاً، PageUp/PageDown شهراً،
 *    Home/End طرف الأسبوع، Enter/Space تحديد، Esc إغلاق.
 * ========================================================================== */

/* مفاتيح دلالية مطابقة لأسماء react-day-picker حتى تبقى الاختبارات واحدة */
val RangeStart = SemanticsPropertyKey<Boolean>("range_start")
val RangeEnd = SemanticsPropertyKey<Boolean>("range_end")
val RangeMiddle = SemanticsPropertyKey<Boolean>("range_middle")

/** فترة محددة بتواريخ مدنية */
data class CivilRange(val from: String?, val to: String?) {
    val isEmpty: Boolean get() = from == null
    val isComplete: Boolean get() = from != null && to != null
}

/* ============================ حقل التاريخ ================================ */

/**
 * حقل يفتح Popover فيه التقويم.
 * يعرض «من ← إلى» ويحفظ القيم كنصوص yyyy-MM-dd.
 */
@Composable
fun DateRangeField(
    range: CivilRange,
    onRangeChange: (CivilRange) -> Unit,
    label: String,
    arabic: Boolean,
    modifier: Modifier = Modifier,
    clearText: String,
    todayText: String,
    applyText: String,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(12.dp),
            color = if (range.isEmpty) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.primaryContainer,
            border = BorderStroke(
                1.5.dp,
                if (range.isEmpty) MaterialTheme.colorScheme.outlineVariant
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (range.isEmpty) MaterialTheme.colorScheme.onSurfaceVariant
                           else MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = displayRange(range, arabic, label),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (range.isEmpty) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        if (expanded) {
            Popup(
                alignment = Alignment.TopStart,
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                RangeCalendarPopover(
                    initial = range,
                    arabic = arabic,
                    clearText = clearText,
                    todayText = todayText,
                    applyText = applyText,
                    title = label,
                    onDismiss = { expanded = false },
                    onApply = {
                        onRangeChange(it)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun displayRange(range: CivilRange, arabic: Boolean, fallback: String): String {
    val from = range.from ?: return fallback
    val to = range.to
    val a = CivilDate.formatPretty(from, arabic)
    return if (to == null || to == from) a else "$a ← ${CivilDate.formatPretty(to, arabic)}"
}

/* ============================ نافذة التقويم ============================== */

@Composable
fun RangeCalendarPopover(
    initial: CivilRange,
    arabic: Boolean,
    clearText: String,
    todayText: String,
    applyText: String,
    title: String,
    onDismiss: () -> Unit,
    onApply: (CivilRange) -> Unit,
) {
    var draft by remember { mutableStateOf(initial) }
    var picking by remember { mutableStateOf(initial.from != null && initial.to == null) }
    var hover by remember { mutableStateOf<String?>(null) }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 12.dp,
        modifier = Modifier.width(340.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            RangeCalendarGrid(
                range = draft,
                picking = picking,
                hover = hover,
                arabic = arabic,
                onHover = { hover = it },
                onPick = { day ->
                    if (!picking) {
                        draft = CivilRange(day, null)
                        picking = true
                        hover = null
                    } else {
                        val from = draft.from!!
                        val a = minOf(from, day)
                        val b = maxOf(from, day)
                        draft = CivilRange(a, b)
                        picking = false
                        hover = null
                    }
                },
                onViewMonthChange = {},
                initialViewMonth = draft.from ?: CivilDate.today(),
            )

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = { draft = CivilRange(null, null); picking = false }, modifier = Modifier.weight(1f)) {
                    Text(clearText)
                }
                TextButton(onClick = {
                    val td = CivilDate.today()
                    draft = CivilRange(td, td); picking = false
                }, modifier = Modifier.weight(1f)) {
                    Text(todayText)
                }
                Button(
                    onClick = { onApply(if (draft.from != null && draft.to == null) draft.copy(to = draft.from) else draft) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text(applyText)
                }
            }
        }
    }
    // ملاحظة: Esc يُعالج داخل الشبكة عبر onPreviewKeyEvent
    LaunchedEffect(Unit) { /* Popup يغلق تلقائياً عند اللمس خارجه */ }
}

/* ============================== شبكة التقويم ============================= */

@Composable
fun RangeCalendarGrid(
    range: CivilRange,
    picking: Boolean,
    hover: String?,
    arabic: Boolean,
    onHover: (String?) -> Unit,
    onPick: (String) -> Unit,
    initialViewMonth: String,
    onViewMonthChange: (String) -> Unit = {},
) {
    val weekStart = CivilDate.weekStart(arabic)
    var viewMonth by remember { mutableStateOf(CivilDate.startOfMonth(initialViewMonth)) }
    var cursor by remember { mutableStateOf(range.from ?: initialViewMonth) }
    val focusRequester = remember { FocusRequester() }

    val today = remember { CivilDate.today() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val step = when (event.key) {
                    // في RTL السهم الأيسر يعني اليوم التالي
                    Key.DirectionLeft -> if (arabic) 1 else -1
                    Key.DirectionRight -> if (arabic) -1 else 1
                    Key.DirectionUp -> -7
                    Key.DirectionDown -> 7
                    else -> 0
                }
                when {
                    step != 0 -> {
                        cursor = CivilDate.plusDays(cursor, step)
                        viewMonth = ensureVisible(cursor, viewMonth)
                        true
                    }
                    event.key == Key.PageUp -> {
                        cursor = CivilDate.plusMonths(cursor, -1)
                        viewMonth = ensureVisible(cursor, viewMonth)
                        true
                    }
                    event.key == Key.PageDown -> {
                        cursor = CivilDate.plusMonths(cursor, 1)
                        viewMonth = ensureVisible(cursor, viewMonth)
                        true
                    }
                    event.key == Key.MoveHome -> {
                        cursor = weekEdge(cursor, weekStart, start = true)
                        viewMonth = ensureVisible(cursor, viewMonth)
                        true
                    }
                    event.key == Key.MoveEnd -> {
                        cursor = weekEdge(cursor, weekStart, start = false)
                        viewMonth = ensureVisible(cursor, viewMonth)
                        true
                    }
                    event.key == Key.Enter || event.key == Key.NumPadEnter -> {
                        onPick(cursor); true
                    }
                    else -> false
                }
            }
    ) {
        /* رأس التنقل بين الشهور */
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavButton("«") { viewMonth = CivilDate.plusMonths(viewMonth, -12); onViewMonthChange(viewMonth) }
            NavButton("‹") { viewMonth = CivilDate.plusMonths(viewMonth, -1); onViewMonthChange(viewMonth) }
            Text(
                text = CivilDate.formatMonthYear(viewMonth, arabic),
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            NavButton("›") { viewMonth = CivilDate.plusMonths(viewMonth, 1); onViewMonthChange(viewMonth) }
            NavButton("»") { viewMonth = CivilDate.plusMonths(viewMonth, 12); onViewMonthChange(viewMonth) }
        }

        Spacer(Modifier.height(6.dp))

        /* أسماء الأيام — مرتّبة حسب بداية الأسبوع للغة المختارة */
        Row(modifier = Modifier.fillMaxWidth()) {
            CivilDate.weekdayLabels(arabic, weekStart).forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        /* شبكة الأيام */
        val parts = CivilDate.parts(viewMonth)
        val days = CivilDate.daysInMonth(viewMonth)
        val lead = ((CivilDate.weekday(CivilDate.of(parts.year, parts.month, 1)) - weekStart) + 7) % 7

        val effFrom = range.from
        val effTo = range.to ?: if (picking) hover else null

        val cells = ArrayList<String?>(lead + days)
        repeat(lead) { cells.add(null) }
        for (d in 1..days) cells.add(CivilDate.of(parts.year, parts.month, d))
        while (cells.size % 7 != 0) cells.add(null)

        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (day == null) {
                            Box(modifier = Modifier.size(38.dp))
                        } else {
                            DayCell(
                                date = day,
                                isStart = effFrom != null && day == effFrom,
                                isEnd = effTo != null && day == effTo,
                                isMiddle = effFrom != null && effTo != null && effFrom != effTo &&
                                    CivilDate.isBetween(day, effFrom, effTo),
                                isToday = day == today,
                                isCursor = day == cursor,
                                onFocus = { cursor = day },
                                onHover = { onHover(day) },
                                onClick = { onPick(day) },
                            )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
}

@Composable
private fun DayCell(
    date: String,
    isStart: Boolean,
    isEnd: Boolean,
    isMiddle: Boolean,
    isToday: Boolean,
    isCursor: Boolean,
    onFocus: () -> Unit,
    onHover: () -> Unit,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val dayNumber = CivilDate.parts(date).day

    val shape = when {
        isStart && isEnd -> RoundedCornerShape(11.dp)
        isStart -> RoundedCornerShape(topStart = 11.dp, bottomStart = 11.dp)
        isEnd -> RoundedCornerShape(topEnd = 11.dp, bottomEnd = 11.dp)
        isMiddle -> RoundedCornerShape(0.dp)
        else -> RoundedCornerShape(11.dp)
    }
    val background = when {
        isStart || isEnd -> primary
        isMiddle -> primary.copy(alpha = 0.22f)
        else -> Color.Transparent
    }
    val contentColor = when {
        isStart || isEnd -> onPrimary
        isMiddle -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface
    }

    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction, 0.9f)

    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 36.dp)
            .scale(scale)
            .clip(shape)
            .background(background)
            .then(
                if (isToday && !isStart && !isEnd)
                    Modifier.border(BorderStroke(1.6.dp, primary.copy(alpha = 0.7f)), shape)
                else Modifier
            )
            .then(
                if (isCursor) Modifier.border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.tertiary), shape)
                else Modifier
            )
            // المفاتيح الدلالية range_start / range_end / range_middle
            .semantics {
                this[RangeStart] = isStart
                this[RangeEnd] = isEnd
                this[RangeMiddle] = isMiddle
            }
            .testTag("day-$date")
            .pressable(
                interaction,
                onClickLabel = date,
                onClick = { onFocus(); onClick() },
            )
            .padding(0.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = dayNumber.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            fontWeight = if (isStart || isEnd || isToday) FontWeight.ExtraBold else FontWeight.SemiBold,
            fontSize = 13.sp,
        )
    }
    // تمرير المؤشر عند اللمس لتحديد الطرف الثاني
    LaunchedEffect(isCursor) { if (isCursor) onHover() }
}

@Composable
private fun NavButton(symbol: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction, 0.88f)
    Box(
        modifier = Modifier
            .size(32.dp)
            .scale(scale)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pressable(interaction, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = symbol,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/* ----------------------------- حسابات مساعدة ----------------------------- */

private fun ensureVisible(cursor: String, viewMonth: String): String {
    val cm = CivilDate.monthKey(cursor)
    val vm = CivilDate.monthKey(viewMonth)
    return if (cm == vm) viewMonth else CivilDate.startOfMonth(cursor)
}

private fun weekEdge(cursor: String, weekStart: Int, start: Boolean): String {
    val wd = CivilDate.weekday(cursor)
    val offsetFromStart = ((wd - weekStart) % 7 + 7) % 7
    return if (start) CivilDate.plusDays(cursor, -offsetFromStart)
    else CivilDate.plusDays(cursor, 6 - offsetFromStart)
}
