package com.abuomar.sawti.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abuomar.sawti.R
import com.abuomar.sawti.ui.components.rememberPressScale
import com.abuomar.sawti.vm.DataViewModel
import com.abuomar.sawti.vm.VoiceViewModel

/* =============================================================================
 *  الشاشة الرئيسية: شريط تنقل سفلي (Material 3 NavigationBar) بأربعة وجهات
 *  متساوية الأهمية — أيقونة فوق تسمية قصيرة.
 *
 *  • الوجهة المحددة ملوّنة (tinted) والبقية باهتة (muted)
 *  • شارة عدّ على «الوارد» (BadgedBox) = عدد التسجيلات التي لم تُسمع بعد
 *  • تبديل الشاشة بدون push animation: تلاشٍ + انزلاق خفيف (AnimatedContent)
 *  • الشريط يحترم شريط التنقل النظامي (navigationBars insets) فلا تختفي
 *    الأيقونات تحت إيماءة النظام
 * ========================================================================== */

enum class Tab(val route: String) {
    TRANSACTIONS("transactions"),
    VOICE("voice"),
    INBOX("inbox"),
    SETTINGS("settings"),
}

@Composable
fun HomeScreen(
    dataViewModel: DataViewModel,
    voiceViewModel: VoiceViewModel,
    startTab: Tab = Tab.TRANSACTIONS,
    onRequestSignOut: () -> Unit,
) {
    var currentTab by rememberSaveable { mutableStateOf(startTab) }
    val snackbarHostState = remember { SnackbarHostState() }
    val unheard by dataViewModel.unheardCount.collectAsStateWithLifecycle()
    val prefs by dataViewModel.prefsState.collectAsStateWithLifecycle()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomNavBar(
                current = currentTab,
                unheardCount = unheard,
                onSelect = { tab -> if (tab != currentTab) currentTab = tab },
            )
        },
        // الشريط السفلي يتكفّل بحشوة الإيماءات بنفسه (navigationBarsPadding)
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ScreenSwitcher(
                tab = currentTab,
                dataViewModel = dataViewModel,
                voiceViewModel = voiceViewModel,
                snackbarHostState = snackbarHostState,
                onRequestSignOut = onRequestSignOut,
            )
        }
    }
}

/* --------------------- انتقال بين التبويبات: تلاشٍ + انزلاق ---------------- */
@Composable
private fun ScreenSwitcher(
    tab: Tab,
    dataViewModel: DataViewModel,
    voiceViewModel: VoiceViewModel,
    snackbarHostState: SnackbarHostState,
    onRequestSignOut: () -> Unit,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val dir = if (rtl) -1 else 1

    AnimatedContent(
        targetState = tab,
        transitionSpec = {
            val forward = targetState.ordinal > initialState.ordinal
            val offset = if (forward) dir else -dir
            (slideInHorizontally(tween(300)) { w -> offset * w / 10 } + fadeIn(tween(320))) togetherWith
                (slideOutHorizontally(tween(260)) { w -> -offset * w / 10 } + fadeOut(tween(240)))
        },
        label = "tabSwitch",
        modifier = Modifier.fillMaxSize(),
    ) { target ->
        when (target) {
            Tab.TRANSACTIONS -> TransactionsScreen(
                viewModel = dataViewModel,
                snackbarHostState = snackbarHostState,
            )
            Tab.VOICE -> VoiceScreen(
                viewModel = voiceViewModel,
                dataViewModel = dataViewModel,
                snackbarHostState = snackbarHostState,
            )
            Tab.INBOX -> InboxScreen(
                viewModel = dataViewModel,
                snackbarHostState = snackbarHostState,
            )
            Tab.SETTINGS -> SettingsScreen(
                viewModel = dataViewModel,
                voiceViewModel = voiceViewModel,
                onSignedOut = onRequestSignOut,
            )
        }
    }
}

/* ========================== شريط التنقل السفلي ============================ */

private data class TabSpec(
    val tab: Tab,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeDescRes: Int?,
)

@Composable
private fun BottomNavBar(
    current: Tab,
    unheardCount: Int,
    onSelect: (Tab) -> Unit,
) {
    val specs = remember {
        listOf(
            TabSpec(Tab.TRANSACTIONS, R.string.nav_transactions,
                Icons.Filled.Receipt, Icons.Outlined.ReceiptLong, null),
            TabSpec(Tab.VOICE, R.string.nav_voice,
                Icons.Filled.Mic, Icons.Outlined.Mic, null),
            TabSpec(Tab.INBOX, R.string.nav_inbox,
                Icons.Filled.Inbox, Icons.Outlined.Inbox, R.string.inbox_badge_desc),
            TabSpec(Tab.SETTINGS, R.string.nav_settings,
                Icons.Filled.Settings, Icons.Outlined.Settings, null),
        )
    }

    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            // حشوة شريط الإيماءات: الأيقونات لا تختفي تحت شريط النظام
            .navigationBarsPadding()
            .semantics { contentDescription = "main-navigation" },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        specs.forEach { spec ->
            val selected = current == spec.tab
            NavTabItem(
                spec = spec,
                selected = selected,
                badgeCount = if (spec.tab == Tab.INBOX) unheardCount else 0,
                onClick = { onSelect(spec.tab) },
            )
        }
    }
}

@Composable
private fun androidx.compose.material3.NavigationBarScope.NavTabItem(
    spec: TabSpec,
    selected: Boolean,
    badgeCount: Int,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    // حركة بسيطة عند النقر: الأيقونة تكبر قليلاً ثم ترتد
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.10f else 1f,
        animationSpec = tween(280),
        label = "tabIconScale",
    )
    val pressScale = rememberPressScale(interaction, 0.88f)

    val label = stringResource(spec.labelRes)
    val badgeDescription = spec.badgeDescRes?.let { stringResource(it, badgeCount) }

    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        interactionSource = interaction,
        alwaysShowLabel = true,
        icon = {
            BadgedBox(
                badge = {
                    if (badgeCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = Color.White,
                            modifier = if (badgeDescription != null)
                                Modifier.semantics { contentDescription = badgeDescription } else Modifier,
                        ) {
                            Text(
                                text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                                fontSize = 10.sp,
                            )
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = if (selected) spec.selectedIcon else spec.unselectedIcon,
                    contentDescription = label,
                    modifier = Modifier
                        .size(24.dp)
                        .scale(iconScale * pressScale),
                )
            }
        },
        label = {
            Text(
                text = label,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
            )
        },
        colors = NavigationBarItemDefaults.colors(
            // المحدد ملوّن، والبقية باهتة
            selectedIconColor = MaterialTheme.colorScheme.primary,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        ),
    )
}
