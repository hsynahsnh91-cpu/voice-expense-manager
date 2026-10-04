package com.abuomar.sawti.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.DisposableEffect
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
import com.abuomar.sawti.data.Category
import com.abuomar.sawti.data.Transaction
import com.abuomar.sawti.ui.components.EmptyState
import com.abuomar.sawti.ui.components.Hairline
import com.abuomar.sawti.ui.components.SectionCard
import com.abuomar.sawti.ui.components.ScreenHeader
import com.abuomar.sawti.ui.components.TagChip
import com.abuomar.sawti.ui.components.pressable
import com.abuomar.sawti.ui.components.rememberPressScale
import com.abuomar.sawti.ui.theme.categoryColor
import com.abuomar.sawti.vm.DataViewModel
import kotlinx.coroutines.launch

/* =============================================================================
 *  قسم الوارد — التسجيلات الصوتية المحفوظة.
 *
 *  إذا بقيت الملاحظة الصوتية لفترة وأراد المستخدم التأكد مما قيل، يمكنه
 *  الاستماع إلى تسجيل الشخص الذي تكلّم: زر تشغيل حقيقي (MediaPlayer) مع
 *  موجة صوتية وتقدّم، ويُوسم العنصر بأنه «تم الاستماع» فيختفي من العدّاد.
 *
 *  ملاحظة: خيار «كتم إعادة الصوت بعد التسجيل» يمنع التشغيل التلقائي دائماً —
 *  التشغيل هنا يتم بضغط المستخدم فقط.
 * ========================================================================== */

@Composable
fun InboxScreen(
    viewModel: DataViewModel,
    snackbarHostState: androidx.compose.material3.SnackbarHostState,
) {
    val inbox by viewModel.inbox.collectAsStateWithLifecycle()
    val prefs by viewModel.prefsState.collectAsStateWithLifecycle()
    val playback = remember { SawtiApp.container().audioPlayback }
    val playingId by playback.playingId.collectAsStateWithLifecycle()
    val progress by playback.progress.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var pendingDelete by remember { mutableStateOf<Transaction?>(null) }
    val scope = rememberCoroutineScope()

    // إيقاف التشغيل عند مغادرة الشاشة حتى لا يبقى صوت يعمل بالخلفية
    DisposableEffect(Unit) { onDispose { playback.stop() } }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                ScreenHeader(
                    title = stringResource(R.string.inbox_title),
                    subtitle = if (inbox.isEmpty()) stringResource(R.string.inbox_subtitle)
                               else stringResource(R.string.inbox_count, inbox.size),
                    trailing = {
                        if (playingId != null) {
                            val interaction = remember { MutableInteractionSource() }
                            val scale = rememberPressScale(interaction, 0.9f)
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.scale(scale).pressable(interaction) { playback.stop() },
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Stop, contentDescription = stringResource(R.string.inbox_stop),
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(15.dp),
                                    )
                                    Text(
                                        stringResource(R.string.inbox_stop),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    },
                )
            }

            if (inbox.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                ) {
                    EmptyState(
                        icon = "🎙️",
                        title = stringResource(R.string.inbox_empty_title),
                        hint = stringResource(R.string.inbox_empty_hint),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    items(inbox, key = { it.id }) { tx ->
                        InboxCard(
                            tx = tx,
                            arabic = prefs.arabic,
                            money = { viewModel.formatMoney(it) },
                            isPlaying = playingId == tx.id,
                            progress = if (playingId == tx.id) progress else 0f,
                            onPlay = {
                                if (playingId == tx.id) {
                                    playback.stop()
                                } else {
                                    val file = viewModel.audioFileFor(tx.id)
                                    if (file == null || !file.exists()) {
                                        // لا يوجد ملف — نخبر المستخدم بصدق ولا نشغّل شيئاً وهمياً
                                        scope.launch {
                                            snackbarHostState.showSnackbar(context.getString(R.string.inbox_audio_missing))
                                        }
                                    } else {
                                        val ok = playback.play(tx.id, file) { viewModel.markListened(tx.id) }
                                        if (ok) {
                                            viewModel.markListened(tx.id)
                                        } else {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(context.getString(R.string.inbox_audio_error))
                                            }
                                        }
                                    }
                                }
                            },
                            onDelete = { pendingDelete = tx },
                        )
                    }
                }
            }
        }

        pendingDelete?.let { tx ->
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                shape = MaterialTheme.shapes.large,
                title = { Text(stringResource(R.string.confirm_delete_title)) },
                text = { Text(stringResource(R.string.inbox_delete_note)) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteTransaction(tx.id)
                        pendingDelete = null
                    }) {
                        Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) } },
            )
        }
    }
}

/* ================================ بطاقة وارد =============================== */
@Composable
private fun InboxCard(
    tx: Transaction,
    arabic: Boolean,
    money: (Long) -> String,
    isPlaying: Boolean,
    progress: Float,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
) {
    val cat = Category.of(tx.category)
    val color = categoryColor(cat.id)
    val interaction = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interaction, 0.985f)
    val played = tx.listened

    val ageDays = remember(tx.date) {
        val diff = CivilDate.daysBetween(tx.date, CivilDate.today())
        diff.coerceAtLeast(0)
    }

    SectionCard(
        padding = 14.dp,
        modifier = Modifier.scale(scale).pressable(interaction, onClick = onPlay),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            /* زر التشغيل */
            val btnInteraction = remember { MutableInteractionSource() }
            val btnScale = rememberPressScale(btnInteraction, 0.88f)
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .scale(btnScale)
                    .clip(CircleShape)
                    .background(if (isPlaying) MaterialTheme.colorScheme.error else color)
                    .pressable(btnInteraction, onClick = onPlay),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = stringResource(if (isPlaying) R.string.inbox_stop else R.string.inbox_listen),
                    tint = Color.White,
                    modifier = Modifier.size(if (isPlaying) 24.dp else 28.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(cat.emoji, fontSize = 14.sp)
                    Text(
                        text = stringResource(cat.nameRes),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                    )
                    if (!played) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = buildString {
                        append(money(tx.amountNew))
                        append(" · ")
                        append(CivilDate.formatPretty(tx.date, arabic))
                        if (tx.audioDurationSec > 0) {
                            append(" · ")
                            append("${tx.audioDurationSec}s")
                        }
                        if (ageDays > 0) {
                            append(" · ")
                            append(if (arabic) "منذ $ageDays يوم" else "${ageDays}d ago")
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(17.dp),
                )
            }
        }

        /* شريط التقدم أثناء التشغيل */
        AnimatedVisibility(
            visible = isPlaying,
            enter = fadeIn(tween(200)) + slideInVertically(tween(240)) { -it / 4 },
            exit = fadeOut(tween(160)),
        ) {
            Column {
                Spacer(Modifier.height(11.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(120), label = "inboxProgress")
                    Box(
                        Modifier
                            .fillMaxWidth(p)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(color)
                    )
                }
            }
        }

        /* النص المنطوق */
        if (!tx.transcript.isNullOrBlank()) {
            Spacer(Modifier.height(11.dp))
            Hairline()
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text(
                        text = stringResource(R.string.voice_transcript),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = tx.transcript!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            TagChip(
                text = stringResource(if (played) R.string.inbox_listened else R.string.inbox_unheard),
                color = if (played) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            if (!tx.muted) {
                TagChip(text = stringResource(R.string.inbox_can_replay), color = MaterialTheme.colorScheme.tertiary)
            }
            if (tx.merchant != null) {
                TagChip(text = tx.merchant!!, color = color)
            }
        }
    }
}

