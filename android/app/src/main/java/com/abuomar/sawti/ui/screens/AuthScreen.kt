package com.abuomar.sawti.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.abuomar.sawti.R
import com.abuomar.sawti.ui.components.rememberPressScale
import com.abuomar.sawti.ui.theme.Brand100
import com.abuomar.sawti.ui.theme.Brand500
import com.abuomar.sawti.ui.theme.Brand700
import com.abuomar.sawti.ui.theme.Brand900

/* =============================================================================
 *  شاشة المصادقة — تسجيل دخول حقيقي بالبريد وكلمة المرور حصراً.
 *
 *  المكافئ الأصلي لدلالات <form> في الويب:
 *   • حقل البريد: KeyboardType.Email + autoCorrect=false → Autofill "username"
 *     (نوع لوحة المفاتيح هو ما يجعل مدير كلمات المرور يتعرف على الحقل ويملأه)
 *   • حقل كلمة المرور: KeyboardType.Password → Autofill "current-password"،
 *     وفي إنشاء الحساب كلمة مرور جديدة → "new-password"
 *   • الإرسال بـ Enter: آخر حقل يحمل ImeAction.Done + KeyboardActions(onDone=)
 *     فمفتاح الإدخال/Enter في لوحة المفاتيح يُرسل النموذج فعلاً
 *   • زر إظهار كلمة المرور: IconButton مستقل (ليس part من الحقل) واسمه
 *     الممكن الوصول له "إظهار/إخفاء كلمة المرور" (مكافئ aria-label)
 *   • أزرار Google/Apple فوق فاصل طريقة الدخول: خط رفيع وفي وسطه «أو»
 *   • لا حسابات تجريبية: لا يمكن الدخول بأي بريد وكلمة مرور — التحقق يتم
 *     مقابل بصمة PBKDF2 لحساب أُنشئ فعلاً على هذا الجهاز.
 * ========================================================================== */

private enum class Mode { SIGN_IN, SIGN_UP }

@Composable
fun AuthScreen(
    busy: Boolean,
    errorText: String?,
    onSignIn: (email: String, password: String) -> Unit,
    onSignUp: (name: String, email: String, password: String, confirm: String) -> Unit,
    onFederatedClick: () -> Unit,
) {
    var mode by rememberSaveable { mutableStateOf(Mode.SIGN_IN) }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Brand900, Brand700, Brand500)))
    ) {
        // هالات ضبابية خلفية — منظر ناعم بلا أي صور
        Box(
            Modifier
                .align(Alignment.TopStart)
                .size(280.dp)
                .blur(70.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.10f))
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(220.dp)
                .blur(60.dp)
                .clip(CircleShape)
                .background(Brand100.copy(alpha = 0.18f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandHeader()
            Spacer(Modifier.height(22.dp))

            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AnimatedContent(
                    targetState = mode,
                    transitionSpec = {
                        val shift = if (rtl) -1 else 1
                        val toSignUp = targetState == Mode.SIGN_UP
                        (slideInHorizontally(tween(320)) { w -> (if (toSignUp) shift else -shift) * w / 6 }
                            + fadeIn(tween(320))) togetherWith
                            (slideOutHorizontally(tween(280)) { w -> (if (toSignUp) -shift else shift) * w / 6 }
                                + fadeOut(tween(280)))
                    },
                    label = "authMode",
                    modifier = Modifier.padding(22.dp),
                ) { current ->
                    when (current) {
                        Mode.SIGN_IN -> SignInForm(busy, errorText, onSignIn, onFederatedClick) { mode = Mode.SIGN_UP }
                        Mode.SIGN_UP -> SignUpForm(busy, errorText, onSignUp, onFederatedClick) { mode = Mode.SIGN_IN }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stringResource(R.string.secure_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center,
                    fontSize = 11.5.sp,
                )
            }
        }
    }
}

/* -------------------------------- الشعار --------------------------------- */
@Composable
private fun BrandHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // الأيقونة الجديدة: قرش + ميكروفون، واضحة ولا تُساء قراءتها من بعيد
        Box(
            modifier = Modifier
                .size(66.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(Brand500, Brand900))),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = stringResource(R.string.app_name),
                tint = Color.Unspecified,
                modifier = Modifier.size(66.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall, color = Color.White)
        Text(
            stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.82f),
        )
    }
}

/* ============================ نموذج تسجيل الدخول ========================== */

@Composable
private fun SignInForm(
    busy: Boolean,
    errorText: String?,
    onSignIn: (String, String) -> Unit,
    onFederatedClick: () -> Unit,
    onSwitch: () -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    val submit = {
        keyboard?.hide()
        focus.clearFocus()
        onSignIn(email, password)
    }

    Column {
        Text(stringResource(R.string.welcome_back), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.welcome_back_sub),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))

        FederatedButtons(onFederatedClick)
        OrDivider()

        OutlinedTextField(
            value = email,
            onValueChange = { email = it.trim() },
            label = { Text(stringResource(R.string.email_label)) },
            placeholder = { Text("name@example.com") },
            singleLine = true,
            enabled = !busy,
            isError = errorText != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                autoCorrectEnabled = false,
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors(),
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password_label)) },
            singleLine = true,
            enabled = !busy,
            isError = errorText != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,            // Enter = إرسال
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            visualTransformation = if (passwordVisible) VisualTransformation.None
            else PasswordVisualTransformation(),
            trailingIcon = {
                PasswordVisibilityToggle(visible = passwordVisible, onToggle = { passwordVisible = !passwordVisible })
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors(),
        )

        if (errorText != null) {
            Spacer(Modifier.height(12.dp))
            ErrorBanner(errorText)
        }

        Spacer(Modifier.height(18.dp))
        Button(
            onClick = submit,
            enabled = !busy && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Brand700),
        ) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(19.dp), color = Color.White, strokeWidth = 2.4.dp)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.signing_in))
            } else {
                Text(stringResource(R.string.sign_in), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(
                stringResource(R.string.no_account),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onSwitch) {
                Text(stringResource(R.string.create_one), fontWeight = FontWeight.Bold)
            }
        }
    }
}

/* ============================ نموذج إنشاء حساب ============================ */

@Composable
private fun SignUpForm(
    busy: Boolean,
    errorText: String?,
    onSignUp: (String, String, String, String) -> Unit,
    onFederatedClick: () -> Unit,
    onSwitch: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    val submit = {
        keyboard?.hide()
        focus.clearFocus()
        onSignUp(name, email, password, confirm)
    }

    Column {
        Text(stringResource(R.string.create_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.create_sub),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))

        FederatedButtons(onFederatedClick)
        OrDivider()

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.name_label)) },
            singleLine = true,
            enabled = !busy,
            isError = errorText != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors(),
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it.trim() },
            label = { Text(stringResource(R.string.email_label)) },
            placeholder = { Text("name@example.com") },
            singleLine = true,
            enabled = !busy,
            isError = errorText != null,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email, imeAction = ImeAction.Next, autoCorrectEnabled = false,
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors(),
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.password_label)) },
            supportingText = { Text(stringResource(R.string.err_password_short), fontSize = 11.5.sp) },
            singleLine = true,
            enabled = !busy,
            isError = errorText != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                PasswordVisibilityToggle(visible = passwordVisible, onToggle = { passwordVisible = !passwordVisible })
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors(),
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = confirm,
            onValueChange = { confirm = it },
            label = { Text(stringResource(R.string.confirm_password_label)) },
            singleLine = true,
            enabled = !busy,
            isError = errorText != null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = fieldColors(),
        )

        if (errorText != null) {
            Spacer(Modifier.height(12.dp))
            ErrorBanner(errorText)
        }

        Spacer(Modifier.height(18.dp))
        Button(
            onClick = submit,
            enabled = !busy && name.isNotBlank() && email.isNotBlank() &&
                password.isNotBlank() && confirm.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Brand700),
        ) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(19.dp), color = Color.White, strokeWidth = 2.4.dp)
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.creating_account))
            } else {
                Text(stringResource(R.string.sign_up), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(
                stringResource(R.string.have_account),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onSwitch) {
                Text(stringResource(R.string.sign_in_link), fontWeight = FontWeight.Bold)
            }
        }
        Text(
            stringResource(R.string.terms_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            fontSize = 11.5.sp,
        )
    }
}

/* ===================== زر إظهار/إخفاء كلمة المرور ========================= */
@Composable
private fun PasswordVisibilityToggle(visible: Boolean, onToggle: () -> Unit) {
    val label = stringResource(if (visible) R.string.hide_password else R.string.show_password)
    IconButton(onClick = onToggle) {
        Icon(
            imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
            contentDescription = label,                 // الاسم الممكن الوصول له
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/* ====================== أزرار فيدرالية + فاصل «أو» ======================== */

@Composable
private fun FederatedButtons(onClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FederatedButton(stringResource(R.string.continue_with_google), "G", onClick)
        FederatedButton(stringResource(R.string.continue_with_apple), "", onClick)
    }
}

@Composable
private fun FederatedButton(label: String, glyph: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction, 0.98f)
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp).scale(scale),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        interactionSource = interaction,
    ) {
        Text(glyph, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        Spacer(Modifier.width(9.dp))
        Text(label, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun OrDivider() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val gradient = Brush.horizontalGradient(
            listOf(Color.Transparent, MaterialTheme.colorScheme.outlineVariant, Color.Transparent)
        )
        Box(Modifier.weight(1f).height(1.dp).background(gradient))
        Text(
            text = stringResource(R.string.sign_in_divider_or).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.6.sp,
        )
        Box(Modifier.weight(1f).height(1.dp).background(gradient))
    }
}

@Composable
private fun ErrorBanner(text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Brand500,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    errorBorderColor = MaterialTheme.colorScheme.error,
)
