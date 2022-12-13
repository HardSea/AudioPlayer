package com.hillywave.audioplayer.data.model

data class Audio(
    val data: String,
    val title: String,
    val album: String,
    val artist: String,
    val display_name: String,
    val duration: String,
    val year: String,
    val lastChange: Long,
)
