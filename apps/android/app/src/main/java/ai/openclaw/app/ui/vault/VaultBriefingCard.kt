package ai.openclaw.app.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
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
import ai.openclaw.app.vault.CloudAuthRequest

/**
 * Morning briefing card shown at the top of [AgentAnalyticsDashboard] when
 * YELLOW-tier requests accumulated during quiet hours.
 *
 * Lets the user approve the entire batch with one biometric scan, or dismiss
 * individual items.
 *
 * [requests]            — queued requests from [VaultRequestQueue.pendingBatch].
 * [onApproveAll]        — called when the user taps "Approve all (biometric)".
 * [onDismiss]           — called with a single [requestId] to remove that item.
 */
@Composable
fun VaultBriefingCard(
    requests: List<CloudAuthRequest>,
    onApproveAll: () -> Unit,
    onDismiss: (requestId: String) -> Unit,
) {
    if (requests.isEmpty()) return

    val colors = LocalMobileColors.current

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.warningSoft)
                .border(1.dp, colors.warning.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = null,
                tint = colors.warning,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Morning briefing — ${requests.size} queued request${if (requests.size == 1) "" else "s"}",
                fontFamily = mobileFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = colors.warning,
            )
        }

        Spacer(Modifier.height(12.dp))

        requests.forEach { req ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = req.summary,
                    fontFamily = mobileFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = colors.text,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onDismiss(req.requestId) }) {
                    Text(
                        text = "Dismiss",
                        fontFamily = mobileFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = colors.textTertiary,
                    )
                }
            }
            HorizontalDivider(color = colors.border, modifier = Modifier.padding(vertical = 4.dp))
        }

        Spacer(Modifier.height(12.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = colors.warning,
                    contentColor = Color.White,
                ),
            shape = RoundedCornerShape(10.dp),
            onClick = onApproveAll,
        ) {
            Text(
                text = "Approve all",
                fontFamily = mobileFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
    }
}
