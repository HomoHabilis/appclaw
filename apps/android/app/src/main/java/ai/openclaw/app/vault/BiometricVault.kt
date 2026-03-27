package ai.openclaw.app.vault

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Thin wrapper around [BiometricPrompt] that exposes a simple callback-based
 * API aligned with the AppClaw vault flow.
 *
 * Usage:
 * ```
 * BiometricVault.authenticate(
 *     activity  = this,
 *     title     = "Confirm purchase",
 *     subtitle  = "OpenClaw wants to spend $200 on Delta Airlines",
 *     onSuccess = { /* send approval response */ },
 *     onFailure = { /* show error */ },
 * )
 * ```
 */
object BiometricVault {

    /**
     * Authenticators accepted: BIOMETRIC_STRONG (fingerprint / face) with
     * fallback to DEVICE_CREDENTIAL (PIN / pattern).
     *
     * Combining BIOMETRIC_STRONG | DEVICE_CREDENTIAL is valid from API 30;
     * since minSdk is 31 this is always safe.
     */
    private const val ALLOWED_AUTHENTICATORS = BIOMETRIC_STRONG or DEVICE_CREDENTIAL

    /**
     * Returns true when the device has at least one enrolled authenticator
     * that satisfies [ALLOWED_AUTHENTICATORS].
     */
    fun isAvailable(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        return manager.canAuthenticate(ALLOWED_AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Shows the system biometric prompt.
     *
     * Must be called from a [FragmentActivity] (the standard host for Compose
     * single-activity apps).
     *
     * [title]     — primary line, e.g. "Confirm purchase".
     * [subtitle]  — secondary line with action context.
     * [onSuccess] — called on the main thread when authentication succeeds.
     * [onFailure] — called on the main thread with a human-readable message on
     *               error or user cancellation.
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt =
            BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        onFailure(errString.toString())
                    }

                    override fun onAuthenticationFailed() {
                        onFailure("Authentication failed — please try again.")
                    }
                },
            )

        val info =
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
                .build()

        prompt.authenticate(info)
    }
}
