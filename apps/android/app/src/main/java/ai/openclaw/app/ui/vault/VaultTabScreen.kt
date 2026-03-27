package ai.openclaw.app.ui.vault

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import ai.openclaw.app.MainActivity
import ai.openclaw.app.MainViewModel
import ai.openclaw.app.vault.LeaseDuration

/**
 * Content of the "Vault" bottom-navigation tab.
 *
 * When a [CloudAuthRequest] is pending it shows [VaultApprovalScreen] with a
 * biometric trigger wired through [MainActivity.launchBiometricForVault].
 * When no request is pending it shows [AgentAnalyticsDashboard] where the user
 * can review their trust history, manage active leases, and approve the morning
 * briefing of queued YELLOW-tier requests.
 */
@Composable
fun VaultTabScreen(viewModel: MainViewModel, activity: MainActivity) {
    val pendingRequest by viewModel.pendingVaultRequest.collectAsState()
    val leases by viewModel.vaultLeases.collectAsState()
    val batchRequests by viewModel.vaultBatchRequests.collectAsState()

    val request = pendingRequest
    if (request != null) {
        VaultApprovalScreen(
            request = request,
            onApprove = { selectedLease: LeaseDuration? ->
                viewModel.approveVaultRequest(selectedLease)
            },
            onDeny = {
                viewModel.denyVaultRequest()
            },
            onBiometricNeeded = { onResult ->
                activity.launchBiometricForVault(
                    title = "Verify identity",
                    subtitle = request.summary,
                    onResult = onResult,
                )
            },
        )
    } else {
        AgentAnalyticsDashboard(
            leases = leases,
            upgradeThreshold = viewModel.vaultUpgradeThreshold,
            onGrantLease = { actionKey, duration ->
                viewModel.upgradeVaultLease(actionKey, duration)
            },
            onRevokeLease = { actionKey ->
                viewModel.revokeVaultLease(actionKey)
            },
            batchRequests = batchRequests,
            onApproveBatch = { viewModel.approveBatchRequests() },
            onDismissBatchItem = { requestId -> viewModel.dismissBatchRequest(requestId) },
        )
    }
}
