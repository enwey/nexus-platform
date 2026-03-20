package com.nexus.platform.api

import com.google.gson.JsonObject

/**
 * API处理器接口
 */
interface ApiHandler {
    /**
     * 处理API请求
     * @param api API名称
     * @param params 请求参数
     * @return 处理结果
     */
    suspend fun handle(api: String, params: JsonObject): Any?
}
