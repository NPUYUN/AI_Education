package com.example.common.config

import app.cash.turbine.test
import com.example.common.database.PreferencesManager
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class GlobalConfigRepositoryTest {
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var repository: GlobalConfigRepository

    @Before
    fun setup() {
        preferencesManager = mock()
        repository = GlobalConfigRepository(preferencesManager)

        whenever(preferencesManager.getBoolean("use_global_api", false)).thenReturn(flowOf(false))
        whenever(preferencesManager.getString("global_api_key", "")).thenReturn(flowOf(""))
        whenever(preferencesManager.getString("global_base_url", AppConstants.BASE_URL)).thenReturn(flowOf(AppConstants.BASE_URL))
        whenever(
            preferencesManager.getString("global_model_name", AppConstants.DEFAULT_MODEL_NAME),
        ).thenReturn(flowOf(AppConstants.DEFAULT_MODEL_NAME))
    }

    // --- getEffectiveAiTutorApiKey ---

    @Test
    fun `getEffectiveAiTutorApiKey returns feature key when available`() =
        runTest {
            whenever(preferencesManager.getString("api_key_ai_tutor", "")).thenReturn(flowOf("feature_key"))
            whenever(preferencesManager.getString("bailian_api_key", "")).thenReturn(flowOf("general_key"))

            repository.getEffectiveAiTutorApiKey().test {
                assertEquals("feature_key", awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `getEffectiveAiTutorApiKey returns general key when feature key is empty`() =
        runTest {
            whenever(preferencesManager.getString("api_key_ai_tutor", "")).thenReturn(flowOf(""))
            whenever(preferencesManager.getString("bailian_api_key", "")).thenReturn(flowOf("general_key"))

            repository.getEffectiveAiTutorApiKey().test {
                assertEquals("general_key", awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `getEffectiveAiTutorApiKey returns default key when both are empty`() =
        runTest {
            whenever(preferencesManager.getString("api_key_ai_tutor", "")).thenReturn(flowOf(""))
            whenever(preferencesManager.getString("bailian_api_key", "")).thenReturn(flowOf(""))

            repository.getEffectiveAiTutorApiKey().test {
                assertEquals(AppConstants.DEFAULT_API_KEY, awaitItem())
                awaitComplete()
            }
        }

    // --- getEffectiveTimelineMapApiKey ---

    @Test
    fun `getEffectiveTimelineMapApiKey returns feature key when available`() =
        runTest {
            whenever(preferencesManager.getString("api_key_timeline_map", "")).thenReturn(flowOf("feature_key_tm"))
            whenever(preferencesManager.getString("bailian_api_key", "")).thenReturn(flowOf("general_key"))

            repository.getEffectiveTimelineMapApiKey().test {
                assertEquals("feature_key_tm", awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `getEffectiveTimelineMapApiKey returns default key when both are empty`() =
        runTest {
            whenever(preferencesManager.getString("api_key_timeline_map", "")).thenReturn(flowOf(""))
            whenever(preferencesManager.getString("bailian_api_key", "")).thenReturn(flowOf(""))

            repository.getEffectiveTimelineMapApiKey().test {
                assertEquals(AppConstants.DEFAULT_API_KEY, awaitItem())
                awaitComplete()
            }
        }

    // --- getEffectiveVideoSummaryApiKey ---

    @Test
    fun `getEffectiveVideoSummaryApiKey returns feature key when available`() =
        runTest {
            whenever(preferencesManager.getString("api_key_video_summary", "")).thenReturn(flowOf("feature_key_vs"))
            whenever(preferencesManager.getString("bailian_api_key", "")).thenReturn(flowOf("general_key"))

            repository.getEffectiveVideoSummaryApiKey().test {
                assertEquals("feature_key_vs", awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `getEffectiveVideoSummaryApiKey returns default key when both are empty`() =
        runTest {
            whenever(preferencesManager.getString("api_key_video_summary", "")).thenReturn(flowOf(""))
            whenever(preferencesManager.getString("bailian_api_key", "")).thenReturn(flowOf(""))

            repository.getEffectiveVideoSummaryApiKey().test {
                assertEquals(AppConstants.DEFAULT_API_KEY, awaitItem())
                awaitComplete()
            }
        }

    // --- General Getters and Setters ---

    @Test
    fun `getBailianApiKey returns correct flow`() =
        runTest {
            whenever(preferencesManager.getString("bailian_api_key", "")).thenReturn(flowOf("my_key"))

            repository.getBailianApiKey().test {
                assertEquals("my_key", awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `saveBailianApiKey calls preferencesManager`() =
        runTest {
            repository.saveBailianApiKey("new_key")
            verify(preferencesManager).saveString("bailian_api_key", "new_key")
        }

    @Test
    fun `saveAiTutorApiKey calls preferencesManager`() =
        runTest {
            repository.saveAiTutorApiKey("new_key")
            verify(preferencesManager).saveString("api_key_ai_tutor", "new_key")
        }

    @Test
    fun `getAiTutorBaseUrl returns correct flow`() =
        runTest {
            whenever(preferencesManager.getString("base_url_ai_tutor", AppConstants.BASE_URL)).thenReturn(flowOf("custom_url"))
            repository.getAiTutorBaseUrl().test {
                assertEquals("custom_url", awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `saveAiTutorBaseUrl calls preferencesManager`() =
        runTest {
            repository.saveAiTutorBaseUrl("new_url")
            verify(preferencesManager).saveString("base_url_ai_tutor", "new_url")
        }

    @Test
    fun `getAiTutorModelName returns correct flow`() =
        runTest {
            whenever(
                preferencesManager.getString("model_name_ai_tutor", AppConstants.DEFAULT_MODEL_NAME),
            ).thenReturn(flowOf("custom_model"))
            repository.getAiTutorModelName().test {
                assertEquals("custom_model", awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `saveAiTutorModelName calls preferencesManager`() =
        runTest {
            repository.saveAiTutorModelName("new_model")
            verify(preferencesManager).saveString("model_name_ai_tutor", "new_model")
        }

    @Test
    fun `getUserNickname returns correct flow`() =
        runTest {
            whenever(preferencesManager.getString("user_nickname", "用户昵称")).thenReturn(flowOf("TestUser"))
            repository.getUserNickname().test {
                assertEquals("TestUser", awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `saveUserNickname calls preferencesManager`() =
        runTest {
            repository.saveUserNickname("NewUser")
            verify(preferencesManager).saveString("user_nickname", "NewUser")
        }

    @Test
    fun `getTimelineMapBaseUrl returns correct flow`() =
        runTest {
            whenever(preferencesManager.getString("base_url_timeline_map", AppConstants.BASE_URL)).thenReturn(flowOf("custom_url"))
            repository.getTimelineMapBaseUrl().test {
                assertEquals("custom_url", awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `saveTimelineMapBaseUrl calls preferencesManager`() =
        runTest {
            repository.saveTimelineMapBaseUrl("new_url")
            verify(preferencesManager).saveString("base_url_timeline_map", "new_url")
        }

    @Test
    fun `getTimelineMapModelName returns correct flow`() =
        runTest {
            whenever(
                preferencesManager.getString("model_name_timeline_map", AppConstants.DEFAULT_MODEL_NAME),
            ).thenReturn(flowOf("custom_model"))
            repository.getTimelineMapModelName().test {
                assertEquals("custom_model", awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `saveTimelineMapModelName calls preferencesManager`() =
        runTest {
            repository.saveTimelineMapModelName("new_model")
            verify(preferencesManager).saveString("model_name_timeline_map", "new_model")
        }

    @Test
    fun `saveTimelineMapApiKey calls preferencesManager`() =
        runTest {
            repository.saveTimelineMapApiKey("new_key")
            verify(preferencesManager).saveString("api_key_timeline_map", "new_key")
        }

    @Test
    fun `getVideoSummaryBaseUrl returns correct flow`() =
        runTest {
            whenever(preferencesManager.getString("base_url_video_summary", AppConstants.BASE_URL)).thenReturn(flowOf("custom_url"))
            repository.getVideoSummaryBaseUrl().test {
                assertEquals("custom_url", awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `saveVideoSummaryBaseUrl calls preferencesManager`() =
        runTest {
            repository.saveVideoSummaryBaseUrl("new_url")
            verify(preferencesManager).saveString("base_url_video_summary", "new_url")
        }

    @Test
    fun `getVideoSummaryModelName returns correct flow`() =
        runTest {
            whenever(
                preferencesManager.getString("model_name_video_summary", AppConstants.DEFAULT_MODEL_NAME),
            ).thenReturn(flowOf("custom_model"))
            repository.getVideoSummaryModelName().test {
                assertEquals("custom_model", awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `saveVideoSummaryModelName calls preferencesManager`() =
        runTest {
            repository.saveVideoSummaryModelName("new_model")
            verify(preferencesManager).saveString("model_name_video_summary", "new_model")
        }

    @Test
    fun `saveVideoSummaryApiKey calls preferencesManager`() =
        runTest {
            repository.saveVideoSummaryApiKey("new_key")
            verify(preferencesManager).saveString("api_key_video_summary", "new_key")
        }

    @Test
    fun `getCurrentUserId returns correct flow`() =
        runTest {
            whenever(preferencesManager.getString("current_user_id", "")).thenReturn(flowOf("user_123"))
            repository.getCurrentUserId().test {
                assertEquals("user_123", awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `saveCurrentUserId calls preferencesManager`() =
        runTest {
            repository.saveCurrentUserId("user_123")
            verify(preferencesManager).saveString("current_user_id", "user_123")
        }

    @Test
    fun `getUserSignature returns correct flow`() =
        runTest {
            whenever(preferencesManager.getString("user_signature", "这里是个性签名...")).thenReturn(flowOf("My signature"))
            repository.getUserSignature().test {
                assertEquals("My signature", awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `saveUserSignature calls preferencesManager`() =
        runTest {
            repository.saveUserSignature("New signature")
            verify(preferencesManager).saveString("user_signature", "New signature")
        }
}
