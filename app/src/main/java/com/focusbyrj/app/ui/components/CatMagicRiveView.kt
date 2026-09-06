package com.focusbyrj.app.ui.components

import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.rive.runtime.kotlin.RiveAnimationView
import app.rive.runtime.kotlin.core.Alignment as RiveAlignment
import app.rive.runtime.kotlin.core.Fit
import app.rive.runtime.kotlin.core.Loop
import com.focusbyrj.app.R

data class RiveCatAsset(
    val resId: Int,
    val assetName: String,
    val displayName: String
)

val WELCOME_CAT_RIVE_ASSETS = listOf(
    RiveCatAsset(resId = R.raw.cat_magic, assetName = "cat_magic.riv", displayName = "Magic Cat"),
    RiveCatAsset(resId = R.raw.cat_gold_fish, assetName = "cat_gold_fish.riv", displayName = "Goldfish Aquarium Cat"),
    RiveCatAsset(resId = R.raw.cat_googlyeyes, assetName = "cat_googlyeyes.riv", displayName = "Googly Eyes Cat"),
    RiveCatAsset(resId = R.raw.cat_shiftyhead, assetName = "cat_shiftyhead.riv", displayName = "Shifty Head Cat"),
    RiveCatAsset(resId = R.raw.cat_blunng, assetName = "cat_blunng.riv", displayName = "Blunng Cat"),
    RiveCatAsset(resId = R.raw.cat_wildfairy, assetName = "cat_wildfairy.riv", displayName = "Wild Fairy Cat")
)

@Composable
fun CatWelcomeRiveView(
    modifier: Modifier = Modifier,
    messageId: String? = null
) {
    val initialIndex = remember(messageId) {
        if (WELCOME_CAT_RIVE_ASSETS.isNotEmpty()) {
            kotlin.random.Random.nextInt(WELCOME_CAT_RIVE_ASSETS.size)
        } else 0
    }
    var currentCatIndex by remember(messageId) { mutableIntStateOf(initialIndex) }
    val currentCat = WELCOME_CAT_RIVE_ASSETS.getOrElse(currentCatIndex) { WELCOME_CAT_RIVE_ASSETS[0] }

    var riveViewRef: RiveAnimationView? = null

    Box(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) {
            try {
                riveViewRef?.let { view ->
                    view.reset()
                    view.play(loop = Loop.LOOP)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        },
        contentAlignment = Alignment.Center
    ) {
        key(currentCat.assetName) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    try {
                        RiveAnimationView.Builder(ctx)
                            .setRendererType(app.rive.runtime.kotlin.core.RendererType.Canvas)
                            .setFit(Fit.CONTAIN)
                            .setAlignment(RiveAlignment.CENTER)
                            .setAutoplay(true)
                            .setLoop(Loop.LOOP)
                            .build().apply {
                                riveViewRef = this
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                fit = Fit.CONTAIN
                                alignment = RiveAlignment.CENTER

                                var loaded = false
                                try {
                                    setRiveResource(
                                        resId = currentCat.resId,
                                        autoplay = true,
                                        fit = Fit.CONTAIN,
                                        alignment = RiveAlignment.CENTER,
                                        loop = Loop.LOOP
                                    )
                                    loaded = true
                                } catch (e: Throwable) {
                                    // Fallback to asset loading
                                }

                                if (!loaded) {
                                    try {
                                        val bytes = ctx.assets.open(currentCat.assetName).readBytes()
                                        setRiveBytes(
                                            bytes = bytes,
                                            autoplay = true,
                                            fit = Fit.CONTAIN,
                                            alignment = RiveAlignment.CENTER,
                                            loop = Loop.LOOP
                                        )
                                    } catch (ex: Throwable) {
                                        // Ignore
                                    }
                                }
                            }
                    } catch (t: Throwable) {
                        android.view.View(ctx)
                    }
                },
                update = { view ->
                    if (view is RiveAnimationView) {
                        riveViewRef = view
                        try {
                            if (!view.isPlaying) {
                                view.play(loop = Loop.LOOP)
                            }
                        } catch (e: Throwable) {}
                    }
                },
                onRelease = { view ->
                    if (view is RiveAnimationView) {
                        try {
                            view.pause()
                            view.stop()
                        } catch (e: Throwable) {}
                    }
                }
            )
        }
    }
}

@Composable
fun CatMagicRiveView(
    modifier: Modifier = Modifier,
    messageId: String? = null
) {
    CatWelcomeRiveView(modifier = modifier, messageId = messageId)
}
