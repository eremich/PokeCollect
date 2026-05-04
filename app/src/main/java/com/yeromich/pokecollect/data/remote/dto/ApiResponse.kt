package com.yeromich.pokecollect.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val data: List<T>,
    val page: Int,
    val pageSize: Int,
    val count: Int,
    val totalCount: Int
)
