package com.focusbyrj.app.service

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.graphics.drawable.GradientDrawable
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.focusbyrj.app.R
import com.focusbyrj.app.ui.screens.BubbleChatActivity
import com.focusbyrj.app.util.BubbleChatManager
import kotlin.math.abs

class BubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var displayManager: DisplayManager
    private var bubbleView: View? = null
    private var badgeView: TextView? = null
    private var glowRingView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var lastX = 0
    private var lastY = 300

    private val hideHandler = Handler(Looper.getMainLooper())
    private var isPeeking = false
    private var hideRunnable = Runnable { peekBubble() }

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {}
        override fun onDisplayChanged(displayId: Int) {
            updateLandscapeVisibility()
        }
    }

    companion object {
        var isChatOpen = false
        const val ACTION_SETTINGS_CHANGED = "com.focusbyrj.app.BUBBLE_SETTINGS_CHANGED"
        
        fun startIfEnabled(context: Context) {
            val prefs = context.getSharedPreferences("bubble_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("bubble_enabled", false) && android.provider.Settings.canDrawOverlays(context)) {
                val intent = Intent(context, BubbleService::class.java)
                context.startService(intent)
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.focusbyrj.app.CHAT_CLOSED" -> {
                    isChatOpen = false
                    layoutParams?.x = lastX
                    layoutParams?.y = lastY
                    windowManager.updateViewLayout(bubbleView, layoutParams)
                    resetHideTimer()
                    updateBadgeCount()
                }
                "com.focusbyrj.app.CHAT_OPENED" -> {
                    isChatOpen = true
                    hideHandler.removeCallbacks(hideRunnable)
                    unpeekBubble(animate = false)
                    lastX = layoutParams?.x ?: 0
                    lastY = layoutParams?.y ?: 0
                    layoutParams?.x = (16 * resources.displayMetrics.density).toInt()
                    layoutParams?.y = (48 * resources.displayMetrics.density).toInt()
                    windowManager.updateViewLayout(bubbleView, layoutParams)
                    updateBadgeCount(0)
                }
                BubbleChatManager.ACTION_UNREAD_COUNT_CHANGED -> {
                    updateBadgeCount()
                }
                ACTION_SETTINGS_CHANGED -> {
                    applyBubbleStyleSettings()
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    override fun onCreate() {
        super.onCreate()
        
        val filter = IntentFilter().apply {
            addAction("com.focusbyrj.app.CHAT_CLOSED")
            addAction("com.focusbyrj.app.CHAT_OPENED")
            addAction(BubbleChatManager.ACTION_UNREAD_COUNT_CHANGED)
            addAction(ACTION_SETTINGS_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
        
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        displayManager.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))
        
        setupBubble()
        resetHideTimer()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateLandscapeVisibility()
    }

    private fun isLandscapeMode(): Boolean {
        val config = resources.configuration
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) return true
        val metrics = resources.displayMetrics
        if (metrics.widthPixels > metrics.heightPixels) return true
        return false
    }

    private fun updateLandscapeVisibility() {
        val prefs = getSharedPreferences("bubble_prefs", Context.MODE_PRIVATE)
        val hideInLandscape = prefs.getBoolean("hide_in_landscape", true)
        
        if (hideInLandscape && isLandscapeMode()) {
            hideHandler.removeCallbacks(hideRunnable)
            bubbleView?.visibility = View.GONE
            if (isChatOpen) {
                sendBroadcast(Intent("com.focusbyrj.app.CLOSE_CHAT"))
            }
        } else {
            val isEnabled = prefs.getBoolean("bubble_enabled", false)
            if (isEnabled) {
                bubbleView?.visibility = View.VISIBLE
                snapToEdge()
                resetHideTimer()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupBubble() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val size = (60 * resources.displayMetrics.density).toInt()
        
        val imageView = ImageView(this).apply {
            setImageResource(R.drawable.ic_bubble_launcher_icon)
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = FrameLayout.LayoutParams(size, size)
            elevation = 10f
        }

        // Subtle accent edge ring for peek & hide state
        val glowRing = View(this).apply {
            val prefs = getSharedPreferences("bubble_prefs", Context.MODE_PRIVATE)
            val accentColorStr = prefs.getString("bubble_accent_color", "#4ADE80") ?: "#4ADE80"
            val glowIntensity = prefs.getInt("bubble_glow_intensity", 65) / 100f
            val strokeWidthDp = (1.5f + (glowIntensity * 1.5f)).coerceIn(1.2f, 3.0f)
            
            val glowDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(android.graphics.Color.TRANSPARENT)
                try {
                    setStroke((strokeWidthDp * resources.displayMetrics.density).toInt(), android.graphics.Color.parseColor(accentColorStr))
                } catch (_: Exception) {
                    setStroke((1.5f * resources.displayMetrics.density).toInt(), android.graphics.Color.parseColor("#4ADE80"))
                }
            }
            background = glowDrawable
            layoutParams = FrameLayout.LayoutParams(size, size)
            alpha = 0f
            elevation = 11f
        }
        glowRingView = glowRing

        val badgeSize = (22 * resources.displayMetrics.density).toInt()
        val badgeBackground = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(android.graphics.Color.parseColor("#E53935")) // Red badge
            setStroke((1.5f * resources.displayMetrics.density).toInt(), android.graphics.Color.WHITE)
        }

        val badge = TextView(this).apply {
            background = badgeBackground
            setTextColor(android.graphics.Color.WHITE)
            textSize = 11f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            includeFontPadding = false
            setPadding((4 * resources.displayMetrics.density).toInt(), 0, (4 * resources.displayMetrics.density).toInt(), 0)
            minWidth = badgeSize
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                badgeSize
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                leftMargin = 0
                topMargin = 0
            }
            elevation = 16f
            visibility = View.GONE
        }
        badgeView = badge

        val container = FrameLayout(this).apply {
            clipChildren = false
            clipToPadding = false
            addView(imageView)
            addView(glowRing)
            addView(badge)
        }
        
        bubbleView = container

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            size,
            size,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 300
        }

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isMoved = false
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop

        bubbleView?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    hideHandler.removeCallbacks(hideRunnable)
                    bubbleView?.animate()?.cancel()
                    if (isPeeking) {
                        unpeekBubble(animate = false)
                    }
                    initialX = layoutParams!!.x
                    initialY = layoutParams!!.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isMoved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    if (isChatOpen) return@setOnTouchListener true // don't drag if chat open
                    
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (abs(dx) > touchSlop || abs(dy) > touchSlop) {
                        isMoved = true
                        val displayMetrics = resources.displayMetrics
                        val screenWidth = displayMetrics.widthPixels
                        val screenHeight = displayMetrics.heightPixels
                        val bubbleSize = (60 * displayMetrics.density).toInt()

                        layoutParams!!.x = (initialX + dx.toInt()).coerceIn(0, screenWidth - bubbleSize)
                        layoutParams!!.y = (initialY + dy.toInt()).coerceIn(0, screenHeight - bubbleSize)
                        windowManager.updateViewLayout(bubbleView, layoutParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isMoved) {
                        view.performClick()
                        if (isChatOpen) {
                            sendBroadcast(Intent("com.focusbyrj.app.CLOSE_CHAT"))
                        } else {
                            openChatWindow()
                        }
                    } else if (!isChatOpen) {
                        snapToEdge()
                    }
                    resetHideTimer()
                    true
                }
                else -> false
            }
        }

        try {
            windowManager.addView(bubbleView, layoutParams)
            updateLandscapeVisibility()
            updateBadgeCount()
        } catch (e: Exception) {
            android.util.Log.e("BubbleService", "Error adding bubble view", e)
        }
    }

    private fun updateBadgeCount(count: Int = BubbleChatManager.getUnreadCount(this)) {
        val bv = badgeView ?: return
        if (count > 0) {
            bv.text = if (count > 99) "99+" else count.toString()
            bv.visibility = View.VISIBLE
            if (isPeeking) {
                unpeekBubble(animate = true)
            }
        } else {
            bv.visibility = View.GONE
        }
    }

    private fun applyBubbleStyleSettings() {
        val prefs = getSharedPreferences("bubble_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("bubble_enabled", false)
        if (!isEnabled) {
            bubbleView?.visibility = View.GONE
            return
        } else {
            bubbleView?.visibility = View.VISIBLE
        }

        val accentColorStr = prefs.getString("bubble_accent_color", "#4ADE80") ?: "#4ADE80"
        val glowIntensity = (prefs.getInt("bubble_glow_intensity", 65) / 100f).coerceIn(0f, 1f)
        val hiddenOpacity = (prefs.getInt("bubble_hidden_opacity", 85) / 100f).coerceIn(0.1f, 1f)
        val hiddenAmountRatio = (prefs.getInt("bubble_hidden_amount", 60) / 100f).coerceIn(0.2f, 0.9f)
        val strokeWidthDp = (1.5f + (glowIntensity * 1.5f)).coerceIn(1.2f, 3.0f)

        glowRingView?.let { gView ->
            val glowDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(android.graphics.Color.TRANSPARENT)
                try {
                    setStroke((strokeWidthDp * resources.displayMetrics.density).toInt(), android.graphics.Color.parseColor(accentColorStr))
                } catch (_: Exception) {
                    setStroke((1.5f * resources.displayMetrics.density).toInt(), android.graphics.Color.parseColor("#4ADE80"))
                }
            }
            gView.background = glowDrawable
        }

        if (isPeeking) {
            val displayMetrics = resources.displayMetrics
            val size = (60 * displayMetrics.density).toInt()
            val screenWidth = displayMetrics.widthPixels
            val isLeft = (layoutParams?.x ?: 0) < screenWidth / 2
            val hideOffset = size * hiddenAmountRatio
            val targetTranslation = if (isLeft) -hideOffset else hideOffset

            glowRingView?.animate()?.cancel()
            glowRingView?.alpha = glowIntensity
            bubbleView?.animate()?.cancel()
            bubbleView?.translationX = targetTranslation
            bubbleView?.alpha = hiddenOpacity
        }

        resetHideTimer()
    }

    private fun peekBubble() {
        if (isChatOpen || isPeeking) return
        val prefs = getSharedPreferences("bubble_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("auto_hide_enabled", false)) return

        isPeeking = true
        val displayMetrics = resources.displayMetrics
        val size = (60 * displayMetrics.density).toInt()
        val screenWidth = displayMetrics.widthPixels
        
        val hiddenAmountRatio = (prefs.getInt("bubble_hidden_amount", 60) / 100f).coerceIn(0.2f, 0.9f)
        val hiddenOpacity = (prefs.getInt("bubble_hidden_opacity", 85) / 100f).coerceIn(0.1f, 1f)
        val glowIntensity = (prefs.getInt("bubble_glow_intensity", 65) / 100f).coerceIn(0f, 1f)
        val accentColorStr = prefs.getString("bubble_accent_color", "#4ADE80") ?: "#4ADE80"
        val strokeWidthDp = (1.5f + (glowIntensity * 1.5f)).coerceIn(1.2f, 3.0f)

        // Ensure stroke and color are up to date
        glowRingView?.let { gView ->
            val glowDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(android.graphics.Color.TRANSPARENT)
                try {
                    setStroke((strokeWidthDp * resources.displayMetrics.density).toInt(), android.graphics.Color.parseColor(accentColorStr))
                } catch (_: Exception) {
                    setStroke((1.5f * resources.displayMetrics.density).toInt(), android.graphics.Color.parseColor("#4ADE80"))
                }
            }
            gView.background = glowDrawable
        }

        // Ensure bubble is snapped to edge first
        val isLeft = (layoutParams?.x ?: 0) < screenWidth / 2
        val targetX = if (isLeft) 0 else (screenWidth - size)
        layoutParams?.x = targetX
        try {
            windowManager.updateViewLayout(bubbleView, layoutParams)
        } catch (_: Exception) {}

        val hideOffset = size * hiddenAmountRatio
        val targetTranslation = if (isLeft) -hideOffset else hideOffset
        
        glowRingView?.animate()
            ?.alpha(glowIntensity)
            ?.setDuration(300)
            ?.start()

        bubbleView?.animate()
            ?.translationX(targetTranslation)
            ?.alpha(hiddenOpacity)
            ?.setDuration(300)
            ?.start()
    }

    private fun unpeekBubble(animate: Boolean) {
        if (!isPeeking) return
        isPeeking = false
        if (animate) {
            glowRingView?.animate()
                ?.alpha(0f)
                ?.setDuration(200)
                ?.start()
            bubbleView?.animate()
                ?.translationX(0f)
                ?.alpha(1.0f)
                ?.setDuration(250)
                ?.start()
        } else {
            glowRingView?.animate()?.cancel()
            glowRingView?.alpha = 0f
            bubbleView?.animate()?.cancel()
            bubbleView?.translationX = 0f
            bubbleView?.alpha = 1.0f
        }
    }

    private fun resetHideTimer() {
        hideHandler.removeCallbacks(hideRunnable)
        if (isChatOpen) return
        val prefs = getSharedPreferences("bubble_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("auto_hide_enabled", false)) {
            val durationSecs = prefs.getInt("auto_hide_duration_sec", 3)
            hideHandler.postDelayed(hideRunnable, durationSecs * 1000L)
        }
    }

    private fun snapToEdge() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val size = (60 * displayMetrics.density).toInt()
        val centerX = screenWidth / 2

        val targetX = if (layoutParams!!.x < centerX) 0 else (screenWidth - size)
        glowRingView?.animate()?.cancel()
        glowRingView?.alpha = 0f
        bubbleView?.animate()?.cancel()
        bubbleView?.translationX = 0f
        bubbleView?.alpha = 1.0f
        
        val animator = ValueAnimator.ofInt(layoutParams!!.x, targetX)
        animator.duration = 200
        animator.addUpdateListener { anim ->
            layoutParams!!.x = anim.animatedValue as Int
            windowManager.updateViewLayout(bubbleView, layoutParams)
        }
        animator.start()
    }

    private fun openChatWindow() {
        BubbleChatManager.clearUnread(this)
        updateBadgeCount(0)
        val intent = Intent(this, BubbleChatActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        hideHandler.removeCallbacks(hideRunnable)
        unregisterReceiver(receiver)
        try {
            displayManager.unregisterDisplayListener(displayListener)
        } catch (_: Exception) {}
        bubbleView?.let {
            windowManager.removeView(it)
        }
    }
}
