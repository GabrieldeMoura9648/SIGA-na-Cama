package com.example.fatecskip.data.remote.dto

data class MyInstantsResponseDto(
    val status: String?,
    val message: String?,
    val data: List<MyInstantSoundDto>?
)

data class MyInstantSoundDto(
    val id: String,
    val title: String,
    val mp3: String
)
