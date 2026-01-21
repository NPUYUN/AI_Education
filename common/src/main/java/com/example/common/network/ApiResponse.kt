package com.example.common.network

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: T? = null
) {
    fun isSuccess(): Boolean = code == 200 // Assuming 200 is success
}
