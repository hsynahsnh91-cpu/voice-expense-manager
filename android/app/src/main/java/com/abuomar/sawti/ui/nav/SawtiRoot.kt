package com.abuomar.sawti.ui.nav

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abuomar.sawti.ui.screens.AuthScreen
import com.abuomar.sawti.ui.screens.BudgetScreen
import com.abuomar.sawti.ui.screens.HomeScreen
import com.abuomar.sawti.ui.screens.Tab
import com.abuomar.sawti.vm.AuthViewModel
import com.abuomar.sawti.vm.BudgetViewModel
import com.abuomar.sawti.vm.DataViewModel
import com.abuomar.sawti.vm.VoiceViewModel

/* =============================================================================
 *  جذر التنقل: يقرر أي شاشة تُعرض بناءً على حالة المصادقة الحقيقية.
 *
 *    غير مسجَّل       → AuthScreen
 *    مسجَّل بلا ميزانية → BudgetScreen («ما ميزانيتك؟»)
 *    مسجَّل وميزانية    → HomeScreen (شريط التنقل السفلي بأربعة تبويبات)
 *
 *  الانتقال بين المراحل تلاشٍ + تكبير خفيف (بدون push animation)،
 *  والتبديل بين التبويبات داخل HomeScreen بنفس المبدأ.
 * ========================================================================== */

private sealed interface Stage {
    data object Auth : Stage
    data object Budget : Stage
    data object App : Stage
}

@Composable
fun SawtiRoot(
    authViewModel: AuthViewModel,
    dataViewModel: DataViewModel,
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val voiceViewModel: VoiceViewModel = viewModel()
    val budgetViewModel: BudgetViewModel = viewModel()
    val context = LocalContext.current

    val stage = when {
        authState.signedInAs == null -> Stage.Auth
        authState.needsBudget -> Stage.Budget
        else -> Stage.App
    }

    // أي رسالة خطأ من المصادقة تُعرض في مكانها داخل AuthScreen (لا SnackBar هنا)
    val errorText = authState.errorRes?.let { res ->
        androidx.compose.ui.res.stringResource(res)
    }

    AnimatedContent(
        targetState = stage,
        transitionSpec = {
            (fadeIn(tween(340)) + scaleIn(tween(340), initialScale = 0.985f)) togetherWith
                fadeOut(tween(220))
        },
        label = "stage",
    ) { current ->
        when (current) {
            Stage.Auth -> AuthScreen(
                busy = authState.busy,
                errorText = errorText,
                onSignIn = authViewModel::signIn,
                onSignUp = authViewModel::signUp,
                onFederatedClick = {
                    // الدخول عبر Google/Apple: يُربط من قِبل المطوّر لاحقاً.
                    // لا يتم إنشاء جلسة وهمية إطلاقاً.
                },
            )

            Stage.Budget -> BudgetScreen(
                viewModel = budgetViewModel,
                onEnterApp = authViewModel::budgetCompleted,
            )

            Stage.App -> HomeScreen(
                dataViewModel = dataViewModel,
                voiceViewModel = voiceViewModel,
                startTab = Tab.TRANSACTIONS,
                onRequestSignOut = { /* العودة لمرحلة المصادقة تتم تلقائياً عبر الحالة */ },
            )
        }
    }

    // عند الخروج أو حذف الحساب يعود التطبيق تلقائياً لشاشة الدخول
    LaunchedEffect(authState.enteredApp) {
        if (authState.enteredApp) dataViewModel.refresh()
    }
}
