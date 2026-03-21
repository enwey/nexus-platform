package com.nexus.platform.api

import android.content.Context
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.nakama.apiclient.ApiClient
import io.nakama.core.DefaultSession
import io.nakama.core.UserAccount
import io.nakama.core.session.AuthenticateRequest
import io.nakama.core.StorageObjectWrite
import io.nakama.core.LeaderboardRecordWrite
import io.nakama.core.StorageObjectId
import io.nakama.apiclient.StorageObject
import io.nakama.apiclient.LeaderboardRecord
import io.nakama.apiclient.StorageObject
import io.nakama.apiclient.LeaderboardRecord

/**
 * 社交API处理器，基于Nakama游戏服务器
 */
class SocialApi(private val context: Context, private val apiClient: ApiClient) : ApiHandler {
    override suspend fun handle(api: String, params: JsonObject): Any? {
        return when (api) {
            "wx.getFriendCloudStorage" -> getFriendCloudStorage(params)
            "wx.setUserCloudStorage" -> setUserCloudStorage(params)
            "wx.getFriendInfo" -> getFriendInfo(params)
            "wx.createLeaderboard" -> createLeaderboard(params)
            "wx.getLeaderboard" -> getLeaderboard(params)
            "wx.getGroupList" -> getGroupList(params)
            "wx.getGroupInfo" -> getGroupInfo(params)
            else -> null
        }
    }

    /**
     * 获取好友云存储数据
     * @param params 包含userId的参数
     * @return 云存储数据列表
     */
    private suspend fun getFriendCloudStorage(params: JsonObject): Map<String, Any> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = params.get("userId")?.asString ?: ""
                val session = getSession(userId) ?: return mapOf("errMsg" to "getFriendCloudStorage:fail")
                
                val storageObjects = apiClient.listStorageObjects(session, "", 100, null).get()
                
                val data = storageObjects.map { obj ->
                    mapOf(
                        "key" to obj.key,
                        "value" to obj.value,
                        "version" to obj.version,
                        "updateTime" to obj.updateTime
                    )
                }
                
                return mapOf(
                    "data" to data,
                    "errMsg" to "getFriendCloudStorage:ok"
                )
            } catch (e: Exception) {
                return mapOf("errMsg" to "getFriendCloudStorage:fail", "error" to e.message)
            }
        }
    }

    /**
     * 设置用户云存储数据
     * @param params 包含userId、key和value的参数
     * @return 操作结果
     */
    private suspend fun setUserCloudStorage(params: JsonObject): Map<String, Any> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = params.get("userId")?.asString ?: ""
                val key = params.get("key")?.asString ?: ""
                val value = params.get("value")?.asString ?: ""
                val kvData = listOf(mapOf("key" to key, "value" to value))
                
                val session = getSession(userId) ?: return mapOf("errMsg" to "setUserCloudStorage:fail")
                
                val write = StorageObjectWrite.builder()
                    .collection("")
                    .key(userId)
                    .value(value)
                    .version("")
                    .build()
                
                apiClient.writeStorageObjects(session, userId, kvData).get()
                
                return mapOf("errMsg" to "setUserCloudStorage:ok")
            } catch (e: Exception) {
                return mapOf("errMsg" to "setUserCloudStorage:fail", "error" to e.message)
            }
        }
    }

    /**
     * 获取好友信息
     * @param params 包含userId和friendId的参数
     * @return 好友信息
     */
    private suspend fun getFriendInfo(params: JsonObject): Map<String, Any> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = params.get("userId")?.asString ?: ""
                val friendId = params.get("friendId")?.asString ?: ""
                
                val session = getSession(userId) ?: return mapOf("errMsg" to "getFriendInfo:fail")
                
                val users = apiClient.getUsers(session, listOf(friendId)).get()
                
                if (users.isEmpty()) {
                    return mapOf("errMsg" to "getFriendInfo:fail")
                }
                
                val user = users[0]
                return mapOf(
                    "data" to mapOf(
                        "openId" to user.id,
                        "username" to user.username,
                        "displayName" to user.displayName,
                        "avatarUrl" to user.avatarUrl,
                        "langTag" to user.langTag,
                        "location" to user.location,
                        "timezone" to user.timezone,
                        "metadata" to user.metadata
                    ),
                    "errMsg" to "getFriendInfo:ok"
                )
            } catch (e: Exception) {
                return mapOf("errMsg" to "getFriendInfo:fail", "error" to e.message)
            }
        }
    }

    /**
     * 创建排行榜记录
     * @param params 包含userId、leaderboardId、score和subscore的参数
     * @return 操作结果
     */
    private suspend fun createLeaderboard(params: JsonObject): Map<String, Any> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = params.get("userId")?.asString ?: ""
                val leaderboardId = params.get("leaderboardId")?.asString ?: "default"
                val score = params.get("score")?.asLong ?: 0L
                val subscore = params.get("subscore")?.asLong ?: 0L
                
                val session = getSession(userId) ?: return mapOf("errMsg" to "createLeaderboard:fail")
                
                val write = LeaderboardRecordWrite.builder()
                    .leaderboardId(leaderboardId)
                    .userId(userId)
                    .username(userId)
                    .score(score)
                    .subscore(subscore)
                    .build()
                
                apiClient.writeLeaderboardRecord(session, write).get()
                
                return mapOf("errMsg" to "createLeaderboard:ok")
            } catch (e: Exception) {
                return mapOf("errMsg" to "createLeaderboard:fail", "error" to e.message)
            }
        }
    }

    /**
     * 获取排行榜数据
     * @param params 包含userId、leaderboardId和limit的参数
     * @return 排行榜数据列表
     */
    private suspend fun getLeaderboard(params: JsonObject): Map<String, Any> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = params.get("userId")?.asString ?: ""
                val leaderboardId = params.get("leaderboardId")?.asString ?: "default"
                val limit = params.get("limit")?.asInt ?: 10
                
                val session = getSession(userId) ?: return mapOf("errMsg" to "getLeaderboard:fail")
                
                val records = apiClient.listLeaderboardRecords(session, leaderboardId, null, limit, null).get()
                
                val data = records.map { record ->
                    mapOf(
                        "rank" to record.rank,
                        "score" to record.score,
                        "subscore" to record.subscore,
                        "numScore" to record.numScore,
                        "metadata" to record.metadata,
                        "ownerId" to record.ownerId,
                        "username" to record.username,
                        "avatarUrl" to record.avatarUrl,
                        "updateTime" to record.updateTime
                    )
                }
                
                return mapOf(
                    "data" to data,
                    "errMsg" to "getLeaderboard:ok"
                )
            } catch (e: Exception) {
                return mapOf("errMsg" to "getLeaderboard:fail", "error" to e.message)
            }
        }
    }

    /**
     * 获取用户群组列表
     * @param params 包含userId的参数
     * @return 群组列表
     */
    private suspend fun getGroupList(params: JsonObject): Map<String, Any> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = params.get("userId")?.asString ?: ""
                
                val session = getSession(userId) ?: return mapOf("errMsg" to "getGroupList:fail")
                
                val groups = apiClient.listUserGroups(session, 0, 100, null).get()
                
                val data = groups.map { group ->
                    mapOf(
                        "id" to group.id,
                        "creatorId" to group.creatorId,
                        "name" to group.name,
                        "description" to group.description,
                        "langTag" to group.langTag,
                        "open" to group.open,
                        "avatarUrl" to group.avatarUrl,
                        "maxCount" to group.maxCount,
                        "createTime" to group.createTime,
                        "updateTime" to group.updateTime
                    )
                }
                
                return mapOf(
                    "data" to data,
                    "errMsg" to "getGroupList:ok"
                )
            } catch (e: Exception) {
                return mapOf("errMsg" to "getGroupList:fail", "error" to e.message)
            }
        }
    }

    /**
     * 获取群组信息
     * @param params 包含userId和groupId的参数
     * @return 群组信息
     */
    private suspend fun getGroupInfo(params: JsonObject): Map<String, Any> {
        return withContext(Dispatchers.IO) {
            try {
                val userId = params.get("userId")?.asString ?: ""
                val groupId = params.get("groupId")?.asString ?: ""
                
                val session = getSession(userId) ?: return mapOf("errMsg" to "getGroupInfo:fail")
                
                val group = apiClient.getGroup(session, groupId).get()
                
                return mapOf(
                    "data" to mapOf(
                        "id" to group.id,
                        "creatorId" to group.creatorId,
                        "name" to group.name,
                        "description" to group.description,
                        "langTag" to group.langTag,
                        "open" to group.open,
                        "avatarUrl" to group.avatarUrl,
                        "maxCount" to group.maxCount,
                        "createTime" to group.createTime,
                        "updateTime" to group.updateTime,
                        "members" to group.members
                    ),
                    "errMsg" to "getGroupInfo:ok"
                )
            } catch (e: Exception) {
                return mapOf("errMsg" to "getGroupInfo:fail", "error" to e.message)
            }
        }
    }

    /**
     * 获取用户会话
     * @param userId 用户ID
     * @return 用户会话对象
     */
    private suspend fun getSession(userId: String): DefaultSession? {
        val prefs = context.getSharedPreferences("nakama", Context.MODE_PRIVATE)
        val token = prefs.getString("nakama_token_$userId", null) ?: return null
        
        return DefaultSession.builder()
            .token(token)
            .userId(userId)
            .build()
    }
}
