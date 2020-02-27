package com.akhbulatov.wordkeeper.domain.common.models

data class Word(
    val id: Long,
    val name: String,
    val translation: String,
    val category: String
)