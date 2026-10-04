package com.abuomar.sawti.ui.screens

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abuomar.sawti.R
import com.abuomar.sawti.core.CivilDate
import com.abuomar.sawti.core.Currency
import com.abuomar.sawti.data.AmountUnit
import com.abuomar.sawti.data.Category
import com.abuomar.sawti.data.TxType
import com.abuomar.sawti.ui.components.Hairline
import com.abuomar.sawti.ui.components.SectionCard
import com.abuomar.sawti.ui.components.ScreenHeader
import com.abuomar.sawti.ui.components.TagChip
import com.abuomar.sawti.ui.components.pressable
import com.abuomar.sawti.ui.components.rememberPressScale
import com.abuomar.sawti.ui.theme.Brand500
import com.abuomar.sawti.ui.theme.Brand700
import com.abuomar.sawti.ui.theme.categoryColor
import com.abuomar.sawti.vm.DataViewModel
import com.abuomar.sawti.vm.VoiceViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

/* =============================================================================
 *  شاشة التسجيل الصوتي.
 *
 *  هنا يُدمَج طلب إذن الميكروفون (Accompanist Permissions) مع الـViewModel
 *  الذي يدير التسجيل وتحويل الكلام إلى نص عبر SpeechRecognizer:
 *
 *    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
 *    LaunchedEffect(micPermission.status) {
 *        vm.onPermissionResult(micPermission.status.isGranted,
 *                              micPermission.status.shouldShowRationale)
 *    }
 *
 *  كل ما يُقال يُكتب مباشرة في خانة الإرسال (نتائج جزئية لحظية من المحرك)،
 *  ثم يفهمه المحلل العربي: المبلغ بالليرة السورية الجديدة، الفئة، التاريخ،
 *  والمكان. لا يوجد أي نص تجريبي أو محاكاة.
 * ========================================================================== */

@OptIn(ExperimentalPermissionsApi::class, ExperimentalLayoutApi::class)
@Composable
fun VoiceScreen(
    viewModel: VoiceViewModel,
    dataViewModel: DataViewModel,
    snackbarHostState: androidx.compose.material3.SnackbarHostState,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val prefs by dataViewModel.prefsState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current

    /* --------- إذن الميكروفون: الطلب مُعدّ مسبقاً عبر Accompanist --------- */
    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    LaunchedEffect(micPermission.status.isGranted, micPermission.status.shouldShowRationale) {
        viewModel.onPermissionResult(
            granted = micPermission.status.isGranted,
            shouldShowRationale = micPermission.status.shouldShowRationale,
        )
    }

    // إعادة فحص المحرك عند الرجوع للشاشة (قد يكون المستخدم ثبّت خدمة التعرّف)
    LaunchedEffect(Unit) { viewModel.refreshEngineStatus() }

    /* --------- التنبيهات والأخطاء --------- */
    LaunchedEffect(state.toastText, state.toastRes) {
        val msg = state.toastText ?: state.toastRes?.let { context.getString(it) }
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeToast()
        }
    }
    LaunchedEffect(state.errorRes) {
        state.errorRes?.let {
            snackbarHostState.showSnackbar(context.getString(it))
            viewModel.consumeError()
        }
    }

    val listening = state.phase == VoiceViewModel.Phase.LISTENING
    val processing = state.phase == VoiceViewModel.Phase.PROCESSING

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 96.dp)
        ) {
            ScreenHeader(
                title = stringResource(R.string.voice_title),
                subtitle = stringResource(R.string.voice_hint),
            )

            /* ------------------------- حالة المحرّك ------------------------- */
            EnginePill(
                available = state.engineAvailable,
                label = state.engineLabel,
                arabic = prefs.arabic,
            )

            Spacer(Modifier.height(16.dp))

            /* ------------------------- زر الميكروفون ------------------------ */
            MicButton(
                listening = listening,
                processing = processing,
                level = state.level,
                enabled = state.phase != VoiceViewModel.Phase.UNSUPPORTED,
                onClick = {
                    if (listening || processing) {
                        keyboard?.hide()
                        viewModel.stopListening()
                    } else if (!micPermission.status.isGranted) {
                        // طلب الإذن — نفس المسار المُعدّ مسبقاً
                        viewModel.markNeedsPermission()
                        micPermission.launchPermissionRequest()
                    } else {
                        keyboard?.hide()
                        viewModel.startListening()
                    }
                },
                onCancel = { viewModel.cancelListening() },
                showCancel = listening || processing,
            )

            /* --------------- شرح حالة الإذن (Rationale) --------------- */
            AnimatedVisibility(
                visible = !micPermission.status.isGranted &&
                    (micPermission.status.shouldShowRationale ||
                        state.phase == VoiceViewModel.Phase.PERMISSION_DENIED),
                enter = fadeIn(tween(220)) + slideInVertically(tween(280)) { -it / 6 },
                exit = fadeOut(tween(180)),
            ) {
                SectionCard(padding = 15.dp) {
                    Text(
                        text = stringResource(R.string.mic_rationale),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(11.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Button(
                            onClick = { micPermission.launchPermissionRequest() },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Brand700),
                            modifier = Modifier.height(44.dp),
                        ) { Text(stringResource(R.string.mic_grant_again)) }
                        TextButton(
                            onClick = { viewModel.clearPermissionUi() },
                            shape = RoundedCornerShape(12.dp),
                        ) { Text(stringResource(R.string.close)) }
                    }
                }
            }

            /* -------------- لا يوجد محرك تعرّف على الجهاز -------------- */
            AnimatedVisibility(
                visible = state.phase == VoiceViewModel.Phase.UNSUPPORTED,
                enter = fadeIn(tween(220)),
                exit = fadeOut(tween(180)),
            ) {
                SectionCard(padding = 15.dp) {
                    Text(
                        text = stringResource(R.string.voice_unsupported),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(9.dp))
                    Text(
                        text = stringResource(R.string.voice_manual_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            /* ------------------------ خانة الإرسال ------------------------ */
            Text(
                text = stringResource(R.string.voice_send_field),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = state.transcript,
                // كل ما يُقال يُكتب هنا فوراً (نتائج جزئية من المحرك)
                onValueChange = { viewModel.onManualEdit(it) },
                placeholder = { Text(stringResource(R.string.voice_send_hint)) },
                minLines = 3,
                maxLines = 6,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Brand500,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                trailingIcon = {
                    if (state.transcript.isNotEmpty()) {
                        IconButton(onClick = { viewModel.cancelListening() }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.clear),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
            )

            /* -------------------- الفهم التلقائي للكلام ------------------- */
            val parsed = state.parsed
            if (parsed != null) {
                Spacer(Modifier.height(14.dp))
                ParsedPreview(
                    amountText = Currency.format(parsed.amountNew ?: 0L, prefs.displayUnit, prefs.arabic),
                    amountIsLegacy = parsed.amountUnit == AmountUnit.LEGACY,
                    category = parsed.category,
                    type = parsed.type,
                    date = parsed.date,
                    merchant = parsed.merchant,
                    confidence = parsed.confidence,
                    needsConfirmation = parsed.needsConfirmation,
                    arabic = prefs.arabic,
                    onCategoryChange = viewModel::setCategory,
                    onDateChange = viewModel::setDate,
                    onMerchantChange = viewModel::setMerchant,
                    onTypeChange = viewModel::setType,
                    onToggleUnit = viewModel::toggleAmountUnit,
                )
            }

            /* -------- المبلغ غير مفهوم → إدخال يدوي (لا تخمين أبداً) -------- */
            if (state.manualAmountNeeded || (parsed != null && (parsed.amountNew ?: 0L) <= 0L)) {
                Spacer(Modifier.height(14.dp))
                ManualAmountField(onConfirm = viewModel::setManualAmount, unit = prefs.displayUnit)
            }

            Spacer(Modifier.height(20.dp))

            /* ------------------------- زر الإرسال ------------------------- */
            Button(
                onClick = { keyboard?.hide(); viewModel.send() },
                enabled = state.transcript.isNotBlank() && !state.saving,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Brand700),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
            ) {
                if (state.saving) {
                    CircularProgressIndicator(Modifier.size(19.dp), color = Color.White, strokeWidth = 2.4.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.saving))
                } else {
                    Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(9.dp))
                    Text(stringResource(R.string.voice_send), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            /* -------- تذكير بخيار الكتم (من الإعدادات) -------- */
            if (prefs.mutePlaybackAfterRecord) {
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("🔇", fontSize = 12.sp)
                    Text(
                        text = stringResource(R.string.mute_enabled_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp,
                    )
                }
            }

            if (state.audioSaved && state.audioDuration > 0) {
                Spacer(Modifier.height(8.dp))
                TagChip(
                    text = stringResource(R.string.voice_audio_saved, state.audioDuration),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/* ============================== حالة المحرّك =============================== */
@Composable
private fun EnginePill(available: Boolean, label: String?, arabic: Boolean) {
    val color = if (available) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Text(
                text = if (available) {
                    label?.let { stringResource(R.string.engine_ready_named, it) }
                        ?: stringResource(R.string.engine_ready)
                } else stringResource(R.string.engine_missing),
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/* =============================== زر الميكروفون ============================= */
@Composable
private fun MicButton(
    listening: Boolean,
    processing: Boolean,
    level: Float,
    enabled: Boolean,
    onClick: () -> Unit,
    onCancel: () -> Unit,
    showCancel: Boolean,
) {
    val interaction = remember { MutableInteractionSource() }
    val press = rememberPressScale(interaction, 0.93f)

    // نبض مستمر أثناء الاستماع — يتفاعل مع مستوى الصوت الحقيقي من الميكروفون
    val infinite = rememberInfiniteTransition(label = "micPulse")
    val breathe by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "breathe",
    )
    val haloAlpha by infinite.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.05f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "halo",
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(168.dp)) {
            // هالات تتوسع مع مستوى الصوت
            if (listening || processing) {
                Box(
                    Modifier
                        .size((128 + level * 58).dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Brand500.copy(alpha = haloAlpha),
                                    Brand500.copy(alpha = 0f),
                                )
                            )
                        )
                )
                Box(
                    Modifier
                        .size((104 + level * 42).dp)
                        .clip(CircleShape)
                        .background(Brand500.copy(alpha = 0.16f))
                )
            }

            val size = if (listening || processing) 104f else 96f
            Box(
                modifier = Modifier
                    .size((size * breathe).dp)
                    .scale(press)
                    .clip(CircleShape)
                    .background(
                        if (listening || processing)
                            Brush.linearGradient(listOf(Color(0xFFE05252), Color(0xFFB91C1C)))
                        else Brush.linearGradient(listOf(Brand500, Brand700))
                    )
                    .pressable(interaction, enabled = enabled, onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    processing -> CircularProgressIndicator(
                        modifier = Modifier.size(34.dp),
                        color = Color.White,
                        strokeWidth = 3.dp,
                    )
                    listening -> Icon(
                        Icons.Filled.Stop,
                        contentDescription = stringResource(R.string.voice_stop),
                        tint = Color.White,
                        modifier = Modifier.size(38.dp),
                    )
                    else -> Icon(
                        Icons.Filled.Mic,
                        contentDescription = stringResource(R.string.voice_record),
                        tint = Color.White,
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            text = when {
                processing -> stringResource(R.string.voice_processing)
                listening -> stringResource(R.string.voice_listening)
                else -> stringResource(R.string.voice_tap_to_start)
            },
            style = MaterialTheme.typography.titleSmall,
            color = if (listening) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        // شريط مستوى الصوت الحي
        AnimatedVisibility(visible = listening, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
            Spacer(Modifier.height(9.dp))
            Box(
                Modifier
                    .width(150.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(level.coerceIn(0.02f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.error)
                )
            }
        }

        if (showCancel) {
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onCancel, shape = RoundedCornerShape(50)) {
                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.voice_cancel))
            }
        }
    }
}


/* ============================ معاينة الفهم التلقائي ======================== */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParsedPreview(
    amountText: String,
    amountIsLegacy: Boolean,
    category: Category,
    type: TxType,
    date: String,
    merchant: String?,
    confidence: Float,
    needsConfirmation: Boolean,
    arabic: Boolean,
    onCategoryChange: (Category) -> Unit,
    onDateChange: (String) -> Unit,
    onMerchantChange: (String) -> Unit,
    onTypeChange: (TxType) -> Unit,
    onToggleUnit: () -> Unit,
) {
    val color = categoryColor(category.id)

    SectionCard(padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.voice_understood), style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.weight(1f))
            TagChip(
                text = stringResource(R.string.voice_confidence, (confidence * 100).toInt()),
                color = if (confidence >= 0.75f) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.tertiary,
            )
        }

        if (needsConfirmation) {
            Spacer(Modifier.height(9.dp))
            Surface(
                shape = RoundedCornerShape(11.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.voice_needs_confirm),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                )
            }
        }

        Spacer(Modifier.height(13.dp))

        /* المبلغ + تبديل الوحدة */
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.voice_amount), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(3.dp))
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = color,
                )
            }
            val interaction = remember { MutableInteractionSource() }
            val scale = rememberPressScale(interaction, 0.94f)
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.scale(scale).pressable(interaction, onClick = onToggleUnit),
            ) {
                Text(
                    text = stringResource(
                        if (amountIsLegacy) R.string.unit_toggle_to_new else R.string.unit_toggle_to_legacy
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        Hairline()
        Spacer(Modifier.height(13.dp))

        /* النوع */
        Text(stringResource(R.string.tx_type), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    onClick = { onTypeChange(t) },
                    label = { Text(stringResource(labelRes), fontSize = 12.sp) },
                    shape = RoundedCornerShape(50),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        /* الفئة */
        Text(stringResource(R.string.tx_category), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Category.ALL_SELECTABLE.forEach { cat ->
                val c = categoryColor(cat.id)
                val sel = category == cat
                val chipInteraction = remember { MutableInteractionSource() }
                val chipScale = rememberPressScale(chipInteraction, 0.92f)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (sel) c else c.copy(alpha = 0.10f),
                    border = BorderStroke(1.3.dp, if (sel) c else c.copy(alpha = 0.3f)),
                    modifier = Modifier.scale(chipScale).pressable(chipInteraction) { onCategoryChange(cat) },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(cat.emoji, fontSize = 13.sp)
                        Text(
                            text = stringResource(cat.nameRes),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (sel) Color.White else c,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        /* التاريخ */
        Text(stringResource(R.string.tx_date), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(7.dp))
        VoiceDateRow(date = date, arabic = arabic, onChange = onDateChange)

        Spacer(Modifier.height(12.dp))

        /* المكان / التاجر */
        OutlinedTextField(
            value = merchant ?: "",
            onValueChange = onMerchantChange,
            label = { Text(stringResource(R.string.tx_merchant)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            shape = RoundedCornerShape(13.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun VoiceDateRow(date: String, arabic: Boolean, onChange: (String) -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction, 0.97f)
    var expanded by remember { androidx.compose.runtime.mutableStateOf(false) }

    Box {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.scale(scale).pressable(interaction) { expanded = true },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Text("📅", fontSize = 14.sp)
                    Text(CivilDate.formatPretty(date, arabic), style = MaterialTheme.typography.bodyMedium)
                }
            }
            QuickDateChip(label = stringResource(R.string.preset_today), onClick = { onChange(CivilDate.today()) })
            QuickDateChip(
                label = stringResource(R.string.voice_yesterday),
                onClick = { onChange(CivilDate.plusDays(CivilDate.today(), -1)) },
            )
        }

        if (expanded) {
            androidx.compose.ui.window.Popup(
                alignment = Alignment.TopStart,
                onDismissRequest = { expanded = false },
                properties = androidx.compose.ui.window.PopupProperties(focusable = true),
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
                        com.abuomar.sawti.ui.components.RangeCalendarGrid(
                            range = com.abuomar.sawti.ui.components.CivilRange(date, date),
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

@Composable
private fun QuickDateChip(label: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction, 0.94f)
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.scale(scale).pressable(interaction, onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

/* ==================== إدخال المبلغ يدوياً عند عدم الفهم ==================== */
@Composable
private fun ManualAmountField(onConfirm: (String) -> Unit, unit: Currency.Unit) {
    var text by remember { androidx.compose.runtime.mutableStateOf("") }

    SectionCard(padding = 15.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("✍️", fontSize = 14.sp)
            Text(
                text = stringResource(R.string.voice_manual_amount),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(9.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = Currency.normalizeDigits(it).filter { c -> c.isDigit() }.take(15) },
                singleLine = true,
                placeholder = { Text(stringResource(R.string.tx_amount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                trailingIcon = {
                    Text(
                        text = stringResource(
                            if (unit == Currency.Unit.LEGACY) R.string.currency_symbol_legacy
                            else R.string.currency_symbol
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 12.dp),
                    )
                },
                shape = RoundedCornerShape(13.dp),
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = { if (text.isNotBlank()) onConfirm(text) },
                enabled = text.isNotBlank(),
                shape = RoundedCornerShape(13.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Brand700),
                modifier = Modifier.height(50.dp),
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

