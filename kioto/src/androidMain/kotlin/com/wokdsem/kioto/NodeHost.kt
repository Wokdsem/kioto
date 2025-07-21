package com.wokdsem.kioto

import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable

/**
 * A composable function that renders the state of a [NodeNav].
 *
 * @see NodeNav
 *
 * @param navigation The NodeNav instance that handles navigation actions.
 */
@Composable
public fun NodeHost(
    navigation: NodeNav,
    onStackCleared: () -> Unit,
) {
    val activity = LocalActivity.current ?: return
    val compactActivity = activity as? AppCompatActivity
    NodeHost(
        bundle = HostBundle(
            navigation = navigation,
            onStackCleared = onStackCleared,
            platform = Platform.ANDROID,
            backHandler = compactActivity?.let(::AndroidBackHandler),
            predictiveBackHandler = compactActivity?.let(::AndroidPredictiveBackHandler)
        )
    )
}

private class AndroidBackHandler(private val activity: AppCompatActivity) : BackHandler {
    override fun register(callback: BackHandler.Callback): Releasable {
        val backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = callback.onBackPressed()
        }.also(activity.onBackPressedDispatcher::addCallback)
        return Releasable(backPressedCallback::remove)
    }
}

private class AndroidPredictiveBackHandler(private val activity: AppCompatActivity) : PredictiveBackHandler {
    override fun register(callback: PredictiveBackHandler.Callback): Releasable {
        val predictiveBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackStarted(backEvent: BackEventCompat) = callback.onBackStarted()
            override fun handleOnBackProgressed(backEvent: BackEventCompat) = callback.onBackProgressed(backEvent.progress)
            override fun handleOnBackPressed() = callback.onBackPressed()
            override fun handleOnBackCancelled() = callback.onBackCancelled()
        }.also(activity.onBackPressedDispatcher::addCallback)
        return Releasable(predictiveBackPressedCallback::remove)
    }
}
