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

package com.focusbyrj.app.data

import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val appRestrictionDao: AppRestrictionDao,
    private val scheduleDao: ScheduleDao
) {
    val allRestrictions: Flow<List<AppRestriction>> = appRestrictionDao.getAllRestrictions()
    val allSchedules: Flow<List<FocusSchedule>> = scheduleDao.getAllSchedules()

    suspend fun getRestriction(packageName: String): AppRestriction? {
        return appRestrictionDao.getRestriction(packageName)
    }

    suspend fun toggleRestriction(app: AppRestriction) {
        appRestrictionDao.insertRestriction(app.copy(isRestricted = !app.isRestricted))
    }
    
    suspend fun updateMode(app: AppRestriction, mode: String) {
        appRestrictionDao.insertRestriction(app.copy(mode = mode))
    }
    
    suspend fun saveApp(app: AppRestriction) {
         appRestrictionDao.insertRestriction(app)
    }

    suspend fun deleteRestriction(app: AppRestriction) {
        appRestrictionDao.deleteRestriction(app.packageName)
    }

    suspend fun insertSchedule(schedule: FocusSchedule) {
        scheduleDao.insertSchedule(schedule)
    }

    suspend fun deleteSchedule(schedule: FocusSchedule) {
        scheduleDao.deleteSchedule(schedule)
    }
}
