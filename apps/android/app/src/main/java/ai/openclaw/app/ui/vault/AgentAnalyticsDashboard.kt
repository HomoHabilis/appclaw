package ai.openclaw.app.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.openclaw.app.ui.LocalMobileColors
import ai.openclaw.app.ui.mobileFontFamily
import ai.openclaw.app.vault.LeaseDuration
import ai.openclaw.app.vault.PermissionLease
import ai.openclaw.app.vault.PermissionTier

/**
 * Trust analytics dashboard shown on the "Vault" tab or after the user taps
 * a progressive-trust suggestion.
 *
 * Surfaces:
 *  1. Per-action approval counts with a progress bar toward the upgrade
 *     threshold.
 *  2. Upgrade suggestion cards for actions that crossed the threshold.
 *  3. A list of active leases with their remaining time.
 *
 * [leases]              — all known [PermissionLease] records.
 * [upgradeThreshold]    — number of approvals before an upgrade is suggested.
 * [onGrantLease]        — called when the user accepts an upgrade suggestion.
 * [onRevokeLease]       — called when the user taps "Revoke" on a lease card.
 * [batchRequests]       — queued requests from the morning briefing.
 * [onApproveBatch]      — called when user taps "Approve all" in the briefing.
 * [onDismissBatchItem]  — called to dismiss a single queued request.
 */
@Composable
fun AgentAnalyticsDashboard(
    leases: List<PermissionLease>,
    upgradeThreshold: Int,
    onGrantLease: (actionKey: String, duration: LeaseDuration) -> Unit,
    onRevokeLease: (actionKey: String) -> Unit,
    batchRequests: List<ai.openclaw.app.vault.CloudAuthRequest> = emptyList(),
    onApproveBatch: () -> Unit = {},
    onDismissBatchItem: (requestId: String) -> Unit = {},
) {
    val colors = LocalMobileColors.current
    val suggestions = leases.filter { it.approvalCount >= upgradeThreshold && !hasLongLease(it) }
    val activeLeases = leases.filter { !it.isExpired && it.remainingMs > 0 }
    val history = leases.sortedByDescending { it.approvalCount }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.surface)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(24.dp))

        Text(
            text = "Agent Analytics",
            fontFamily = mobileFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = colors.text,
        )
        Text(
            text = "Track what OpenClaw has been doing and adjust your trust settings.",
            fontFamily = mobileFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = colors.textSecondary,
        )

        if (batchRequests.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            VaultBriefingCard(
                requests = batchRequests,
                onApproveAll = onApproveBatch,
                onDismiss = onDismissBatchItem,
            )
        }

        if (suggestions.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            SectionHeader(title = "Trust Upgrade Suggestions")
            Spacer(Modifier.height(12.dp))
            suggestions.forEach { lease ->
                UpgradeSuggestionCard(
                    lease = lease,
                    onGrant = { duration -> onGrantLease(lease.actionKey, duration) },
                )
                Spacer(Modifier.height(10.dp))
            }
        }

        if (activeLeases.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            SectionHeader(title = "Active Leases")
            Spacer(Modifier.height(12.dp))
            activeLeases.forEach { lease ->
                ActiveLeaseCard(lease = lease, onRevoke = { onRevokeLease(lease.actionKey) })
                Spacer(Modifier.height(10.dp))
            }
        }

        if (history.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            SectionHeader(title = "Approval History")
            Spacer(Modifier.height(12.dp))
            history.forEach { lease ->
                ApprovalHistoryRow(lease = lease, threshold = upgradeThreshold)
                HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 6.dp))
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ---------------------------------------------------------------------------
// Sub-components
// ---------------------------------------------------------------------------

@Composable
private fun SectionHeader(title: String) {
    val colors = LocalMobileColors.current
    Text(
        text = title,
        fontFamily = mobileFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        letterSpacing = 0.6.sp,
        color = colors.textTertiary,
    )
}

@Composable
private fun UpgradeSuggestionCard(lease: PermissionLease, onGrant: (LeaseDuration) -> Unit) {
    val colors = LocalMobileColors.current
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.accentSoft)
                .border(1.dp, colors.accentBorderStrong.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.TrendingUp,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Upgrade suggestion",
                fontFamily = mobileFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = colors.accent,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "You've approved \"${lease.actionKey}\" ${lease.approvalCount} times without issues. Grant a longer lease?",
            fontFamily = mobileFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = colors.text,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onGrant(LeaseDuration.SEVEN_DAYS) },
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = "7 days",
                    fontFamily = mobileFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color.White,
                )
            }
            Button(
                onClick = { onGrant(LeaseDuration.THIRTY_DAYS) },
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = colors.border,
                        contentColor = colors.text,
                    ),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = "30 days",
                    fontFamily = mobileFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun ActiveLeaseCard(lease: PermissionLease, onRevoke: () -> Unit) {
    val colors = LocalMobileColors.current
    val tierColor =
        when (lease.tier) {
            PermissionTier.RED -> colors.danger
            PermissionTier.YELLOW -> colors.warning
            PermissionTier.GREEN -> colors.success
        }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(colors.cardSurface)
                .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(tierColor),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = lease.actionKey,
                    fontFamily = mobileFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = colors.text,
                )
                Text(
                    text = formatRemaining(lease.remainingMs),
                    fontFamily = mobileFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = colors.textTertiary,
                )
            }
        }
        TextButton(onClick = onRevoke) {
            Text(
                text = "Revoke",
                fontFamily = mobileFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = colors.danger,
            )
        }
    }
}

@Composable
private fun ApprovalHistoryRow(lease: PermissionLease, threshold: Int) {
    val colors = LocalMobileColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = colors.success,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = lease.actionKey,
                fontFamily = mobileFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = colors.text,
            )
        }
        Text(
            text = "${lease.approvalCount} / $threshold approvals",
            fontFamily = mobileFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = colors.textTertiary,
        )
    }
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

private fun hasLongLease(lease: PermissionLease): Boolean =
    lease.expiresAtMs - lease.grantedAtMs >= LeaseDuration.THIRTY_DAYS.durationMs

private fun formatRemaining(ms: Long): String {
    if (ms <= 0) return "Expired"
    val minutes = ms / 60_000
    val hours = minutes / 60
    val days = hours / 24
    return when {
        days > 0 -> "$days day${if (days == 1L) "" else "s"} remaining"
        hours > 0 -> "$hours hour${if (hours == 1L) "" else "s"} remaining"
        else -> "$minutes minute${if (minutes == 1L) "" else "s"} remaining"
    }
}
