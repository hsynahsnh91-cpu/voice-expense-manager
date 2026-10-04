package com.abuomar.sawti.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abuomar.sawti.R
import com.abuomar.sawti.core.CivilDate
import com.abuomar.sawti.data.Category
import com.abuomar.sawti.data.Transaction
import com.abuomar.sawti.data.TxType
import com.abuomar.sawti.ui.components.CivilRange
import com.abuomar.sawti.ui.components.EmptyState
import com.abuomar.sawti.ui.components.Hairline
import com.abuomar.sawti.ui.components.RangeCalendarGrid
import com.abuomar.sawti.ui.components.SectionCard
import com.abuomar.sawti.ui.components.ScreenHeader
import com.abuomar.sawti.ui.components.TagChip
import com.abuomar.sawti.ui.components.pressable
import com.abuomar.sawti.ui.components.rememberPressScale
import com.abuomar.sawti.ui.theme.categoryColor
import com.abuomar.sawti.vm.DataViewModel
import kotlinx.coroutines.delay
import kotlin.math.max

/* =============================================================================
 *  شاشة المصاريف: ملخص الميزانية + شريط التقدم + فلاتر الفترة والفئة والبحث
 *  + تفصيل الفئات + قائمة المصاريف مصنّفة حسب اليوم.
 *  كل رقم يُعرض حسب وحدة العرض المختارة (ليرة سورية جديدة / قديمة).
 * ========================================================================== */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionsScreen(
    viewModel: DataViewModel,
    snackbarHostState: androidx.compose.material3.SnackbarHostState,
) {
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val grouped by viewModel.grouped.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val prefs by viewModel.prefsState.collectAsStateWithLifecycle()

    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Transaction?>(null) }
    var confirmDelete by remember { mutableStateOf<Transaction?>(null) }
    var search by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }

    LaunchedEffect(search) {
        delay(160)
        viewModel.setSearch(search)
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {

            /* ---------------------------- الترويسة --------------------------- */
            Column(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
            ) {
                ScreenHeader(
                    title = stringResource(R.string.transactions_title),
                    subtitle = CivilDate.formatMonthYear(CivilDate.today(), prefs.arabic),
                    trailing = {
                        val interaction = remember { MutableInteractionSource() }
                        val scale = rememberPressScale(interaction, 0.9f)
                        Box(
                            Modifier
                                .size(38.dp)
                                .scale(scale)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .pressable(interaction) { showFilters = !showFilters },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("🔎", fontSize = 16.sp)
                        }
                    },
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp, end = 16.dp, bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                /* ------------------------- بطاقات الملخص ------------------------ */
                item { SummaryCards(summary, viewModel) }

                /* ------------------------- شريط الميزانية ----------------------- */
                item { BudgetProgressBar(summary, viewModel, prefs.arabic) }

                /* --------------------------- الفلاتر ---------------------------- */
                item {
                    AnimatedVisibility(
                        visible = showFilters || filters.active || filters.category != "all",
                        enter = fadeIn(tween(200)) + slideInVertically(tween(260)) { -it / 8 },
                        exit = fadeOut(tween(160)),
                    ) {
                        FiltersBlock(
                            filters = filters,
                            prefs = prefs,
                            viewModel = viewModel,
                            search = search,
                            onSearchChange = { search = it },
                        )
                    }
                }

                /* ------------------------- تفصيل الفئات ------------------------- */
                if (summary.budget > 0L || grouped.isNotEmpty()) {
                    item { CategoryBreakdown(summary.byCategory, viewModel) }
                }

                /* --------------------------- القائمة ---------------------------- */
                if (grouped.isEmpty()) {
                    item {
                        EmptyState(
                            icon = "🧾",
                            title = stringResource(R.string.transactions_empty_title),
                            hint = stringResource(R.string.transactions_empty_hint),
                        )
                    }
                } else {
                    grouped.forEach { (day, list) ->
                        item(key = "hdr-$day") { DayHeader(day, list, viewModel, prefs.arabic) }
                        items(list, key = { it.id }) { tx ->
                            TransactionRow(
                                tx = tx,
                                arabic = prefs.arabic,
                                money = { viewModel.formatMoney(it) },
                                onEdit = { editing = tx; editorOpen = true },
                                onDelete = { confirmDelete = tx },
                            )
                        }
                    }
                }
            }
        }

        /* ------------------------- زر الإضافة العائم -------------------------- */
        val fabInteraction = remember { MutableInteractionSource() }
        val fabScale = rememberPressScale(fabInteraction, 0.88f)
        FloatingActionButton(
            onClick = { editing = null; editorOpen = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 18.dp, bottom = 18.dp)
                .scale(fabScale),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(18.dp),
            interactionSource = fabInteraction,
        ) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_transaction))
        }

        /* --------------------- محرّر المصروف (لوح منزلق) --------------------- */
        AnimatedVisibility(
            visible = editorOpen,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(160)),
        ) {
            Scrim { editorOpen = false }
        }
        AnimatedVisibility(
            visible = editorOpen,
            enter = slideInVertically(tween(320)) { it } + fadeIn(tween(240)),
            exit = slideOutVertically(tween(260)) { it } + fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            TransactionEditorSheet(
                existing = editing,
                defaultDisplayUnit = prefs.displayUnit,
                arabic = prefs.arabic,
                onDismiss = { editorOpen = false },
                onSave = { amountNew, cat, type, date, merchant, note ->
                    viewModel.saveTransaction(editing?.id, amountNew, cat, type, date, merchant, note)
                    editorOpen = false
                    editing = null
                },
            )
        }

        /* ----------------------- تأكيد الحذف ------------------------------- */
        confirmDelete?.let { tx ->
            ConfirmDeleteDialog(
                tx = tx,
                money = viewModel.formatMoney(tx.amountNew),
                onDismiss = { confirmDelete = null },
                onConfirm = {
                    viewModel.deleteTransaction(tx.id)
                    confirmDelete = null
                },
            )
        }
    }
}

/* ============================== بطاقات الملخص ============================== */
@Composable
private fun SummaryCards(summary: com.abuomar.sawti.data.MonthSummary, viewModel: DataViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.tx_sum_spent),
            value = viewModel.formatMoney(summary.spent),
            accent = MaterialTheme.colorScheme.error,
            icon = "📤",
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.tx_sum_income),
            value = viewModel.formatMoney(summary.income),
            accent = MaterialTheme.colorScheme.primary,
            icon = "📥",
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label = stringResource(R.string.tx_sum_remaining),
            value = if (summary.budget > 0L) viewModel.formatMoney(max(0L, summary.remaining)) else "—",
            accent = if (summary.overBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
            icon = if (summary.overBudget) "⚠️" else "💰",
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    accent: Color,
    icon: String,
) {
    SectionCard(modifier = modifier, padding = 13.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(icon, fontSize = 13.sp)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = accent,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 15.sp,
            maxLines = 1,
        )
    }
}

/* ============================ شريط الميزانية ============================== */
@Composable
private fun BudgetProgressBar(
    summary: com.abuomar.sawti.data.MonthSummary,
    viewModel: DataViewModel,
    arabic: Boolean,
) {
    if (summary.budget <= 0L) {
        SectionCard(padding = 15.dp) {
            Text(
                text = stringResource(R.string.hint_set_budget),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    // percent عدد صحيح من 0 إلى 999 (نسبة مئوية) وليس كسراً
    val pct = summary.percent.toFloat().coerceIn(0f, 999f)
    val progress by animateFloatAsState(
        targetValue = (summary.percent / 100f).coerceIn(0f, 1f),
        animationSpec = tween(700),
        label = "budgetProgress",
    )
    val over = summary.overBudget

    SectionCard(padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.budget_amount_label), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(
                    R.string.budget_usage_format,
                    viewModel.formatMoney(summary.spent),
                    viewModel.formatMoney(summary.budget),
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(11.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
            )
        }
        Spacer(Modifier.height(9.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "%.0f%%".format(pct),
                style = MaterialTheme.typography.labelMedium,
                color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (over) stringResource(R.string.budget_over)
                       else stringResource(R.string.budget_left_format, viewModel.formatMoney(max(0L, summary.remaining))),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/* ================================ الفلاتر ================================= */
@Composable
private fun FiltersBlock(
    filters: DataViewModel.Filters,
    prefs: DataViewModel.Prefs,
    viewModel: DataViewModel,
    search: String,
    onSearchChange: (String) -> Unit,
) {
    SectionCard(padding = 14.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(
                DataViewModel.Preset.TODAY to R.string.preset_today,
                DataViewModel.Preset.THIS_WEEK to R.string.preset_this_week,
                DataViewModel.Preset.THIS_MONTH to R.string.preset_this_month,
                DataViewModel.Preset.LAST_MONTH to R.string.preset_last_month,
                DataViewModel.Preset.ALL_TIME to R.string.preset_all_time,
            ).forEach { (preset, labelRes) ->
                FilterChip(
                    selected = false,
                    onClick = { viewModel.applyPreset(preset) },
                    label = { Text(stringResource(labelRes), fontSize = 12.sp) },
                    shape = RoundedCornerShape(50),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // حقل الفترة — يفتح Popover فيه التقويم (نمط shadcn)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            DateRangeTrigger(
                range = CivilRange(filters.from.takeIf { filters.active }, filters.to.takeIf { filters.active }),
                arabic = prefs.arabic,
                label = stringResource(R.string.filter_range),
                onApply = { r -> viewModel.setRange(r.from, r.to) },
            )
            CategoryFilterChip(
                selected = filters.category,
                onSelect = viewModel::setCategory,
            )
        }

        Spacer(Modifier.height(11.dp))

        OutlinedTextField(
            value = search,
            onValueChange = onSearchChange,
            placeholder = { Text(stringResource(R.string.tx_search_hint)) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    Icons.Filled.Search, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp),
                )
            },
            trailingIcon = {
                if (search.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(
                            Icons.Filled.Close,
                            contentDescription = stringResource(R.string.clear),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = RoundedCornerShape(14.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )
    }
}

/* --------------------- حقل الفترة + Popover التقويم ---------------------- */
@Composable
private fun DateRangeTrigger(
    range: CivilRange,
    arabic: Boolean,
    label: String,
    onApply: (CivilRange) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction, 0.96f)

    Box {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (range.isEmpty) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.primaryContainer,
            border = BorderStroke(
                1.3.dp,
                if (range.isEmpty) MaterialTheme.colorScheme.outlineVariant
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
            ),
            modifier = Modifier.scale(scale).pressable(interaction) { expanded = true },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text("📅", fontSize = 14.sp)
                Text(
                    text = if (range.from == null) label
                           else if (range.to == null || range.to == range.from)
                               CivilDate.formatPretty(range.from!!, arabic)
                           else "${CivilDate.formatPretty(range.from!!, arabic)} ← ${CivilDate.formatPretty(range.to!!, arabic)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (range.isEmpty) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                )
            }
        }

        if (expanded) {
            Popup(
                alignment = Alignment.TopStart,
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 14.dp,
                    modifier = Modifier.width(330.dp),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(label, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        RangeCalendarGrid(
                            range = range,
                            picking = range.from != null && range.to == null,
                            hover = null,
                            arabic = arabic,
                            onHover = {},
                            initialViewMonth = range.to ?: range.from ?: CivilDate.today(),
                            onPick = { day ->
                                // نقرة أولى = «من»، نقرة ثانية = «إلى»
                                if (range.from == null) onApply(CivilRange(day, day))
                                else if (range.to == null) onApply(
                                    CivilRange(minOf(range.from!!, day), maxOf(range.from!!, day))
                                ) else onApply(CivilRange(day, day))
                                expanded = false
                            },
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { onApply(CivilRange(null, null)); expanded = false }) {
                                Text(stringResource(R.string.filter_clear))
                            }
                        }
                    }
                }
            }
        }
    }
}

/* ----------------------------- فلتر الفئة -------------------------------- */
@Composable
private fun CategoryFilterChip(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction, 0.96f)
    val current = if (selected == "all") null else Category.of(selected)

    Box {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (current == null) MaterialTheme.colorScheme.surfaceVariant
                    else categoryColor(current.id).copy(alpha = 0.16f),
            border = BorderStroke(
                1.3.dp,
                if (current == null) MaterialTheme.colorScheme.outlineVariant
                else categoryColor(current.id).copy(alpha = 0.5f),
            ),
            modifier = Modifier.scale(scale).pressable(interaction) { expanded = true },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(current?.emoji ?: "🗂️", fontSize = 14.sp)
                Text(
                    text = if (current == null) stringResource(R.string.category_all)
                           else stringResource(current.nameRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (current == null) MaterialTheme.colorScheme.onSurfaceVariant
                            else categoryColor(current.id),
                    maxLines = 1,
                )
            }
        }

        DropdownMenuCompat(expanded = expanded, onDismiss = { expanded = false }) {
            Category.ALL_SELECTABLE.forEach { cat ->
                DropdownMenuItemCompat(
                    label = stringResource(cat.nameRes),
                    emoji = cat.emoji,
                    color = categoryColor(cat.id),
                    onClick = { onSelect(cat.id); expanded = false },
                )
            }
            DropdownMenuItemCompat(
                label = stringResource(R.string.category_all),
                emoji = "🗂️",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = { onSelect("all"); expanded = false },
            )
        }
    }
}

@Composable
private fun DropdownMenuCompat(
    expanded: Boolean,
    onDismiss: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    androidx.compose.material3.DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(MaterialTheme.colorScheme.surface),
    ) { content() }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.DropdownMenuItemCompat(
    label: String,
    emoji: String,
    color: Color,
    onClick: () -> Unit,
) {
    androidx.compose.material3.DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(emoji, fontSize = 15.sp)
                Text(label, style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.SemiBold)
            }
        },
        onClick = onClick,
    )
}

/* =========================== تفصيل حسب الفئة ============================== */
@Composable
private fun CategoryBreakdown(byCategory: Map<String, Long>, viewModel: DataViewModel) {
    if (byCategory.isEmpty()) return
    val total = byCategory.values.sum().coerceAtLeast(1L)
    val sorted = byCategory.entries.sortedByDescending { it.value }

    SectionCard(padding = 16.dp) {
        Text(stringResource(R.string.by_category), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(13.dp))
        sorted.forEach { (catId, amount) ->
            val color = categoryColor(catId)
            val frac = amount.toFloat() / total
            val width by animateFloatAsState(frac.coerceIn(0.02f, 1f), tween(600), label = "bar-$catId")
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(9.dp).clip(CircleShape).background(color)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(Category.of(catId).nameRes),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(78.dp),
                    maxLines = 1,
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(9.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(width)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(color)
                    )
                }
                Spacer(Modifier.width(9.dp))
                Text(
                    text = viewModel.formatMoney(amount),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(74.dp),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                )
            }
        }
    }
}

/* ============================ رأس اليوم والمجموع ========================== */
@Composable
private fun DayHeader(
    day: String,
    list: List<Transaction>,
    viewModel: DataViewModel,
    arabic: Boolean,
) {
    val today = remember { CivilDate.today() }
    val yesterday = remember { CivilDate.plusDays(today, -1) }
    val label = when (day) {
        today -> stringResource(R.string.tx_group_today)
        yesterday -> stringResource(R.string.tx_group_yesterday)
        else -> CivilDate.formatPretty(day, arabic)
    }
    val spent = list.filter { it.type == TxType.EXPENSE.id }.sumOf { it.amountNew }
    val income = list.filter { it.type == TxType.INCOME.id }.sumOf { it.amountNew }

    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
        Spacer(Modifier.width(8.dp))
        if (spent > 0) Text(
            "−${viewModel.formatMoney(spent)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
        )
        if (income > 0) {
            Spacer(Modifier.width(6.dp))
            Text(
                "+${viewModel.formatMoney(income)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/* ================================ صف المصروف ============================== */
@Composable
private fun TransactionRow(
    tx: Transaction,
    arabic: Boolean,
    money: (Long) -> String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val cat = Category.of(tx.category)
    val color = categoryColor(cat.id)
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction, 0.985f)
    val type = TxType.of(tx.type)

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().scale(scale).pressable(interaction, onClick = onEdit),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) { Text(cat.emoji, fontSize = 19.sp) }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(cat.nameRes),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                    )
                    if (tx.hasAudio) Text("🎙", fontSize = 11.sp)
                    if (!tx.listened && tx.hasAudio) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
                    }
                }
                val sub = listOfNotNull(
                    tx.merchant?.takeIf { it.isNotBlank() },
                    CivilDate.formatPretty(tx.date, arabic),
                ).joinToString(" · ")
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = when (type) {
                    TxType.INCOME -> "+${money(tx.amountNew)}"
                    TxType.DEBT -> "−${money(tx.amountNew)}"
                    TxType.EXPENSE -> "−${money(tx.amountNew)}"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = when (type) {
                    TxType.INCOME -> MaterialTheme.colorScheme.primary
                    TxType.DEBT -> MaterialTheme.colorScheme.tertiary
                    TxType.EXPENSE -> MaterialTheme.colorScheme.error
                },
                maxLines = 1,
            )

            IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(17.dp),
                )
            }
        }
    }
}

/* ======================= محرّر المصروف (لوح منزلق) ======================== */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransactionEditorSheet(
    existing: Transaction?,
    defaultDisplayUnit: com.abuomar.sawti.core.Currency.Unit,
    arabic: Boolean,
    onDismiss: () -> Unit,
    onSave: (Long, Category, TxType, String, String?, String) -> Unit,
) {
    val isLegacy = defaultDisplayUnit == com.abuomar.sawti.core.Currency.Unit.LEGACY
    var amountText by remember(existing?.id) {
        mutableStateOf(
            if (existing != null) {
                val v = if (isLegacy) com.abuomar.sawti.core.Currency.newToLegacy(existing.amountNew) else existing.amountNew
                v.toString()
            } else ""
        )
    }
    var category by remember(existing?.id) { mutableStateOf(existing?.let { Category.of(it.category) } ?: Category.FOOD) }
    var type by remember(existing?.id) { mutableStateOf(existing?.let { TxType.of(it.type) } ?: TxType.EXPENSE) }
    var date by remember(existing?.id) { mutableStateOf(existing?.date ?: CivilDate.today()) }
    var merchant by remember(existing?.id) { mutableStateOf(existing?.merchant ?: "") }
    var note by remember(existing?.id) { mutableStateOf(existing?.note) }
    val focus = LocalFocusManager.current

    Surface(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 20.dp,
        modifier = Modifier.fillMaxWidth().imePadding(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 12.dp)
                .navigationBarsPadding()
                .padding(bottom = 18.dp)
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 44.dp, height = 4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )
            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(if (existing == null) R.string.tx_new_title else R.string.tx_edit_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close))
                }
            }

            Spacer(Modifier.height(6.dp))

            // المبلغ
            Text(stringResource(R.string.tx_amount), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(7.dp))
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = com.abuomar.sawti.core.Currency.normalizeDigits(it).filter { c -> c.isDigit() }.take(15) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                trailingIcon = {
                    Text(
                        text = stringResource(if (isLegacy) R.string.currency_symbol_legacy else R.string.currency_symbol),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                },
                textStyle = MaterialTheme.typography.titleLarge,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(14.dp))

            // النوع
            Text(stringResource(R.string.tx_type), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(TxType.EXPENSE, TxType.INCOME, TxType.DEBT).forEach { t ->
                    val labelRes = when (t) {
                        TxType.EXPENSE -> R.string.type_expense
                        TxType.INCOME -> R.string.type_income
                        TxType.DEBT -> R.string.type_debt
                    }
                    FilterChip(
                        selected = type == t,
                        onClick = { type = t },
                        label = { Text(stringResource(labelRes), fontSize = 12.5.sp) },
                        shape = RoundedCornerShape(50),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // الفئة
            Text(stringResource(R.string.tx_category), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.ALL_SELECTABLE.forEach { cat ->
                    val color = categoryColor(cat.id)
                    val sel = category == cat
                    val interaction = remember { MutableInteractionSource() }
                    val scale = rememberPressScale(interaction, 0.93f)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (sel) color else color.copy(alpha = 0.10f),
                        border = BorderStroke(1.4.dp, if (sel) color else color.copy(alpha = 0.30f)),
                        modifier = Modifier.scale(scale).pressable(interaction) { category = cat },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(cat.emoji, fontSize = 14.sp)
                            Text(
                                text = stringResource(cat.nameRes),
                                style = MaterialTheme.typography.labelMedium,
                                color = if (sel) Color.White else color,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // التاريخ
            Text(stringResource(R.string.tx_date), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(7.dp))
            SingleDateField(date = date, arabic = arabic, onChange = { date = it })

            Spacer(Modifier.height(14.dp))

            // التاجر / المكان
            OutlinedTextField(
                value = merchant,
                onValueChange = { merchant = it },
                label = { Text(stringResource(R.string.tx_merchant)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(11.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(stringResource(R.string.tx_note)) },
                minLines = 2,
                maxLines = 3,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            if (existing?.transcript != null) {
                Spacer(Modifier.height(9.dp))
                TagChip(text = existing.transcript!!, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (existing != null) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) { Text(stringResource(R.string.cancel)) }
                }
                Button(
                    onClick = {
                        focus.clearFocus()
                        val raw = com.abuomar.sawti.core.Currency.parseAmount(amountText)
                        if (raw <= 0L) return@Button
                        val amountNew = if (isLegacy) com.abuomar.sawti.core.Currency.legacyToNew(raw) else raw
                        onSave(amountNew, category, type, date, merchant.takeIf { it.isNotBlank() }, note)
                    },
                    enabled = com.abuomar.sawti.core.Currency.parseAmount(amountText) > 0L,
                    modifier = Modifier.weight(if (existing != null) 1.4f else 1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    Text(
                        stringResource(if (existing == null) R.string.tx_save else R.string.tx_update),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/* ------------------------- حقل تاريخ واحد (Popover) ----------------------- */
@Composable
private fun SingleDateField(date: String, arabic: Boolean, onChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction, 0.97f)

    Box {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.3.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().scale(scale).pressable(interaction) { expanded = true },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("📅", fontSize = 15.sp)
                Text(CivilDate.formatPretty(date, arabic), style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (expanded) {
            Popup(
                alignment = Alignment.TopStart,
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 14.dp,
                    modifier = Modifier.width(330.dp),
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text(stringResource(R.string.tx_date), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        // وضع اليوم الواحد: from = to = اليوم المحدد
                        RangeCalendarGrid(
                            range = CivilRange(date, date),
                            picking = false,
                            hover = null,
                            arabic = arabic,
                            onHover = {},
                            initialViewMonth = date,
                            onPick = { day -> onChange(day); expanded = false },
                        )
                    }
                }
            }
        }
    }
}

/* ============================== خلفية معتمة =============================== */
@Composable
private fun Scrim(onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.42f))
            .pressable(remember { MutableInteractionSource() }, onClick = onClick)
    )
}

/* ============================ تأكيد الحذف ================================= */
@Composable
private fun ConfirmDeleteDialog(
    tx: Transaction,
    money: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text(stringResource(R.string.confirm_delete_title), style = MaterialTheme.typography.titleLarge) },
        text = {
            Text(
                stringResource(R.string.confirm_delete_body, money, CivilDate.formatPretty(tx.date, true)),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
