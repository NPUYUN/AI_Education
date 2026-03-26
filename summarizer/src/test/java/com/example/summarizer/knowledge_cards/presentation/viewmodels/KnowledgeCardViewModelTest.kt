package com.example.summarizer.knowledge_cards.presentation.viewmodels

import com.example.common.database.dao.KnowledgeCardDao
import com.example.common.database.models.KnowledgeCardEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class KnowledgeCardViewModelTest {
    private lateinit var viewModel: KnowledgeCardViewModel
    private lateinit var mockDao: KnowledgeCardDao
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockDao = mock(KnowledgeCardDao::class.java)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads cards`() =
        runTest(testDispatcher) {
            val cards =
                listOf(
                    KnowledgeCardEntity(id = "1", title = "Test", content = "Content", tags = "tag", source = "source", timestamp = 0L),
                )
            whenever(mockDao.getAllCards()).thenReturn(flowOf(cards))

            viewModel = KnowledgeCardViewModel(mockDao)
            advanceUntilIdle()

            assertEquals(cards, viewModel.uiState.value.cards)
            assertNull(viewModel.uiState.value.error)
        }

    @Test
    fun `updateSearchQuery loads cards by tag when query is not blank`() =
        runTest(testDispatcher) {
            val allCards =
                listOf(
                    KnowledgeCardEntity(id = "1", title = "Test1", content = "Content1", tags = "tag1", source = "source", timestamp = 0L),
                )
            val filteredCards =
                listOf(
                    KnowledgeCardEntity(id = "2", title = "Test2", content = "Content2", tags = "tag2", source = "source", timestamp = 0L),
                )

            whenever(mockDao.getAllCards()).thenReturn(flowOf(allCards))
            whenever(mockDao.getCardsByTag("tag2")).thenReturn(flowOf(filteredCards))

            viewModel = KnowledgeCardViewModel(mockDao)
            advanceUntilIdle()

            viewModel.updateSearchQuery("tag2")
            advanceUntilIdle()

            assertEquals("tag2", viewModel.uiState.value.searchQuery)
            assertEquals(filteredCards, viewModel.uiState.value.cards)
        }

    @Test
    fun `addCard inserts card into database`() =
        runTest(testDispatcher) {
            whenever(mockDao.getAllCards()).thenReturn(flowOf(emptyList()))
            viewModel = KnowledgeCardViewModel(mockDao)
            advanceUntilIdle()

            viewModel.addCard("New Title", "New Content", "new, tags")
            advanceUntilIdle()

            verify(mockDao).insertCard(
                org.mockito.kotlin.check {
                    assertEquals("New Title", it.title)
                    assertEquals("New Content", it.content)
                    assertEquals("new, tags", it.tags)
                    assertEquals("manual", it.source)
                },
            )
        }

    @Test
    fun `deleteCard removes card from database`() =
        runTest(testDispatcher) {
            whenever(mockDao.getAllCards()).thenReturn(flowOf(emptyList()))
            viewModel = KnowledgeCardViewModel(mockDao)
            advanceUntilIdle()

            val card = KnowledgeCardEntity(id = "1", title = "Test", content = "Content", tags = "tag", source = "source", timestamp = 0L)
            viewModel.deleteCard(card)
            advanceUntilIdle()

            verify(mockDao).deleteCard(card)
        }
}
