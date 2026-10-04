package com.abuomar.sawti.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abuomar.sawti.R
import com.abuomar.sawti.SawtiApp
import com.abuomar.sawti.core.CivilDate
import com.abuomar.sawti.core.Currency
import com.abuomar.sawti.core.PrefsStore
import com.abuomar.sawti.ui.components.CREDIT_AR
import com.abuomar.sawti.ui.components.CREDIT_EN
import com.abuomar.sawti.ui.components.GroupTitle
import com.abuomar.sawti.ui.components.PRIVACY_POLICY_AR
import com.abuomar.sawti.ui.components.PRIVACY_POLICY_EN
import com.abuomar.sawti.ui.components.TERMS_AR
import com.abuomar.sawti.ui.components.TERMS_EN
import com.abuomar.sawti.ui.components.Hairline
import com.abuomar.sawti.ui.components.SectionCard
import com.abuomar.sawti.ui.components.ScreenHeader
import com.abuomar.sawti.ui.components.SettingsRow
import com.abuomar.sawti.ui.components.pressable
import com.abuomar.sawti.ui.components.rememberPressScale
import com.abuomar.sawti.ui.theme.Brand100
import com.abuomar.sawti.ui.theme.Brand500
import com.abuomar.sawti.ui.theme.Brand700
import com.abuomar.sawti.vm.DataViewModel
import com.abuomar.sawti.vm.VoiceViewModel
import kotlinx.coroutines.launch

/* =============================================================================
 *  شاشة الإعدادات: اللغة، السمة، وحدة العرض، الميزانية، الصوت (النطق،
 *  السرعة، الطبقة، المحرك)، خيار كتم إعادة الصوت بعد التسجيل، حفظ المقاطع،
 *  التصدير، مسح البيانات، الخروج، حذف الحساب.
 *
 *  وفي الأسفل: رقم الإصدار 1.0.0 + سياسة الخصوصية + شروط الاستخدام +
 *  بطاقة اعتماد «تم إنشاء التطبيق بواسطة أبو عمر».
 * ========================================================================== */

@Composable
fun SettingsScreen(
    viewModel: DataViewModel,
    voiceViewModel: VoiceViewModel,
    onSignedOut: () -> Unit,
) {
    val prefs by viewModel.prefsState.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val user by viewModel.currentUser.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val voiceState by voiceViewModel.state.collectAsStateWithLifecycle()

    var showPolicy by remember { mutableStateOf<PolicyType?>(null) }
    var showBudgetEditor by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }
    var confirmDeleteAccount by remember { mutableStateOf(false) }
    var showExportResult by remember { mutableStateOf<String?>(null) }

    /* --------------------- التصدير عبر مستندات النظام --------------------- */
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = viewModel.exportJson()
                val ok = runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                        out.flush()
                    } ?: false
                }.isSuccess
                showExportResult = context.getString(
                    if (ok) R.string.export_done else R.string.export_failed
                )
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(Modifier.padding(horizontal = 16.dp)) {
            ScreenHeader(
                title = stringResource(R.string.settings_title),
                subtitle = user?.let { stringResource(R.string.settings_signed_in_as, it.email) },
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            /* ============================ الحساب ============================ */
            item { GroupTitle(stringResource(R.string.settings_group_account)) }
            item {
                SectionCard(padding = 0.dp) {
                    SettingsRow(
                        icon = "👤",
                        title = user?.name ?: "—",
                        subtitle = user?.email ?: "",
                        showChevron = false,
                    )
                    Hairline()
                    SettingsRow(
                        icon = "💵",
                        title = stringResource(R.string.settings_budget),
                        subtitle = if (prefs.monthlyBudget > 0L) {
                            stringResource(
                                R.string.settings_budget_value,
                                viewModel.formatMoney(prefs.monthlyBudget),
                                CivilDate.formatMonthYear(CivilDate.today(), prefs.arabic),
                            )
                        } else stringResource(R.string.settings_budget_unset),
                        onClick = { showBudgetEditor = true },
                        showChevron = true,
                    )
                    Hairline()
                    SettingsRow(
                        icon = "🔢",
                        title = stringResource(R.string.settings_display_unit),
                        subtitle = stringResource(
                            if (prefs.displayUnit == Currency.Unit.LEGACY) R.string.unit_legacy_full
                            else R.string.unit_new_full
                        ),
                        onClick = {
                            viewModel.setDisplayUnit(
                                if (prefs.displayUnit == Currency.Unit.LEGACY) Currency.Unit.NEW
                                else Currency.Unit.LEGACY
                            )
                        },
                        showChevron = true,
                    )
                }
            }

            /* ============================= الصوت ============================= */
            item { GroupTitle(stringResource(R.string.settings_group_voice)) }
            item {
                SectionCard(padding = 0.dp) {
                    // الخيار المطلوب: كتم إعادة الصوت بعد التسجيل
                    SettingsRow(
                        icon = "🔇",
                        title = stringResource(R.string.settings_mute_replay),
                        subtitle = stringResource(R.string.settings_mute_replay_desc),
                        trailing = {
                            SawtiSwitch(
                                checked = prefs.mutePlaybackAfterRecord,
                                onCheckedChange = viewModel::setMutePlayback,
                            )
                        },
                    )
                    Hairline()
                    SettingsRow(
                        icon = "🗣️",
                        title = stringResource(R.string.settings_speak_responses),
                        subtitle = stringResource(R.string.settings_speak_responses_desc),
                        trailing = {
                            SawtiSwitch(
                                checked = prefs.speakResponses,
                                onCheckedChange = viewModel::setSpeakResponses,
                            )
                        },
                    )
                    Hairline()
                    SettingsRow(
                        icon = "🎙️",
                        title = stringResource(R.string.settings_save_audio),
                        subtitle = stringResource(R.string.settings_save_audio_desc),
                        trailing = {
                            SawtiSwitch(
                                checked = prefs.saveAudioClip,
                                onCheckedChange = viewModel::setSaveAudioClip,
                            )
                        },
                    )
                }
            }

            item {
                SectionCard(padding = 16.dp) {
                    /* سرعة الصوت */
                    SliderRow(
                        label = stringResource(R.string.settings_voice_rate),
                        value = prefs.voiceRate,
                        range = 0.6f..1.6f,
                        onValueChange = viewModel::setVoiceRate,
                        format = { "×%.2f".format(it) },
                    )
                    Spacer(Modifier.height(14.dp))
                    /* طبقة الصوت */
                    SliderRow(
                        label = stringResource(R.string.settings_voice_pitch),
                        value = prefs.voicePitch,
                        range = 0.6f..1.5f,
                        onValueChange = viewModel::setVoicePitch,
                        format = { "%.1f".format(it) },
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { viewModel.testVoice() },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(13.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Brand700),
                    ) { Text(stringResource(R.string.settings_test_voice)) }

                    Spacer(Modifier.height(13.dp))
                    Hairline()
                    Spacer(Modifier.height(12.dp))

                    /* حالة محرك التعرّف على الكلام — اسم المحرك الحقيقي */
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(
                                    if (voiceState.engineAvailable) Brand500
                                    else MaterialTheme.colorScheme.error
                                )
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_speech_engine),
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Text(
                                text = if (voiceState.engineAvailable) {
                                    voiceState.engineLabel
                                        ?.let { stringResource(R.string.engine_ready_named, it) }
                                        ?: stringResource(R.string.engine_ready)
                                } else stringResource(R.string.engine_missing),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    /* لغة التعرّف */
                    SpeechLanguageSelector(
                        current = prefs.speechLanguage,
                        onSelect = viewModel::setSpeechLanguage,
                        labelAuto = stringResource(R.string.settings_speech_lang_auto),
                    )

                    if (!prefs.concurrentCaptureSupported) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(11.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = stringResource(R.string.settings_concurrent_off),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                            )
                        }
                    }
                }
            }

            /* ============================ المظهر ============================= */
            item { GroupTitle(stringResource(R.string.settings_group_appearance)) }
            item {
                SectionCard(padding = 0.dp) {
                    SettingsRow(
                        icon = "🌐",
                        title = stringResource(R.string.settings_language),
                        subtitle = stringResource(R.string.settings_language_desc),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        SegmentedButton(
                            label = stringResource(R.string.lang_ar),
                            selected = prefs.language == "ar",
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setLanguage("ar") },
                        )
                        SegmentedButton(
                            label = stringResource(R.string.lang_en),
                            selected = prefs.language == "en",
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setLanguage("en") },
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Hairline()
                    SettingsRow(icon = "🎨", title = stringResource(R.string.settings_theme))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        SegmentedButton(
                            label = stringResource(R.string.theme_system),
                            selected = prefs.themeMode == PrefsStore.THEME_SYSTEM,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setTheme(PrefsStore.THEME_SYSTEM) },
                        )
                        SegmentedButton(
                            label = stringResource(R.string.theme_light),
                            selected = prefs.themeMode == PrefsStore.THEME_LIGHT,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setTheme(PrefsStore.THEME_LIGHT) },
                        )
                        SegmentedButton(
                            label = stringResource(R.string.theme_dark),
                            selected = prefs.themeMode == PrefsStore.THEME_DARK,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.setTheme(PrefsStore.THEME_DARK) },
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }

            /* ============================ البيانات =========================== */
            item { GroupTitle(stringResource(R.string.settings_group_data)) }
            item {
                SectionCard(padding = 0.dp) {
                    SettingsRow(
                        icon = "📦",
                        title = stringResource(R.string.settings_export),
                        subtitle = stringResource(R.string.settings_export_desc),
                        onClick = {
                            exportLauncher.launch("sawti-export-${CivilDate.today()}.json")
                        },
                        showChevron = true,
                    )
                    Hairline()
                    SettingsRow(
                        icon = "🗑️",
                        title = stringResource(R.string.settings_clear_transactions),
                        subtitle = stringResource(R.string.settings_clear_transactions_desc),
                        onClick = { confirmClear = true },
                        danger = true,
                    )
                    Hairline()
                    SettingsRow(
                        icon = "🔒",
                        title = stringResource(R.string.settings_sign_out),
                        subtitle = stringResource(R.string.settings_sign_out_desc),
                        onClick = { confirmSignOut = true },
                        danger = true,
                    )
                    Hairline()
                    SettingsRow(
                        icon = "⚠️",
                        title = stringResource(R.string.settings_delete_account),
                        subtitle = stringResource(R.string.settings_delete_account_desc),
                        onClick = { confirmDeleteAccount = true },
                        danger = true,
                    )
                }
            }

            /* ============================ القانوني =========================== */
            item { GroupTitle(stringResource(R.string.settings_group_legal)) }
            item {
                SectionCard(padding = 0.dp) {
                    SettingsRow(
                        icon = "🛡️",
                        title = stringResource(R.string.privacy_policy_title),
                        subtitle = stringResource(R.string.privacy_policy_subtitle),
                        onClick = { showPolicy = PolicyType.PRIVACY },
                        showChevron = true,
                    )
                    Hairline()
                    SettingsRow(
                        icon = "📜",
                        title = stringResource(R.string.terms_title),
                        subtitle = stringResource(R.string.terms_subtitle),
                        onClick = { showPolicy = PolicyType.TERMS },
                        showChevron = true,
                    )
                }
            }

            /* ========================= الإصدار والاعتماد ====================== */
            item {
                Spacer(Modifier.height(6.dp))
                VersionAndCredit(
                    version = "${stringResource(R.string.settings_version)} ${SawtiApp.VERSION_NAME}",
                    buildLabel = stringResource(R.string.settings_build, SawtiApp.VERSION_CODE),
                    credit = if (prefs.arabic) CREDIT_AR else CREDIT_EN,
                    offline = stringResource(R.string.settings_offline),
                    monthLabel = CivilDate.formatMonthYear(CivilDate.today(), prefs.arabic),
                )
            }
        }
    }

    /* ================================ الحوارات ============================== */
    showPolicy?.let { type ->
        PolicySheet(
            type = type,
            arabic = prefs.arabic,
            onDismiss = { showPolicy = null },
        )
    }

    if (showBudgetEditor) {
        BudgetEditorDialog(
            currentBudgetNew = prefs.monthlyBudget,
            arabic = prefs.arabic,
            onDismiss = { showBudgetEditor = false },
            onSave = { raw, legacy ->
                viewModel.setBudget(raw, if (legacy) BudgetViewModel.CURRENCY_LEGACY else Currency.CODE_NEW)
                showBudgetEditor = false
            },
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            shape = MaterialTheme.shapes.large,
            title = { Text(stringResource(R.string.confirm_clear_title)) },
            text = { Text(stringResource(R.string.confirm_clear_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearAllTransactions(); confirmClear = false }) {
                    Text(stringResource(R.string.delete_all), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            shape = MaterialTheme.shapes.large,
            title = { Text(stringResource(R.string.confirm_signout_title)) },
            text = { Text(stringResource(R.string.confirm_signout_body)) },
            confirmButton = {
                TextButton(onClick = { confirmSignOut = false; viewModel.signOut(); onSignedOut() }) {
                    Text(stringResource(R.string.settings_sign_out), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (confirmDeleteAccount) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAccount = false },
            shape = MaterialTheme.shapes.large,
            title = { Text(stringResource(R.string.confirm_delete_account_title)) },
            text = { Text(stringResource(R.string.confirm_delete_account_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteAccount = false
                    viewModel.deleteAccountAndAll()
                    onSignedOut()
                }) {
                    Text(stringResource(R.string.settings_delete_account), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteAccount = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

    showExportResult?.let { msg ->
        AlertDialog(
            onDismissRequest = { showExportResult = null },
            shape = MaterialTheme.shapes.large,
            title = { Text(stringResource(R.string.settings_export)) },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { showExportResult = null }) { Text(stringResource(R.string.close)) } },
        )
    }
}

private enum class PolicyType { PRIVACY, TERMS }

/* ======================== الإصدار وبطاقة الاعتماد ========================= */
@Composable
private fun VersionAndCredit(
    version: String,
    buildLabel: String,
    credit: String,
    offline: String,
    monthLabel: String,
) {
    // حركة طفو ناعمة لبطاقة الاعتماد
    val infinite = rememberInfiniteTransition(label = "creditFloat")
    val float by infinite.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse),
        label = "float",
    )
    val glow by animateFloatAsState(
        targetValue = 0.30f + float * 0.40f, tween(2600), label = "glow"
    )

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth()
                .scale(1f + float * 0.012f),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.linearGradient(listOf(Brand100.copy(alpha = 0.35f), Color.Transparent)))
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(Brand500, Brand700))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("ل", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = credit,
                    style = MaterialTheme.typography.titleSmall,
                    color = Brand700,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$version · $buildLabel",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "$offline · $monthLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    fontSize = 10.5.sp,
                )
                Spacer(Modifier.height(9.dp))
                Box(
                    Modifier
                        .width(72.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Brand500.copy(alpha = glow))
                )
            }
        }
    }
}

/* ============================ منزلق بإطار ================================= */
@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    format: (Float) -> String,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Surface(
                shape = RoundedCornerShape(50),
                color = Brand100,
            ) {
                Text(
                    text = format(value),
                    style = MaterialTheme.typography.labelMedium,
                    color = Brand700,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                )
            }
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/* ====================== زر مجزّأ (Segmented) بسيط ========================= */
@Composable
private fun SegmentedButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction, 0.95f)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) Brand700 else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.3.dp, if (selected) Brand700 else MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.scale(scale).pressable(interaction, onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
        )
    }
}

/* ================================ مفتاح ================================== */
@Composable
private fun SawtiSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Brand600Safe(),
            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
            uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant,
        ),
    )
}

@Composable
private fun Brand600Safe(): Color = MaterialTheme.colorScheme.primary

/* ==================== اختيار لغة محرّك التعرّف =========================== */
@Composable
private fun SpeechLanguageSelector(
    current: String?,
    onSelect: (String?) -> Unit,
    labelAuto: String,
) {
    val options = remember {
        listOf(
            null to labelAuto,
            "ar-SY" to "ar-SY",
            "ar-LB" to "ar-LB",
            "ar-JO" to "ar-JO",
            "ar" to "ar",
            "en-US" to "en-US",
        )
    }
    Column {
        Text(
            text = stringResource(R.string.settings_speech_lang),
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            options.forEach { (value, label) ->
                val interaction = remember { MutableInteractionSource() }
                val scale = rememberPressScale(interaction, 0.93f)
                val sel = current == value
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (sel) Brand700 else MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.2.dp, if (sel) Brand700 else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.scale(scale).pressable(interaction) { onSelect(value) },
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (sel) FontWeight.Bold else FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}


/* ========================= لوح سياسة الخصوصية ============================ */
@Composable
private fun PolicySheet(type: PolicyType, arabic: Boolean, onDismiss: () -> Unit) {
    val title = stringResource(
        if (type == PolicyType.PRIVACY) R.string.privacy_policy_title else R.string.terms_title
    )
    val body = when {
        type == PolicyType.PRIVACY && arabic -> PRIVACY_POLICY_AR
        type == PolicyType.PRIVACY -> PRIVACY_POLICY_EN
        arabic -> TERMS_AR
        else -> TERMS_EN
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.close))
                }
            }
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = body.trim(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close), fontWeight = FontWeight.Bold) }
        },
    )
}

/* ======================= محرّر الميزانية من الإعدادات ===================== */
@Composable
private fun BudgetEditorDialog(
    currentBudgetNew: Long,
    arabic: Boolean,
    onDismiss: () -> Unit,
    onSave: (Long, Boolean) -> Unit,
) {
    var legacy by remember { mutableStateOf(false) }
    var text by remember {
        mutableStateOf(
            if (currentBudgetNew > 0L) (if (legacy) Currency.newToLegacy(currentBudgetNew) else currentBudgetNew).toString()
            else ""
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.large,
        title = { Text(stringResource(R.string.settings_budget)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.budget_amount_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(7.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = text,
                    onValueChange = { text = Currency.normalizeDigits(it).filter { c -> c.isDigit() }.take(15) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                        imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                    ),
                    shape = RoundedCornerShape(13.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SegmentedButton(
                        label = stringResource(R.string.currency_new_short),
                        selected = !legacy,
                        modifier = Modifier.weight(1f),
                        onClick = { legacy = false },
                    )
                    SegmentedButton(
                        label = stringResource(R.string.currency_legacy_short),
                        selected = legacy,
                        modifier = Modifier.weight(1f),
                        onClick = { legacy = true },
                    )
                }
                Spacer(Modifier.height(9.dp))
                Text(
                    text = stringResource(R.string.hint_legacy_rate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val raw = Currency.parseAmount(text)
                    if (raw > 0L) onSave(raw, legacy) else onDismiss()
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Brand700),
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
