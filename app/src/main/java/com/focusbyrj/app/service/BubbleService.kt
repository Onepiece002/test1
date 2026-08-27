package com.focusbyrj.app.service

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.focusbyrj.app.R
import com.focusbyrj.app.ui.screens.BubbleChatActivity
import kotlin.math.abs

class BubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    private var lastX = 0
    private var lastY = 300

    private val hideHandler = Handler(Looper.getMainLooper())
    private var isPeeking = false
    private var hideRunnable = Runnable { peekBubble() }

    companion object {
        const val CHANNEL_ID = "bubble_service_channel"
        const val NOTIFICATION_ID = 1002
        var isChatOpen = false
        
        fun startIfEnabled(context: Context) {
            val prefs = context.getSharedPreferences("bubble_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("bubble_enabled", false) && android.provider.Settings.canDrawOverlays(context)) {
                val intent = Intent(context, BubbleService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
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
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        val filter = IntentFilter().apply {
            addAction("com.focusbyrj.app.CHAT_CLOSED")
            addAction("com.focusbyrj.app.CHAT_OPENED")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(receiver, filter)
        }
        
        setupBubble()
        resetHideTimer()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupBubble() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val size = (60 * resources.displayMetrics.density).toInt()
        
        val imageView = ImageView(this).apply {
            setImageResource(R.drawable.ic_bubble_launcher_icon)
            scaleType = ImageView.ScaleType.FIT_CENTER
            layoutParams = WindowManager.LayoutParams(size, size)
            elevation = 10f
        }
        
        bubbleView = imageView

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
                    if (isPeeking) {
                        unpeekBubble(animate = true)
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
                        layoutParams!!.x = initialX + dx.toInt()
                        layoutParams!!.y = initialY + dy.toInt()
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

        windowManager.addView(bubbleView, layoutParams)
    }

    private fun peekBubble() {
        if (isChatOpen || isPeeking) return
        val prefs = getSharedPreferences("bubble_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("auto_hide_enabled", false)) return

        isPeeking = true
        val size = (60 * resources.displayMetrics.density).toInt()
        val screenWidth = resources.displayMetrics.widthPixels
        
        val targetX = if (layoutParams!!.x < screenWidth / 2) {
            -(size / 2)
        } else {
            screenWidth - (size / 2)
        }
        
        val animator = ValueAnimator.ofInt(layoutParams!!.x, targetX)
        animator.duration = 300
        animator.addUpdateListener { anim ->
            layoutParams!!.x = anim.animatedValue as Int
            windowManager.updateViewLayout(bubbleView, layoutParams)
        }
        animator.start()
        
        bubbleView?.animate()?.alpha(0.5f)?.setDuration(300)?.start()
    }

    private fun unpeekBubble(animate: Boolean) {
        if (!isPeeking) return
        isPeeking = false
        bubbleView?.animate()?.alpha(1.0f)?.setDuration(300)?.start()
        
        val screenWidth = resources.displayMetrics.widthPixels
        val targetX = if (layoutParams!!.x < screenWidth / 2) 0 else screenWidth
        
        if (animate) {
            val animator = ValueAnimator.ofInt(layoutParams!!.x, targetX)
            animator.duration = 300
            animator.addUpdateListener { anim ->
                layoutParams!!.x = anim.animatedValue as Int
                windowManager.updateViewLayout(bubbleView, layoutParams)
            }
            animator.start()
        } else {
            layoutParams!!.x = targetX
            windowManager.updateViewLayout(bubbleView, layoutParams)
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
        val centerX = screenWidth / 2

        val targetX = if (layoutParams!!.x < centerX) 0 else screenWidth
        
        val animator = ValueAnimator.ofInt(layoutParams!!.x, targetX)
        animator.duration = 200
        animator.addUpdateListener { anim ->
            layoutParams!!.x = anim.animatedValue as Int
            windowManager.updateViewLayout(bubbleView, layoutParams)
        }
        animator.start()
    }

    private fun openChatWindow() {
        val intent = Intent(this, BubbleChatActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        hideHandler.removeCallbacks(hideRunnable)
        unregisterReceiver(receiver)
        bubbleView?.let {
            windowManager.removeView(it)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Chat Bubble Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the chat bubble active"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focus Assistant Active")
            .setContentText("Chat bubble is floating")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
