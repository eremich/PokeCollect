package com.yeromich.pokecollect.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SingleResponse<T>(val data: T)
