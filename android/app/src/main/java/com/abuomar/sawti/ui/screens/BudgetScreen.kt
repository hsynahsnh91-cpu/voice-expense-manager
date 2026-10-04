package com.abuomar.sawti.ui.screens

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abuomar.sawti.R
import com.abuomar.sawti.core.Currency
import com.abuomar.sawti.ui.components.SectionCard
import com.abuomar.sawti.ui.components.pressable
import com.abuomar.sawti.ui.components.rememberPressScale
import com.abuomar.sawti.ui.theme.Brand100
import com.abuomar.sawti.ui.theme.Brand500
import com.abuomar.sawti.ui.theme.Brand700
import com.abuomar.sawti.vm.BudgetViewModel

/* =============================================================================
 *  شاشة «ما ميزانيتك؟» — تظهر بعد إنشاء حساب جديد أو بعد تسجيل الدخول،
 *  وفيها: خانة رقم الميزانية + خانة عملة الميزانية + زر «الدخول للتطبيق».
 *
 *  العملة الافتراضية: الليرة السورية الجديدة (حُذف صفران من ١ كانون الثاني ٢٠٢٦).
 *  ويمكن اختيار الليرة القديمة فيُحوَّل المبلغ تلقائياً (÷١٠٠).
 * ========================================================================== */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BudgetScreen(
    viewModel: BudgetViewModel,
    onEnterApp: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    val isLegacy = state.currency == BudgetViewModel.CURRENCY_LEGACY
    val quick = if (isLegacy) viewModel.quickAmountsLegacy else viewModel.quickAmountsNew

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Brand700, Brand500, Color(0xFFDCEFE6))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            /* ------------------------- الشعار والعنوان ------------------------- */
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.20f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(74.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.budget_title),
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.budget_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.88f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(0.92f),
            )
            Spacer(Modifier.height(26.dp))

            /* ------------------------------ البطاقة ---------------------------- */
            SectionCard(padding = 20.dp) {

                Text(
                    text = stringResource(R.string.budget_amount_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.amountText,
                    onValueChange = viewModel::onAmountChange,
                    placeholder = { Text(stringResource(R.string.budget_amount_hint)) },
                    singleLine = true,
                    isError = state.errorRes != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { keyboard?.hide(); focus.clearFocus(); viewModel.save() }
                    ),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                    ),
                    trailingIcon = {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Brand100,
                            modifier = Modifier.padding(end = 4.dp),
                        ) {
                            Text(
                                text = stringResource(
                                    if (isLegacy) R.string.currency_symbol_legacy
                                    else R.string.currency_symbol
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = Brand700,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Brand500,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                )

                if (state.convertedText != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = state.convertedText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = Brand700,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(Modifier.height(18.dp))

                /* ------------------------- عملة الميزانية ------------------------ */
                Text(
                    text = stringResource(R.string.budget_currency_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                CurrencyDropdown(selectedLegacy = isLegacy, onSelect = viewModel::onCurrencyChange)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.hint_legacy_rate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(18.dp))

                /* --------------------------- مبالغ سريعة ------------------------- */
                Text(
                    text = stringResource(R.string.quick_amounts),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(9.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    quick.forEach { value ->
                        QuickChip(
                            label = Currency.format(
                                valueNew = if (isLegacy) Currency.legacyToNew(value) else value,
                                unit = if (isLegacy) Currency.Unit.LEGACY else Currency.Unit.NEW,
                                arabic = true,
                                withSymbol = false,
                            ),
                            selected = state.amountText == value.toString(),
                            onClick = { viewModel.pickQuick(value) },
                        )
                    }
                }

                if (state.errorRes != null) {
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(id = state.errorRes!!),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))

                /* ------------------------ زر الدخول للتطبيق --------------------- */
                Button(
                    onClick = { keyboard?.hide(); focus.clearFocus(); viewModel.save() },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand700),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                ) {
                    if (state.busy) {
                        CircularProgressIndicator(Modifier.size(19.dp), color = Color.White, strokeWidth = 2.4.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.saving))
                    } else {
                        Text(stringResource(R.string.enter_app), fontSize = 16.5.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Text("←", fontSize = 17.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.made_by),
                style = MaterialTheme.typography.bodySmall,
                color = Brand700,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
        }
    }

    LaunchedEffect(state.done) { if (state.done) onEnterApp() }
}

/* --------------------------- قائمة العملة المنسدلة ------------------------- */
@Composable
private fun CurrencyDropdown(selectedLegacy: Boolean, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction, 0.985f)

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = BorderStroke(1.4.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().scale(scale).pressable(interaction) { expanded = true },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(if (selectedLegacy) R.string.currency_legacy else R.string.currency_new),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.currency_new), fontWeight = FontWeight.SemiBold) },
                onClick = { onSelect(Currency.CODE_NEW); expanded = false },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.currency_legacy), fontWeight = FontWeight.SemiBold) },
                onClick = { onSelect(BudgetViewModel.CURRENCY_LEGACY); expanded = false },
            )
        }
    }
}

@Composable
private fun QuickChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction, 0.93f)
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) Brand700 else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.4.dp, if (selected) Brand700 else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.scale(scale).pressable(interaction, onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp),
        )
    }
}
