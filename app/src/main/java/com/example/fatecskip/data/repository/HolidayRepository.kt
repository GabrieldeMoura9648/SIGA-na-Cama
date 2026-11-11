package com.example.fatecskip.data.repository

import android.util.Log
import com.example.fatecskip.data.remote.HolidayApiService
import com.example.fatecskip.data.remote.dto.HolidayDto
import java.time.LocalDate
import java.time.MonthDay
import java.util.concurrent.ConcurrentHashMap

class HolidayRepository(
    private val apiService: HolidayApiService
) {
    private val nationalHolidaysByYear = ConcurrentHashMap<Int, Set<LocalDate>>()

    suspend fun isHoliday(date: LocalDate): Boolean {
        if (isLocalHoliday(date)) return true

        val nationalHolidays = nationalHolidaysByYear[date.year] ?: try {
            apiService.getNationalHolidays(date.year)
                .map { LocalDate.parse(it.date) }
                .toSet()
                .also { nationalHolidaysByYear[date.year] = it }
        } catch (exception: Exception) {
            Log.e(TAG, "Não foi possível consultar os feriados nacionais de ${date.year}.", exception)
            throw exception
        }

        return date in nationalHolidays
    }

    private fun isLocalHoliday(date: LocalDate): Boolean =
        LOCAL_HOLIDAYS.any { (monthDay, _) -> monthDay == MonthDay.from(date) }

    companion object {
        private const val TAG = "HolidayRepository"

        private val LOCAL_HOLIDAYS = listOf(
            MonthDay.of(7, 9) to "Revolução Constitucionalista (São Paulo)",
            MonthDay.of(12, 8) to "Aniversário de Diadema"
        )
    }
}
