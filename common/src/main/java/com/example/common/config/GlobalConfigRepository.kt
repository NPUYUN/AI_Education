package com.example.common.config

import com.example.common.database.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlobalConfigRepository @Inject constructor(
    private val preferencesManager: PreferencesManager
) {
    // General API Key
    fun getBailianApiKey(): Flow<String> = preferencesManager.getString("bailian_api_key", "")
    suspend fun saveBailianApiKey(apiKey: String) = preferencesManager.saveString("bailian_api_key", apiKey)

    // AI Tutor Configs
    fun getAiTutorApiKey(): Flow<String> = preferencesManager.getString("api_key_ai_tutor", "")
    suspend fun saveAiTutorApiKey(apiKey: String) = preferencesManager.saveString("api_key_ai_tutor", apiKey)

    fun getAiTutorBaseUrl(): Flow<String> = preferencesManager.getString("base_url_ai_tutor", AppConstants.BASE_URL)
    suspend fun saveAiTutorBaseUrl(baseUrl: String) = preferencesManager.saveString("base_url_ai_tutor", baseUrl)

    fun getAiTutorModelName(): Flow<String> = preferencesManager.getString("model_name_ai_tutor", AppConstants.DEFAULT_MODEL_NAME)
    suspend fun saveAiTutorModelName(modelName: String) = preferencesManager.saveString("model_name_ai_tutor", modelName)

    // Timeline Map Configs
    fun getTimelineMapApiKey(): Flow<String> = preferencesManager.getString("api_key_timeline_map", "")
    suspend fun saveTimelineMapApiKey(apiKey: String) = preferencesManager.saveString("api_key_timeline_map", apiKey)

    fun getTimelineMapBaseUrl(): Flow<String> = preferencesManager.getString("base_url_timeline_map", AppConstants.BASE_URL)
    suspend fun saveTimelineMapBaseUrl(baseUrl: String) = preferencesManager.saveString("base_url_timeline_map", baseUrl)

    fun getTimelineMapModelName(): Flow<String> = preferencesManager.getString("model_name_timeline_map", AppConstants.DEFAULT_MODEL_NAME)
    suspend fun saveTimelineMapModelName(modelName: String) = preferencesManager.saveString("model_name_timeline_map", modelName)

    // Video Summarizer Configs
    fun getVideoSummaryApiKey(): Flow<String> = preferencesManager.getString("api_key_video_summary", "")
    suspend fun saveVideoSummaryApiKey(apiKey: String) = preferencesManager.saveString("api_key_video_summary", apiKey)

    fun getVideoSummaryBaseUrl(): Flow<String> = preferencesManager.getString("base_url_video_summary", AppConstants.BASE_URL)
    suspend fun saveVideoSummaryBaseUrl(baseUrl: String) = preferencesManager.saveString("base_url_video_summary", baseUrl)

    fun getVideoSummaryModelName(): Flow<String> = preferencesManager.getString("model_name_video_summary", AppConstants.DEFAULT_MODEL_NAME)
    suspend fun saveVideoSummaryModelName(modelName: String) = preferencesManager.saveString("model_name_video_summary", modelName)

    // Current User ID
    fun getCurrentUserId(): Flow<String> = preferencesManager.getString("current_user_id", "")
    suspend fun saveCurrentUserId(userId: String) = preferencesManager.saveString("current_user_id", userId)

    fun getUserNickname(): Flow<String> = preferencesManager.getString("user_nickname", "用户昵称")
    suspend fun saveUserNickname(nickname: String) = preferencesManager.saveString("user_nickname", nickname)

    fun getUserSignature(): Flow<String> = preferencesManager.getString("user_signature", "这里是个性签名...")
    suspend fun saveUserSignature(signature: String) = preferencesManager.saveString("user_signature", signature)
    fun getEffectiveAiTutorApiKey(): Flow<String> {
        return getAiTutorApiKey().combine(getBailianApiKey()) { featureKey, generalKey ->
            featureKey.ifBlank { generalKey.ifBlank { AppConstants.DEFAULT_API_KEY } }
        }
    }

    fun getEffectiveTimelineMapApiKey(): Flow<String> {
        return getTimelineMapApiKey().combine(getBailianApiKey()) { featureKey, generalKey ->
            featureKey.ifBlank { generalKey.ifBlank { AppConstants.DEFAULT_API_KEY } }
        }
    }

    fun getEffectiveVideoSummaryApiKey(): Flow<String> {
        return getVideoSummaryApiKey().combine(getBailianApiKey()) { featureKey, generalKey ->
            featureKey.ifBlank { generalKey.ifBlank { AppConstants.DEFAULT_API_KEY } }
        }
    }
}
