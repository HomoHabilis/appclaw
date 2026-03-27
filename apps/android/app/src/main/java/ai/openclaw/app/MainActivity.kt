package ai.openclaw.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import ai.openclaw.app.ui.RootScreen
import ai.openclaw.app.ui.OpenClawTheme
import ai.openclaw.app.vault.BiometricVault
import ai.openclaw.app.vault.LeaseDuration
import androidx.core.view.WindowCompat
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
  private val viewModel: MainViewModel by viewModels()
  private lateinit var permissionRequester: PermissionRequester
  private var didAttachRuntimeUi = false
  private var didStartNodeService = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WindowCompat.setDecorFitsSystemWindows(window, false)
    permissionRequester = PermissionRequester(this)

    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.preventSleep.collect { enabled ->
          if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
          } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
          }
        }
      }
    }

    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.runtimeInitialized.collect { ready ->
          if (!ready || didAttachRuntimeUi) return@collect
          viewModel.attachRuntimeUi(owner = this@MainActivity, permissionRequester = permissionRequester)
          didAttachRuntimeUi = true
          if (!didStartNodeService) {
            NodeForegroundService.start(this@MainActivity)
            didStartNodeService = true
          }
        }
      }
    }

    setContent {
      OpenClawTheme {
        Surface(modifier = Modifier) {
          RootScreen(viewModel = viewModel, activity = this)
        }
      }
    }
  }

  /**
   * Triggered by [VaultApprovalScreen] when the user taps "Scan fingerprint".
   * The [onResult] callback is called on the main thread with the biometric outcome.
   */
  fun launchBiometricForVault(
    title: String,
    subtitle: String,
    onResult: (approved: Boolean) -> Unit,
  ) {
    if (!BiometricVault.isAvailable(this)) {
      // No biometric enrolled — fall back to approved=false so the user sees an error.
      onResult(false)
      return
    }
    BiometricVault.authenticate(
      activity = this,
      title = title,
      subtitle = subtitle,
      onSuccess = { onResult(true) },
      onFailure = { onResult(false) },
    )
  }

  override fun onStart() {
    super.onStart()
    viewModel.setForeground(true)
  }

  override fun onStop() {
    viewModel.setForeground(false)
    super.onStop()
  }
}
