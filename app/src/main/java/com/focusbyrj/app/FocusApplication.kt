/*
 * Copyright (C) 2024-2026 Focus by Rj
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.focusbyrj.app

import android.app.Application
import androidx.room.Room
import com.focusbyrj.app.data.AppRepository
import com.focusbyrj.app.data.FocusDatabase
import com.focusbyrj.app.data.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FocusApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        com.focusbyrj.app.util.AppThemeManager.init(this)
        com.focusbyrj.app.util.FocusStatsManager.init(this)
        com.focusbyrj.app.util.FocusEconomyManager.init(this)
        com.focusbyrj.app.util.AptitudeManager.init(this)
        com.focusbyrj.app.util.CustomCategoryManager.init(this)
        com.focusbyrj.app.util.BubbleChatManager.init(this)
        com.focusbyrj.app.util.AppIconManager.init(this)

        // Warm up Ayva knowledge base in background IO thread for instant 0ms responses
        CoroutineScope(Dispatchers.IO).launch {
            com.focusbyrj.app.util.AyvaTalkEngine.warmUp(this@FocusApplication)
        }
    }

    val database by lazy { 
        Room.databaseBuilder(
            this,
            FocusDatabase::class.java,
            "focus_database"
        )
        .addMigrations(
            FocusDatabase.MIGRATION_1_2,
            FocusDatabase.MIGRATION_2_3,
            FocusDatabase.MIGRATION_3_4,
            FocusDatabase.MIGRATION_1_4,
            FocusDatabase.MIGRATION_2_4,
            FocusDatabase.MIGRATION_4_5,
            FocusDatabase.MIGRATION_1_5,
            FocusDatabase.MIGRATION_5_6,
            FocusDatabase.MIGRATION_1_6,
            FocusDatabase.MIGRATION_6_7,
            FocusDatabase.MIGRATION_1_7
        )
        .fallbackToDestructiveMigrationOnDowngrade()
        .build() 
    }
    
    val repository by lazy { AppRepository(database.appRestrictionDao(), database.scheduleDao()) }
    val taskRepository by lazy { TaskRepository(database.taskDao()) }
}
