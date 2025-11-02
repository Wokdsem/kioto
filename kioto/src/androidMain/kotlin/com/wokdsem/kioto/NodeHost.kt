package com.wokdsem.kioto

import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import com.wokdsem.kioto.engine.BackHandler
import com.wokdsem.kioto.engine.HostBundle
import com.wokdsem.kioto.engine.NodeHost
import com.wokdsem.kioto.engine.NodeNavRunner
import com.wokdsem.kioto.engine.Releasable

/**
 * A composable function that renders the state of a [NodeNav].
 *
 * @see NodeNav
 *
 * @param nodeNav The NodeNav instance that handles navigation actions.
 */
@Composable
public fun NodeHost(nodeNav: NodeNav) {
    val compactActivity = LocalActivity.current as? AppCompatActivity ?: throw IllegalStateException("NodeHost must be used within an AppCompatActivity context")
    NodeHost(
        bundle = HostBundle(
            nodeNav = nodeNav as NodeNavRunner,
            platform = Platform.ANDROID,
            backHandler = AndroidPredictiveBackHandler(compactActivity)
        )
    )
}

private class AndroidPredictiveBackHandler(private val activity: AppCompatActivity) : BackHandler {
    override fun register(callback: BackHandler.Callback): Releasable {
        val backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackStarted(backEvent: BackEventCompat) = callback.onBackStarted()
            override fun handleOnBackProgressed(backEvent: BackEventCompat) = callback.onBackProgressed(backEvent.progress)
            override fun handleOnBackPressed() = callback.onBackPressed()
            override fun handleOnBackCancelled() = callback.onBackCancelled()
        }.also(activity.onBackPressedDispatcher::addCallback)
        return Releasable { backPressedCallback.remove() }
    }
}
