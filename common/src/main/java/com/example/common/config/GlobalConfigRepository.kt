package com.example.common.config

import com.example.common.database.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlobalConfigRepository
    @Inject
    constructor(
        private val preferencesManager: PreferencesManager,
    ) {
        // Global API Toggle
        fun getUseGlobalApi(): Flow<Boolean> = preferencesManager.getBoolean("use_global_api", false)

        suspend fun saveUseGlobalApi(useGlobal: Boolean) = preferencesManager.saveBoolean("use_global_api", useGlobal)

        // General API Key (Bailian fallback - maintained for legacy support)
        fun getBailianApiKey(): Flow<String> = preferencesManager.getString("bailian_api_key", "")

        suspend fun saveBailianApiKey(apiKey: String) = preferencesManager.saveString("bailian_api_key", apiKey)

        // Global Settings
        fun getGlobalApiKey(): Flow<String> = preferencesManager.getString("global_api_key", "")

        suspend fun saveGlobalApiKey(apiKey: String) = preferencesManager.saveString("global_api_key", apiKey)

        fun getGlobalBaseUrl(): Flow<String> = preferencesManager.getString("global_base_url", AppConstants.BASE_URL)

        suspend fun saveGlobalBaseUrl(baseUrl: String) = preferencesManager.saveString("global_base_url", baseUrl)

        fun getGlobalModelName(): Flow<String> = preferencesManager.getString("global_model_name", AppConstants.DEFAULT_MODEL_NAME)

        suspend fun saveGlobalModelName(modelName: String) = preferencesManager.saveString("global_model_name", modelName)

        // AI Tutor Configs
        fun getAiTutorApiKeyRaw(): Flow<String> = preferencesManager.getString("api_key_ai_tutor", "")

        fun getAiTutorApiKey(): Flow<String> =
            combine(getUseGlobalApi(), getGlobalApiKey(), getAiTutorApiKeyRaw()) { useGlobal, global, local ->
                if (useGlobal) global else local
            }

        suspend fun saveAiTutorApiKey(apiKey: String) = preferencesManager.saveString("api_key_ai_tutor", apiKey)

        fun getAiTutorBaseUrlRaw(): Flow<String> = preferencesManager.getString("base_url_ai_tutor", AppConstants.BASE_URL)

        fun getAiTutorBaseUrl(): Flow<String> =
            combine(getUseGlobalApi(), getGlobalBaseUrl(), getAiTutorBaseUrlRaw()) { useGlobal, global, local ->
                if (useGlobal) global.ifBlank { AppConstants.BASE_URL } else local.ifBlank { AppConstants.BASE_URL }
            }

        suspend fun saveAiTutorBaseUrl(baseUrl: String) = preferencesManager.saveString("base_url_ai_tutor", baseUrl)

        fun getAiTutorModelNameRaw(): Flow<String> = preferencesManager.getString("model_name_ai_tutor", AppConstants.DEFAULT_MODEL_NAME)

        fun getAiTutorModelName(): Flow<String> =
            combine(getUseGlobalApi(), getGlobalModelName(), getAiTutorModelNameRaw()) { useGlobal, global, local ->
                if (useGlobal) global.ifBlank { AppConstants.DEFAULT_MODEL_NAME } else local.ifBlank { AppConstants.DEFAULT_MODEL_NAME }
            }

        suspend fun saveAiTutorModelName(modelName: String) = preferencesManager.saveString("model_name_ai_tutor", modelName)

        // Timeline Map Configs
        fun getTimelineMapApiKeyRaw(): Flow<String> = preferencesManager.getString("api_key_timeline_map", "")

        fun getTimelineMapApiKey(): Flow<String> =
            combine(getUseGlobalApi(), getGlobalApiKey(), getTimelineMapApiKeyRaw()) { useGlobal, global, local ->
                if (useGlobal) global else local
            }

        suspend fun saveTimelineMapApiKey(apiKey: String) = preferencesManager.saveString("api_key_timeline_map", apiKey)

        fun getTimelineMapBaseUrlRaw(): Flow<String> = preferencesManager.getString("base_url_timeline_map", AppConstants.BASE_URL)

        fun getTimelineMapBaseUrl(): Flow<String> =
            combine(getUseGlobalApi(), getGlobalBaseUrl(), getTimelineMapBaseUrlRaw()) { useGlobal, global, local ->
                if (useGlobal) global.ifBlank { AppConstants.BASE_URL } else local.ifBlank { AppConstants.BASE_URL }
            }

        suspend fun saveTimelineMapBaseUrl(baseUrl: String) = preferencesManager.saveString("base_url_timeline_map", baseUrl)

        fun getTimelineMapModelNameRaw(): Flow<String> =
            preferencesManager.getString("model_name_timeline_map", AppConstants.DEFAULT_MODEL_NAME)

        fun getTimelineMapModelName(): Flow<String> =
            combine(getUseGlobalApi(), getGlobalModelName(), getTimelineMapModelNameRaw()) { useGlobal, global, local ->
                if (useGlobal) global.ifBlank { AppConstants.DEFAULT_MODEL_NAME } else local.ifBlank { AppConstants.DEFAULT_MODEL_NAME }
            }

        suspend fun saveTimelineMapModelName(modelName: String) = preferencesManager.saveString("model_name_timeline_map", modelName)

        // Video Summarizer Configs
        fun getVideoSummaryApiKeyRaw(): Flow<String> = preferencesManager.getString("api_key_video_summary", "")

        fun getVideoSummaryApiKey(): Flow<String> =
            combine(getUseGlobalApi(), getGlobalApiKey(), getVideoSummaryApiKeyRaw()) { useGlobal, global, local ->
                if (useGlobal) global else local
            }

        suspend fun saveVideoSummaryApiKey(apiKey: String) = preferencesManager.saveString("api_key_video_summary", apiKey)

        fun getVideoSummaryBaseUrlRaw(): Flow<String> = preferencesManager.getString("base_url_video_summary", AppConstants.BASE_URL)

        fun getVideoSummaryBaseUrl(): Flow<String> =
            combine(getUseGlobalApi(), getGlobalBaseUrl(), getVideoSummaryBaseUrlRaw()) { useGlobal, global, local ->
                if (useGlobal) global.ifBlank { AppConstants.BASE_URL } else local.ifBlank { AppConstants.BASE_URL }
            }

        suspend fun saveVideoSummaryBaseUrl(baseUrl: String) = preferencesManager.saveString("base_url_video_summary", baseUrl)

        fun getVideoSummaryModelNameRaw(): Flow<String> =
            preferencesManager.getString("model_name_video_summary", AppConstants.DEFAULT_MODEL_NAME)

        fun getVideoSummaryModelName(): Flow<String> =
            combine(getUseGlobalApi(), getGlobalModelName(), getVideoSummaryModelNameRaw()) { useGlobal, global, local ->
                if (useGlobal) global.ifBlank { AppConstants.DEFAULT_MODEL_NAME } else local.ifBlank { AppConstants.DEFAULT_MODEL_NAME }
            }

        suspend fun saveVideoSummaryModelName(modelName: String) = preferencesManager.saveString("model_name_video_summary", modelName)

        // Effective Key Getters (for backward compatibility and general fallback)
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

        // Current User ID
        fun getCurrentUserId(): Flow<String> = preferencesManager.getString("current_user_id", "")

        suspend fun saveCurrentUserId(userId: String) = preferencesManager.saveString("current_user_id", userId)

        fun getUserNickname(): Flow<String> = preferencesManager.getString("user_nickname", "用户昵称")

        suspend fun saveUserNickname(nickname: String) = preferencesManager.saveString("user_nickname", nickname)

        fun getUserSignature(): Flow<String> = preferencesManager.getString("user_signature", "这里是个性签名...")

        suspend fun saveUserSignature(signature: String) = preferencesManager.saveString("user_signature", signature)
    }
