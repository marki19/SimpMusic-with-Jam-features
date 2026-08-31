package com.maxrave.simpmusic.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.maxrave.simpmusic.expect.ui.layerBackdrop
import com.maxrave.simpmusic.expect.ui.rememberBackdrop
import com.maxrave.simpmusic.extension.angledGradientBackground
import com.maxrave.simpmusic.extension.artworkScrimBrush
import com.maxrave.simpmusic.extension.rgbFactor
import com.maxrave.simpmusic.ui.component.EndOfPage
import com.maxrave.simpmusic.ui.component.LiquidGlassIconButton
import com.maxrave.simpmusic.ui.icon.ArrowBackIosNew
import com.maxrave.simpmusic.ui.icon.Logout
import com.maxrave.simpmusic.ui.icon.SimpIcons
import com.maxrave.simpmusic.ui.theme.typo
import com.maxrave.simpmusic.viewModel.ListenTogetherSettingsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import simpmusic.composeapp.generated.resources.Res
import simpmusic.composeapp.generated.resources.listen_together
import simpmusic.composeapp.generated.resources.lt_as_host
import simpmusic.composeapp.generated.resources.lt_auto_approve_joins
import simpmusic.composeapp.generated.resources.lt_auto_approve_joins_desc
import simpmusic.composeapp.generated.resources.lt_blocked
import simpmusic.composeapp.generated.resources.lt_blocked_empty
import simpmusic.composeapp.generated.resources.lt_end_room
import simpmusic.composeapp.generated.resources.lt_jam_allow_play_direct
import simpmusic.composeapp.generated.resources.lt_jam_allow_play_direct_desc
import simpmusic.composeapp.generated.resources.lt_jam_allow_play_pause
import simpmusic.composeapp.generated.resources.lt_jam_allow_play_pause_desc
import simpmusic.composeapp.generated.resources.lt_jam_allow_queue
import simpmusic.composeapp.generated.resources.lt_jam_allow_queue_desc
import simpmusic.composeapp.generated.resources.lt_jam_allow_reorder
import simpmusic.composeapp.generated.resources.lt_jam_allow_reorder_desc
import simpmusic.composeapp.generated.resources.lt_jam_allow_seek
import simpmusic.composeapp.generated.resources.lt_jam_allow_seek_desc
import simpmusic.composeapp.generated.resources.lt_jam_settings
import simpmusic.composeapp.generated.resources.lt_jam_settings_desc
import simpmusic.composeapp.generated.resources.lt_leave_room
import simpmusic.composeapp.generated.resources.lt_unblock

/** Generous width for settings on tablets / wide screens. */
private const val CONTENT_MAX_WIDTH_DP = 640
private val CARD_SHAPE = RoundedCornerShape(20.dp)

@Composable
fun ListenTogetherSettingsScreen(
    navController: NavController,
    innerPadding: PaddingValues,
    viewModel: ListenTogetherSettingsViewModel = koinViewModel(),
) {
    val room by viewModel.room.collectAsStateWithLifecycle()
    val autoJoins by viewModel.autoApproveJoins.collectAsStateWithLifecycle()
    val blocked by viewModel.blockedNames.collectAsStateWithLifecycle()

    val jamAllowQueue by viewModel.jamAllowQueue.collectAsStateWithLifecycle()
    val jamAllowReorder by viewModel.jamAllowReorder.collectAsStateWithLifecycle()
    val jamAllowPlayDirect by viewModel.jamAllowPlayDirect.collectAsStateWithLifecycle()
    val jamAllowSeek by viewModel.jamAllowSeek.collectAsStateWithLifecycle()
    val jamAllowPlayPause by viewModel.jamAllowPlayPause.collectAsStateWithLifecycle()

    val backdrop = rememberBackdrop(MaterialTheme.colorScheme.background)

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val contentWidth = minOf(maxWidth.value, CONTENT_MAX_WIDTH_DP.toFloat()).dp

        val bg = MaterialTheme.colorScheme.background
        val glow =
            if (bg.luminance() > 0.5f) {
                lerp(MaterialTheme.colorScheme.primary, Color.White, 0.85f)
            } else {
                MaterialTheme.colorScheme.primary.rgbFactor(0.3f)
            }
        Box(Modifier.matchParentSize().layerBackdrop(backdrop)) {
            Box(Modifier.matchParentSize().background(bg))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .angledGradientBackground(listOf(glow, bg), 25f),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .align(Alignment.BottomCenter)
                        .background(artworkScrimBrush(bg)),
                )
            }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Full-width header
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(48.dp)
                        .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.width(48.dp))
                Text(
                    text = stringResource(Res.string.listen_together),
                    style = typo().titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(48.dp))
            }

            Column(
                modifier =
                    Modifier
                        .width(contentWidth)
                        .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                // ── When you host ─────────────────────────────────────────────
                SettingCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SectionTitle(stringResource(Res.string.lt_as_host))
                        ToggleRow(
                            title = stringResource(Res.string.lt_auto_approve_joins),
                            subtitle = stringResource(Res.string.lt_auto_approve_joins_desc),
                            checked = autoJoins,
                            onCheckedChange = { viewModel.setAutoApproveJoins(it) },
                        )
                    }
                }

                // ── Jam Permissions ───────────────────────────────────────────
                SettingCard {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            SectionTitle(stringResource(Res.string.lt_jam_settings))
                            Text(
                                stringResource(Res.string.lt_jam_settings_desc),
                                style = typo().bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        ToggleRow(
                            title = stringResource(Res.string.lt_jam_allow_queue),
                            subtitle = stringResource(Res.string.lt_jam_allow_queue_desc),
                            checked = jamAllowQueue,
                            onCheckedChange = { viewModel.setJamAllowQueue(it) },
                        )

                        ToggleRow(
                            title = stringResource(Res.string.lt_jam_allow_reorder),
                            subtitle = stringResource(Res.string.lt_jam_allow_reorder_desc),
                            checked = jamAllowReorder,
                            onCheckedChange = { viewModel.setJamAllowReorder(it) },
                        )

                        ToggleRow(
                            title = stringResource(Res.string.lt_jam_allow_play_direct),
                            subtitle = stringResource(Res.string.lt_jam_allow_play_direct_desc),
                            checked = jamAllowPlayDirect,
                            onCheckedChange = { viewModel.setJamAllowPlayDirect(it) },
                        )

                        ToggleRow(
                            title = stringResource(Res.string.lt_jam_allow_seek),
                            subtitle = stringResource(Res.string.lt_jam_allow_seek_desc),
                            checked = jamAllowSeek,
                            onCheckedChange = { viewModel.setJamAllowSeek(it) },
                        )

                        ToggleRow(
                            title = stringResource(Res.string.lt_jam_allow_play_pause),
                            subtitle = stringResource(Res.string.lt_jam_allow_play_pause_desc),
                            checked = jamAllowPlayPause,
                            onCheckedChange = { viewModel.setJamAllowPlayPause(it) },
                        )
                    }
                }

                // ── Blocked ───────────────────────────────────────────────────
                SettingCard {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SectionTitle(stringResource(Res.string.lt_blocked))
                            Text("${blocked.size}", style = typo().bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (blocked.isEmpty()) {
                            Text(
                                stringResource(Res.string.lt_blocked_empty),
                                style = typo().bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            blocked.forEach { name ->
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            name.trim().firstOrNull()?.uppercase() ?: "?",
                                            style = typo().titleSmall,
                                            color = MaterialTheme.colorScheme.surface,
                                        )
                                    }
                                    Text(
                                        name,
                                        style = typo().bodyMedium,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    SmallAction(text = stringResource(Res.string.lt_unblock)) { viewModel.unblock(name) }
                                }
                            }
                        }
                    }
                }

                // ── Session controls (when in room) ───────────────────────────
                AnimatedVisibility(visible = room.inRoom) {
                    SettingCard {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionTitle("Active Jam Session")
                            if (room.isHost) {
                                FilledTonalButton(
                                    onClick = {
                                        viewModel.endRoom()
                                        navController.navigateUp()
                                    },
                                    shape = CircleShape,
                                    colors =
                                        ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                                            contentColor = MaterialTheme.colorScheme.error,
                                        ),
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(SimpIcons.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(Res.string.lt_end_room), style = typo().bodyMedium)
                                }
                            }
                            FilledTonalButton(
                                onClick = {
                                    viewModel.leaveRoom()
                                    navController.navigateUp()
                                },
                                shape = CircleShape,
                                colors =
                                    ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f),
                                        contentColor = MaterialTheme.colorScheme.onBackground,
                                    ),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(SimpIcons.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(Res.string.lt_leave_room), style = typo().bodyMedium)
                            }
                        }
                    }
                }

                EndOfPage()
            }
        }

        LiquidGlassIconButton(
            backdrop = backdrop,
            imageVector = SimpIcons.ArrowBackIosNew,
            tint = MaterialTheme.colorScheme.onSurface,
            shape = RoundedCornerShape(24.dp),
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 12.dp, top = 8.dp)
                    .size(48.dp),
        ) {
            navController.navigateUp()
        }
    }
}

@Composable
private fun SettingCard(content: @Composable () -> Unit) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(CARD_SHAPE)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.05f))
                .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f), CARD_SHAPE)
                .padding(20.dp),
    ) {
        content()
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = typo().titleSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onBackground)
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onCheckedChange(!checked) }
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = typo().bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = typo().bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LtSwitch(checked = checked)
    }
}

/** The artboard's own switch: a 44×26 track with a 20dp thumb sliding between two insets. */
@Composable
private fun LtSwitch(checked: Boolean) {
    val thumbOffset by animateDpAsState(if (checked) 21.dp else 3.dp, label = "ltSwitchThumb")
    Box(
        modifier =
            Modifier
                .size(width = 44.dp, height = 26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.26f)),
    ) {
        Box(
            modifier =
                Modifier
                    .padding(top = 3.dp)
                    .offset(x = thumbOffset)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        if (checked) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    ),
        )
    }
}

@Composable
private fun SmallAction(
    text: String,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f), RoundedCornerShape(16.dp))
                .clickable { onClick() }
                .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = typo().labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}