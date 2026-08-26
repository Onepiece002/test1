package com.focusbyrj.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.widget.RemoteViews
import com.focusbyrj.app.FocusApplication
import com.focusbyrj.app.MainActivity
import com.focusbyrj.app.R
import com.focusbyrj.app.data.RecurrencePattern
import com.focusbyrj.app.data.Task
import com.focusbyrj.app.util.TaskReminderHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class TodoWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_SET_TAB = "com.focusbyrj.app.widget.ACTION_SET_TAB"
        const val ACTION_TOGGLE_TASK = "com.focusbyrj.app.widget.ACTION_TOGGLE_TASK"
        const val ACTION_ADD_TASK = "com.focusbyrj.app.widget.ACTION_ADD_TASK"
        const val ACTION_REFRESH = "com.focusbyrj.app.widget.ACTION_REFRESH"
        const val ACTION_OPEN_APP = "com.focusbyrj.app.widget.ACTION_OPEN_APP"

        const val EXTRA_TAB_INDEX = "extra_tab_index"
        const val EXTRA_TASK_ID = "extra_task_id"

        private const val PREFS_NAME = "todo_widget_prefs"
        private const val KEY_TAB_PREFIX = "tab_index_"

        fun getSelectedTab(context: Context, appWidgetId: Int): Int {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            return prefs.getInt(KEY_TAB_PREFIX + appWidgetId, 0)
        }

        fun setSelectedTab(context: Context, appWidgetId: Int, tabIndex: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putInt(KEY_TAB_PREFIX + appWidgetId, tabIndex).apply()
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, TodoWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_list_view)
                for (widgetId in appWidgetIds) {
                    updateWidget(context, appWidgetManager, widgetId)
                }
            }
        }

        fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val selectedTab = getSelectedTab(context, appWidgetId)
            val views = RemoteViews(context.packageName, R.layout.widget_todo_layout)

            // Setup Tab Click Intents
            for (tabIndex in 0..2) {
                val tabIntent = Intent(context, TodoWidgetProvider::class.java).apply {
                    action = ACTION_SET_TAB
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    putExtra(EXTRA_TAB_INDEX, tabIndex)
                    data = Uri.parse("widget://$appWidgetId/tab/$tabIndex")
                }
                val tabPendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 10 + tabIndex,
                    tabIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                when (tabIndex) {
                    0 -> {
                        views.setOnClickPendingIntent(R.id.widget_tab_today, tabPendingIntent)
                        if (selectedTab == 0) {
                            views.setInt(R.id.widget_tab_today, "setBackgroundResource", R.drawable.widget_tab_active)
                            views.setTextColor(R.id.widget_tab_today, Color.parseColor("#121516"))
                        } else {
                            views.setInt(R.id.widget_tab_today, "setBackgroundResource", R.drawable.widget_tab_inactive)
                            views.setTextColor(R.id.widget_tab_today, Color.parseColor("#A0ABA4"))
                        }
                    }
                    1 -> {
                        views.setOnClickPendingIntent(R.id.widget_tab_upcoming, tabPendingIntent)
                        if (selectedTab == 1) {
                            views.setInt(R.id.widget_tab_upcoming, "setBackgroundResource", R.drawable.widget_tab_active)
                            views.setTextColor(R.id.widget_tab_upcoming, Color.parseColor("#121516"))
                        } else {
                            views.setInt(R.id.widget_tab_upcoming, "setBackgroundResource", R.drawable.widget_tab_inactive)
                            views.setTextColor(R.id.widget_tab_upcoming, Color.parseColor("#A0ABA4"))
                        }
                    }
                    2 -> {
                        views.setOnClickPendingIntent(R.id.widget_tab_all, tabPendingIntent)
                        if (selectedTab == 2) {
                            views.setInt(R.id.widget_tab_all, "setBackgroundResource", R.drawable.widget_tab_active)
                            views.setTextColor(R.id.widget_tab_all, Color.parseColor("#121516"))
                        } else {
                            views.setInt(R.id.widget_tab_all, "setBackgroundResource", R.drawable.widget_tab_inactive)
                            views.setTextColor(R.id.widget_tab_all, Color.parseColor("#A0ABA4"))
                        }
                    }
                }
            }

            // Quick Add '+' Button Intent (Opens floating dialog directly over home screen)
            val addIntent = Intent(context, com.focusbyrj.app.ui.screens.QuickAddTaskActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val addPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 100 + 1,
                addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_add, addPendingIntent)

            // Header Title Click (Opens App Todos)
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "todos")
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId * 100 + 2,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_header_title_layout, openAppPendingIntent)

            // Refresh Button Intent
            val refreshIntent = Intent(context, TodoWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId * 100 + 3,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPendingIntent)

            // ListView Adapter Setup
            val serviceIntent = Intent(context, TodoWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(EXTRA_TAB_INDEX, selectedTab)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list_view, serviceIntent)
            views.setEmptyView(R.id.widget_list_view, R.id.widget_empty_view)

            // Template PendingIntent for List Item interactions
            val listClickIntent = Intent(context, TodoWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_TASK
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val listClickPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId * 1000,
                listClickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_list_view, listClickPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)

            // Calculate task count asynchronously
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val app = context.applicationContext as FocusApplication
                    val allTasks = app.database.taskDao().getAllTasks().first()
                    
                    val now = Calendar.getInstance()
                    val todayStart = now.apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    val todayEnd = todayStart + 86400000L

                    val uncompleted = allTasks.filter { !it.isCompleted }

                    val count = when (selectedTab) {
                        0 -> uncompleted.count {
                            (it.dueDate != null && it.dueDate <= todayEnd) ||
                            (it.dueDate == null && it.type == com.focusbyrj.app.data.TaskType.TASK)
                        }
                        1 -> uncompleted.count {
                            it.type == com.focusbyrj.app.data.TaskType.TASK && it.dueDate != null && it.dueDate > todayEnd
                        }
                        2 -> uncompleted.size
                        else -> uncompleted.size
                    }
                    
                    views.setTextViewText(R.id.widget_task_count, count.toString())
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetManager = AppWidgetManager.getInstance(context)

        when (intent.action) {
            ACTION_SET_TAB -> {
                val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                val tabIndex = intent.getIntExtra(EXTRA_TAB_INDEX, 0)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    setSelectedTab(context, appWidgetId, tabIndex)
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view)
                    updateWidget(context, appWidgetManager, appWidgetId)
                }
            }

            ACTION_REFRESH -> {
                val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view)
                    updateWidget(context, appWidgetManager, appWidgetId)
                } else {
                    updateAllWidgets(context)
                }
            }

            ACTION_TOGGLE_TASK -> {
                val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
                val actionType = intent.getStringExtra("action_type") ?: "toggle"

                if (actionType == "open_app") {
                    val mainIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra("navigate_to", "todos")
                    }
                    context.startActivity(mainIntent)
                    return
                }

                if (taskId != -1L) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val app = context.applicationContext as FocusApplication
                            val taskDao = app.database.taskDao()
                            val task = taskDao.getTaskById(taskId)
                            if (task != null) {
                                val updated = task.copy(isCompleted = !task.isCompleted)
                                taskDao.updateTask(updated)

                                if (updated.isCompleted) {
                                    TaskReminderHelper.cancelReminder(context, updated)
                                    if (updated.recurrence != RecurrencePattern.NONE) {
                                        val nextTask = TaskReminderHelper.generateNextRecurringTask(updated)
                                        val newId = taskDao.insertTask(nextTask)
                                        TaskReminderHelper.scheduleReminder(context, nextTask.copy(id = newId))
                                    }
                                } else {
                                    TaskReminderHelper.scheduleReminder(context, updated)
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        updateAllWidgets(context)
                    }
                }
            }
        }
    }
}
