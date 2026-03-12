package com.elroi.lemurloop.domain.repository

interface AppDataRepository {
    suspend fun wipeAll()
}

