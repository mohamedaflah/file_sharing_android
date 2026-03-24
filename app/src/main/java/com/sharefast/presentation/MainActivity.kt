package com.sharefast.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sharefast.presentation.navigation.ShareFastRoot
import com.sharefast.presentation.TransferNavExtras
import com.sharefast.presentation.theme.ShareFastTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val transferNavState = mutableStateOf(TransferNavExtras())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeIntent(intent)
        setContent {
            val systemDark = isSystemInDarkTheme()
            var amoledBlack by rememberSaveable { mutableStateOf(false) }
            var forceLight by rememberSaveable { mutableStateOf(false) }
            var forceDark by rememberSaveable { mutableStateOf(false) }
            val darkTheme = when {
                forceLight -> false
                forceDark -> true
                else -> systemDark
            }
            val transferNav by transferNavState
            ShareFastTheme(darkTheme = darkTheme, amoled = amoledBlack) {
                ShareFastRoot(
                    modifier = Modifier,
                    amoledBlack = amoledBlack,
                    onAmoledChange = { amoledBlack = it },
                    onThemeCycle = {
                        when {
                            !forceLight && !forceDark -> forceLight = true
                            forceLight -> {
                                forceLight = false
                                forceDark = true
                            }
                            else -> {
                                forceDark = false
                            }
                        }
                    },
                    transferNavExtras = transferNav,
                    onConsumedTransferNav = {
                        transferNavState.value = TransferNavExtras()
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeIntent(intent)
    }

    private fun consumeIntent(intent: Intent?) {
        if (intent == null) return
        val scroll = intent.getBooleanExtra(EXTRA_OPEN_TRANSFERS, false)
        val uri = intent.getStringExtra(EXTRA_OPEN_URI)
        val name = intent.getStringExtra(EXTRA_OPEN_FILENAME)
        if (!scroll && uri == null) return
        transferNavState.value = TransferNavExtras(
            scrollToRecent = scroll && uri == null,
            openUri = uri,
            openFileNameHint = name,
        )
        intent.removeExtra(EXTRA_OPEN_TRANSFERS)
        intent.removeExtra(EXTRA_OPEN_URI)
        intent.removeExtra(EXTRA_OPEN_FILENAME)
    }

    companion object {
        const val EXTRA_OPEN_TRANSFERS = "sharefast_open_transfers"
        const val EXTRA_OPEN_URI = "sharefast_open_uri"
        const val EXTRA_OPEN_FILENAME = "sharefast_open_filename"
    }
}
