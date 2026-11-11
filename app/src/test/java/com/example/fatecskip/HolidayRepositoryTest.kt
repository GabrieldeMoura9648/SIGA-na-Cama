package com.example.fatecskip

import com.example.fatecskip.data.remote.dto.FatecDataDto
import com.example.fatecskip.data.remote.HolidayApiService
import com.example.fatecskip.data.remote.dto.HolidayDto
import com.example.fatecskip.data.repository.HolidayRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HolidayRepositoryTest {

    @Test
    fun `recognizes local state and city holidays without network`() = runBlocking {
        val service = FakeHolidayApiService()
        val repository = HolidayRepository(service)

        assertTrue(repository.isHoliday(LocalDate.of(2030, 7, 9)))
        assertTrue(repository.isHoliday(LocalDate.of(2030, 12, 8)))
        assertEquals(0, service.requestedYears.size)
    }

    @Test
    fun `recognizes and caches national holidays from BrasilAPI`() = runBlocking {
        val service = FakeHolidayApiService(
            holidays = listOf(HolidayDto("2030-01-01", "Confraternização Universal", "national"))
        )
        val repository = HolidayRepository(service)

        assertTrue(repository.isHoliday(LocalDate.of(2030, 1, 1)))
        assertFalse(repository.isHoliday(LocalDate.of(2030, 1, 2)))
        assertEquals(listOf(2030), service.requestedYears)
    }

    @Test
    fun `keeps compatibility with academic API holiday title`() {
        val response = Gson().fromJson(
            """{"semester_info":{"holidays":[{"date":"2030-07-09","title":"Feriado legado"}]}}""",
            FatecDataDto::class.java
        )

        assertEquals("Feriado legado", response.semesterInfo?.holidays?.single()?.name)
    }

    @Test
    fun `maps BrasilAPI holiday name and type fields`() {
        val holidays = Gson().fromJson<List<HolidayDto>>(
            """[{"date":"2030-01-01","name":"Ano Novo","type":"national"}]""",
            object : TypeToken<List<HolidayDto>>() {}.type
        )

        assertEquals("Ano Novo", holidays.single().name)
        assertEquals("national", holidays.single().type)
    }

    private class FakeHolidayApiService(
        private val holidays: List<HolidayDto> = emptyList()
    ) : HolidayApiService {
        val requestedYears = mutableListOf<Int>()

        override suspend fun getNationalHolidays(year: Int): List<HolidayDto> {
            requestedYears += year
            return holidays
        }
    }
}
