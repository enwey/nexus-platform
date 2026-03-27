package com.nexus.platform.core.bridge.api

import com.google.gson.JsonObject

/**
 * API澶勭悊鍣ㄦ帴鍙? */
interface ApiHandler {
    /**
     * 澶勭悊API璇锋眰
     * @param api API鍚嶇О
     * @param params 璇锋眰鍙傛暟
     * @return 澶勭悊缁撴灉
     */
    suspend fun handle(api: String, params: JsonObject): Any?
}

