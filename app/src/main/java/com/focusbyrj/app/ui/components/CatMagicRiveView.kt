package com.focusbyrj.app.ui.components

import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
    val assetName: String,
    val displayName: String
)

val WELCOME_CAT_RIVE_ASSETS = listOf(
    RiveCatAsset(assetName = "cat_magic.riv", displayName = "Magic Cat"),
    RiveCatAsset(assetName = "cat_gold_fish.riv", displayName = "Goldfish Aquarium Cat"),
    RiveCatAsset(assetName = "cat_googlyeyes.riv", displayName = "Googly Eyes Cat"),
    RiveCatAsset(assetName = "cat_shiftyhead.riv", displayName = "Shifty Head Cat"),
    RiveCatAsset(assetName = "cat_blunng.riv", displayName = "Blunng Cat"),
    RiveCatAsset(assetName = "cat_wildfairy.riv", displayName = "Wild Fairy Cat")
)

@Composable
fun CatWelcomeRiveView(
    modifier: Modifier = Modifier,
    messageId: String? = null
) {
    val initialIndex = remember(messageId) {
        if (WELCOME_CAT_RIVE_ASSETS.isNotEmpty()) {
            if (messageId != null) {
                kotlin.math.abs(messageId.hashCode()) % WELCOME_CAT_RIVE_ASSETS.size
            } else {
                kotlin.random.Random.nextInt(WELCOME_CAT_RIVE_ASSETS.size)
            }
        } else 0
    }
    var currentCatIndex by remember(messageId) { mutableIntStateOf(initialIndex) }
    val currentCat = WELCOME_CAT_RIVE_ASSETS.getOrElse(currentCatIndex) { WELCOME_CAT_RIVE_ASSETS[0] }

    var riveViewRef: RiveAnimationView? = null

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        key(currentCat.assetName) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    try {
                        try {
                            app.rive.runtime.kotlin.core.Rive.init(ctx.applicationContext)
                        } catch (_: Throwable) {}

                        RiveAnimationView(ctx).apply {
                            riveViewRef = this
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            fit = Fit.CONTAIN
                            alignment = RiveAlignment.CENTER
                            isClickable = true
                            isFocusable = true

                            var loaded = false
                            try {
                                val bytes = ctx.assets.open(currentCat.assetName).readBytes()
                                setRiveBytes(
                                    bytes = bytes,
                                    autoplay = true,
                                    fit = Fit.CONTAIN,
                                    alignment = RiveAlignment.CENTER
                                )
                                play()
                                loaded = true
                            } catch (ex: Throwable) {
                                ex.printStackTrace()
                            }

                            if (!loaded) {
                                val fallbackAssets = listOf("cat_magic.riv", "cat_gold_fish.riv", "cat_googlyeyes.riv", "cat_shiftyhead.riv", "cat_blunng.riv", "cat_wildfairy.riv")
                                for (fallback in fallbackAssets) {
                                    try {
                                        val bytes = ctx.assets.open(fallback).readBytes()
                                        setRiveBytes(
                                            bytes = bytes,
                                            autoplay = true,
                                            fit = Fit.CONTAIN,
                                            alignment = RiveAlignment.CENTER
                                        )
                                        play()
                                        break
                                    } catch (_: Throwable) {}
                                }
                            }

                            setOnClickListener {
                                try {
                                    val triggers = listOf(
                                        "Tap", "tap", "Trigger", "trigger", "Click", "click",
                                        "Press", "press", "Hit", "hit", "Action", "action",
                                        "Meow", "meow", "Jump", "jump", "Blink", "blink",
                                        "Happy", "happy", "interact", "Interact", "Touch", "touch"
                                    )
                                    val smNames = listOf("State Machine 1", "StateMachine", "Designer State Machine", "SM", "State Machine", "Motion", "Interactive")
                                    var fired = false
                                    for (sm in smNames) {
                                        for (trig in triggers) {
                                            try {
                                                fireState(sm, trig)
                                                fired = true
                                            } catch (_: Throwable) {}
                                        }
                                    }
                                    if (!fired && !isPlaying) {
                                        play()
                                    }
                                } catch (e: Throwable) {
                                    try {
                                        if (!isPlaying) play()
                                    } catch (_: Throwable) {}
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
                                view.play()
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

        // Companion tag & switch badge
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                )
                .clickable {
                    currentCatIndex = (currentCatIndex + 1) % WELCOME_CAT_RIVE_ASSETS.size
                }
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "✨ ${currentCat.displayName}",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "• Tap to switch",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
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
