package com.akra.geonsaehelperaiserver.vector

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore

class VectorStoreServiceTest {

    private val vectorStore: VectorStore = Mockito.mock(VectorStore::class.java)
    private val properties = VectorStoreProperties(defaultTopK = 3)
    private val service = VectorStoreService(vectorStore, properties)

    @BeforeEach
    fun resetMocks() {
        Mockito.reset(vectorStore)
    }

    @Test
    fun `upsert ignores empty payload`() {
        service.upsert(VectorUpsertRequest(emptyList()))

        Mockito.verifyNoInteractions(vectorStore)
    }

    @Test
    fun `upsert adds documents to store`() {
        val request = VectorUpsertRequest(
            documents = listOf(
                VectorDocumentPayload(
                    id = "doc-1",
                    content = "sample content",
                    metadata = mapOf(
                        "keep" to "value",
                        "ignore" to null
                    )
                )
            )
        )

        service.upsert(request)

        @Suppress("UNCHECKED_CAST")
        val captor = ArgumentCaptor.forClass(List::class.java) as ArgumentCaptor<List<Document>>
        verify(vectorStore).add(captor.capture())

        val savedDocuments = captor.value
        assertThat(savedDocuments).hasSize(1)
        val document = savedDocuments.first()
        assertThat(document.id).isEqualTo("doc-1")
        assertThat(document.text).isEqualTo("sample content")
        assertThat(document.metadata).containsEntry("keep", "value")
        assertThat(document.metadata).doesNotContainKey("ignore")
    }

    @Test
    fun `search maps documents and uses default topK`() {
        val storedDocument = Document("doc-1", "hello world", mutableMapOf<String, Any>("source" to "test"))
        Mockito.`when`(
            vectorStore.similaritySearch(ArgumentMatchers.any(SearchRequest::class.java))
        ).thenReturn(listOf(storedDocument))

        val response = service.search(VectorSearchRequest(query = "hello", topK = null))

        assertThat(response.documents).hasSize(1)
        val result = response.documents.first()
        assertThat(result.id).isEqualTo("doc-1")
        assertThat(result.content).isEqualTo("hello world")
        assertThat(result.score).isNull()
        assertThat(result.metadata).containsEntry("source", "test")

        val searchCaptor = ArgumentCaptor.forClass(SearchRequest::class.java)
        verify(vectorStore).similaritySearch(searchCaptor.capture())
        assertThat(searchCaptor.value.topK).isEqualTo(3)
        assertThat(searchCaptor.value.query).isEqualTo("hello")
    }
}
