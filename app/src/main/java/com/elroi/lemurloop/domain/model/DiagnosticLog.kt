package com.elroi.lemurloop.domain.model

data class DiagnosticLog(
    val id: Long,
    val timestamp: Long,
    val tag: String,
    val message: String,
    val level: String
)

