package com.example.fatecskip.data.remote.dto

import com.google.gson.annotations.SerializedName

data class FatecDataDto(
    @SerializedName("semester_info")
    val semesterInfo: SemesterInfoDto? = null,

    @SerializedName("excuses")
    val excuses: List<String>? = null,

    @SerializedName("curriculums")
    val curriculums: Map<String, List<SubjectDto>>? = null
)

data class SemesterInfoDto(
    @SerializedName("name")
    val name: String? = null,

    @SerializedName("start_date")
    val startDate: String? = null,

    @SerializedName("end_date")
    val endDate: String? = null,

    @SerializedName("holidays")
    val holidays: List<HolidayDto>? = null
)

data class HolidayDto(
    @SerializedName("date")
    val date: String,

    @SerializedName("title")
    val title: String
)

data class SubjectDto(
    @SerializedName("code")
    val code: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("dayOfWeek")
    val dayOfWeek: String,

    @SerializedName("schedule")
    val schedule: String = "07:40 - 11:20",

    @SerializedName("classesPerDay")
    val classesPerDay: Int = 4,

    @SerializedName("maxAbsences")
    val maxAbsences: Int = 20,
    
    @SerializedName("isExtension")
    val isExtension: Boolean = false
)

