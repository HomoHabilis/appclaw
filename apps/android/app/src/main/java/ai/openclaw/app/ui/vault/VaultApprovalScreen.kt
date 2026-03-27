package ai.openclaw.app.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ai.openclaw.app.ui.LocalMobileColors
import ai.openclaw.app.ui.mobileFontFamily
import ai.openclaw.app.vault.CloudAuthRequest
import ai.openclaw.app.vault.LeaseDuration
import ai.openclaw.app.vault.PermissionTier

/**
 * Full-screen approval prompt shown whenever [PermissionLeaseManager.decide]
 * returns [AuthDecision.RequireBiometric].
 *
 * Layout:
 *  ┌──────────────────────────────────────────┐
 *  │  Tier badge  (🔴 HIGH RISK)              │
 *  │  Summary headline                        │
 *  │  Context detail paragraph                │
 *  │  ─────────────────────────────────────── │
 *  │  Lease picker (YELLOW only)              │
 *  │  ─────────────────────────────────────── │
 *  │  [Scan fingerprint]  [Deny]              │
 *  └──────────────────────────────────────────┘
 *
 * [request]           — the incoming cloud auth request.
 * [onApprove]         — called with the chosen [LeaseDuration] (null = no lease /
 *                       RED tier single approval).
 * [onDeny]            — called when the user dismisses without approving.
 * [onBiometricNeeded] — called to trigger [BiometricVault.authenticate]; the
 *                       caller (FragmentActivity) owns the prompt lifecycle.
 */
@Composable
fun VaultApprovalScreen(
    request: CloudAuthRequest,
    onApprove: (selectedLease: LeaseDuration?) -> Unit,
    onDeny: () -> Unit,
    onBiometricNeeded: (onResult: (Boolean) -> Unit) -> Unit,
) {
    val colors = LocalMobileColors.current
    val tierColor = tierColor(request.tier, colors)
    val tierBgColor = tierBgColor(request.tier, colors)

    var selectedLease by remember { mutableStateOf<LeaseDuration?>(LeaseDuration.ONE_HOUR) }
    var biometricError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(colors.surface)
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(32.dp))

            TierBadge(tier = request.tier, color = tierColor, bgColor = tierBgColor)

            Spacer(Modifier.height(20.dp))

            Text(
                text = request.summary,
                fontFamily = mobileFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                color = colors.text,
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = request.contextDetail,
                fontFamily = mobileFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = colors.textSecondary,
            )

            if (request.tier.supportsLease) {
                Spacer(Modifier.height(28.dp))
                HorizontalDivider(color = colors.border)
                Spacer(Modifier.height(20.dp))

                LeasePicker(selected = selectedLease, onSelect = { selectedLease = it })
            }

            biometricError?.let { err ->
                Spacer(Modifier.height(16.dp))
                Text(
                    text = err,
                    fontFamily = mobileFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = colors.danger,
                )
            }

            Spacer(Modifier.height(28.dp))
        }

        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            HorizontalDivider(color = colors.border)
            Spacer(Modifier.height(16.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = tierColor,
                        contentColor = Color.White,
                    ),
                shape = RoundedCornerShape(12.dp),
                onClick = {
                    biometricError = null
                    onBiometricNeeded { success ->
                        if (success) {
                            onApprove(if (request.tier.supportsLease) selectedLease else null)
                        } else {
                            biometricError = "Authentication failed — please try again."
                        }
                    }
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Scan fingerprint to approve",
                    fontFamily = mobileFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onDeny,
            ) {
                Text(
                    text = "Deny",
                    fontFamily = mobileFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = colors.textSecondary,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Sub-components
// ---------------------------------------------------------------------------

@Composable
private fun TierBadge(tier: PermissionTier, color: Color, bgColor: Color) {
    val icon =
        when (tier) {
            PermissionTier.RED -> Icons.Default.Security
            PermissionTier.YELLOW -> Icons.Default.Lock
            PermissionTier.GREEN -> Icons.Default.Lock
        }
    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(bgColor)
                .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            text = tier.displayLabel.uppercase(),
            fontFamily = mobileFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            letterSpacing = 0.8.sp,
            color = color,
        )
    }
}

@Composable
private fun LeasePicker(selected: LeaseDuration?, onSelect: (LeaseDuration?) -> Unit) {
    val colors = LocalMobileColors.current

    Text(
        text = "Keep access open for",
        fontFamily = mobileFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        color = colors.textSecondary,
    )
    Spacer(Modifier.height(10.dp))
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        LeaseDuration.entries.take(3).forEach { dur ->
            LeaseChip(label = dur.label, selected = selected == dur) { onSelect(dur) }
        }
    }
    Spacer(Modifier.height(6.dp))
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LeaseDuration.entries.drop(3).forEach { dur ->
            LeaseChip(label = dur.label, selected = selected == dur) { onSelect(dur) }
        }
        LeaseChip(label = "Just once", selected = selected == null) { onSelect(null) }
    }
}

@Composable
private fun LeaseChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalMobileColors.current
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontFamily = mobileFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
            )
        },
        colors =
            FilterChipDefaults.filterChipColors(
                selectedContainerColor = colors.accentSoft,
                selectedLabelColor = colors.accent,
            ),
        border =
            FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selected,
                borderColor = colors.border,
                selectedBorderColor = colors.accentBorderStrong,
            ),
    )
}

// ---------------------------------------------------------------------------
// Color helpers (tier → semantic token)
// ---------------------------------------------------------------------------

private fun tierColor(tier: PermissionTier, colors: ai.openclaw.app.ui.MobileColors): Color =
    when (tier) {
        PermissionTier.RED -> colors.danger
        PermissionTier.YELLOW -> colors.warning
        PermissionTier.GREEN -> colors.success
    }

private fun tierBgColor(tier: PermissionTier, colors: ai.openclaw.app.ui.MobileColors): Color =
    when (tier) {
        PermissionTier.RED -> colors.dangerSoft
        PermissionTier.YELLOW -> colors.warningSoft
        PermissionTier.GREEN -> colors.successSoft
    }
