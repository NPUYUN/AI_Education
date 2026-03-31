package com.example.common.utils

import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 统一网络异常处理，转换为用户友好的中文提示
 */
fun Throwable.toUserFriendlyMessage(): String {
    return when (this) {
        is SocketTimeoutException -> "网络请求超时，请稍后重试"
        is UnknownHostException -> "无法连接到服务器，请检查网络设置"
        else -> this.message ?: "发生异常"
    }
}
