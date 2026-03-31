package com.example.summarizer.knowledge_cards.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.common.database.dao.KnowledgeCardDao
import com.example.common.database.models.KnowledgeCardEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class KnowledgeCardUiState(
    val cards: List<KnowledgeCardEntity> = emptyList(),
    val searchQuery: String = "",
)

@HiltViewModel
class KnowledgeCardViewModel
    @Inject
    constructor(
        private val knowledgeCardDao: KnowledgeCardDao,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(KnowledgeCardUiState())
        val uiState: StateFlow<KnowledgeCardUiState> = _uiState.asStateFlow()

        private val _errorEvents = Channel<String>()
        val errorEvents = _errorEvents.receiveAsFlow()

        init {
            loadCards()
        }

        private fun loadCards(tag: String = "") {
            viewModelScope.launch {
                val flow =
                    if (tag.isBlank()) {
                        knowledgeCardDao.getAllCards()
                    } else {
                        knowledgeCardDao.getCardsByTag(tag)
                    }

                flow.catch { e ->
                    _errorEvents.send("加载知识卡片失败: ${e.message}")
                }.collect { cards ->
                    _uiState.value = _uiState.value.copy(cards = cards)
                }
            }
        }

        fun updateSearchQuery(query: String) {
            _uiState.value = _uiState.value.copy(searchQuery = query)
            loadCards(query)
        }

        fun deleteCard(card: KnowledgeCardEntity) {
            viewModelScope.launch {
                try {
                    knowledgeCardDao.deleteCard(card)
                } catch (e: Exception) {
                    _errorEvents.send("删除失败: ${e.message}")
                }
            }
        }

        fun addCard(
            title: String,
            content: String,
            tags: String,
            source: String = "manual",
        ) {
            viewModelScope.launch {
                try {
                    val card =
                        KnowledgeCardEntity(
                            id = UUID.randomUUID().toString(),
                            title = title,
                            content = content,
                            tags = tags,
                            source = source,
                        )
                    knowledgeCardDao.insertCard(card)
                } catch (e: Exception) {
                    _errorEvents.send("保存失败: ${e.message}")
                }
            }
        }
    }
