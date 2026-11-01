package com.ledgerly.app

import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ledgerly.app.ui.AppViewModel
import com.ledgerly.app.ui.LedgerlyApp

class MainActivity : ComponentActivity() {

    /** Re-lock only when the app was actually in the background for a moment. */
    private var stoppedAt = 0L
    private var locked by mutableStateOf(false)
    private var pendingAdd by mutableStateOf(false)

    private val viewModel: AppViewModel by lazy { AppViewModel(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val keyguard = getSystemService(KeyguardManager::class.java)
        locked = viewModel.settings.value.appLock && keyguard?.isDeviceSecure == true
        pendingAdd = intent?.action == ACTION_ADD_EXPENSE
        setContent {
            LedgerlyApp(
                viewModel = viewModel,
                locked = locked,
                onUnlock = { locked = false },
                pendingAdd = pendingAdd,
                onPendingAddHandled = { pendingAdd = false }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == ACTION_ADD_EXPENSE) pendingAdd = true
    }

    override fun onStop() {
        super.onStop()
        stoppedAt = System.currentTimeMillis()
    }

    override fun onStart() {
        super.onStart()
        val away = stoppedAt == 0L || System.currentTimeMillis() - stoppedAt > RELOCK_AFTER_MS
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (away && viewModel.settings.value.appLock && keyguard?.isDeviceSecure == true) {
            locked = true
        }
    }

    private companion object {
        const val ACTION_ADD_EXPENSE = "com.ledgerly.app.action.ADD_EXPENSE"
        const val RELOCK_AFTER_MS = 15_000L
    }
}