package com.example.fatecskip

import com.example.fatecskip.worker.AlarmScheduler
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmSchedulerTest {

    @Test
    fun extractStartTimeForDayReturnsTheMatchingWeekdaySchedule() {
        val classTime = "Seg 11:20 - 13:00 | Qua 07:40 - 09:20"

        assertEquals(11 to 20, AlarmScheduler.extractStartTimeForDay(classTime, 1))
        assertEquals(7 to 40, AlarmScheduler.extractStartTimeForDay(classTime, 3))
    }

    @Test
    fun extractStartTimeForDayUsesSingleScheduleForAnyDay() {
        assertEquals(7 to 40, AlarmScheduler.extractStartTimeForDay("07:40 - 11:20", 3))
    }
}
