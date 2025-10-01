package com.akra.geonsaehelperaiserver.domain.vector

import com.akra.geonsaehelperaiserver.domain.ai.config.AiProperties
import com.akra.geonsaehelperaiserver.domain.ai.model.AiEmbeddingModel
import com.akra.geonsaehelperaiserver.domain.ai.model.AiEmbeddingRequest
import com.akra.geonsaehelperaiserver.domain.ai.model.AiEmbeddingResponse
import com.akra.geonsaehelperaiserver.domain.ai.service.AiEmbeddingService
import com.akra.geonsaehelperaiserver.domain.vector.LoanProductType
import com.akra.geonsaehelperaiserver.domain.vector.LoanProductVectorPayload
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.stubbing.Answer
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore

class VectorStoreServiceTest {

    private val vectorStore: VectorStore = Mockito.mock(VectorStore::class.java)
    private val aiEmbeddingService: AiEmbeddingService = Mockito.mock(
        AiEmbeddingService::class.java,
        Answer { invocation ->
            when (invocation.method.name) {
                "embed" -> AiEmbeddingResponse(
                    vectors = listOf(listOf(1.0f, 0.5f, -0.2f)),
                    dimensions = 3,
                    model = AiEmbeddingModel.TEXT_EMBEDDING_3_SMALL,
                    provider = AiProperties.Provider.OPENAI
                ).also {
                    lastEmbedRequest = invocation.getArgument(0) as AiEmbeddingRequest
                }

                else -> Mockito.RETURNS_DEFAULTS.answer(invocation)
            }
        }
    )
    private val properties = VectorStoreProperties(defaultTopK = 3)
    private val service = VectorStoreService(vectorStore, properties, aiEmbeddingService)
    private var lastEmbedRequest: AiEmbeddingRequest? = null

    @BeforeEach
    fun resetMocks() {
        Mockito.reset(vectorStore)
        Mockito.clearInvocations(aiEmbeddingService)
        lastEmbedRequest = null
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
                LoanProductVectorPayload(
                    id = "doc-1",
                    content = "sample content",
                    productType = LoanProductType.RENT_STANDARD,
                    chunkIndex = 0,
                    embeddingModel = "model-x",
                    provider = "provider-y",
                    extraMetadata = mapOf(
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
        assertThat(document.metadata)
            .containsEntry(LoanProductVectorPayload.KEY_PRODUCT_TYPE, LoanProductType.RENT_STANDARD.name)
            .containsEntry(
                LoanProductVectorPayload.KEY_PRODUCT_TYPE_DESCRIPTION,
                LoanProductType.RENT_STANDARD.description
            )
            .containsEntry(LoanProductVectorPayload.KEY_CHUNK_INDEX, 0)
            .containsEntry(LoanProductVectorPayload.KEY_EMBEDDING_MODEL, "model-x")
            .containsEntry(LoanProductVectorPayload.KEY_PROVIDER, "provider-y")
            .containsEntry("keep", "value")
        assertThat(document.metadata).doesNotContainKey("ignore")
    }

    @Test
    fun `search maps documents and uses default topK`() {
        val storedDocument = Document(
            "doc-1",
            "hello world",
            mutableMapOf<String, Any>(
                "source" to "test",
                LoanProductVectorPayload.KEY_PRODUCT_TYPE to LoanProductType.RENT_STANDARD.name
            )
        )
        Mockito.doReturn(listOf(storedDocument))
            .`when`(vectorStore)
            .similaritySearch(ArgumentMatchers.any(SearchRequest::class.java))

        val response = service.search(VectorSearchRequest(query = VectorQuery.Text("hello"), topK = null))

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

        assertThat(lastEmbedRequest).isNotNull
        assertThat(lastEmbedRequest?.inputs).containsExactly("hello")
    }

    @Test
    fun `search applies product type metadata filter`() {
        val storedDocument = Document(
            "doc-1",
            "hello world",
            mutableMapOf<String, Any>(
                LoanProductVectorPayload.KEY_PRODUCT_TYPE to LoanProductType.RENT_STANDARD.name,
                "source" to "test"
            )
        )
        Mockito.doReturn(listOf(storedDocument))
            .`when`(vectorStore)
            .similaritySearch(ArgumentMatchers.any(SearchRequest::class.java))

        val response = service.search(
            VectorSearchRequest(
                query = VectorQuery.Text("hello"),
                topK = 2,
                productTypes = setOf(LoanProductType.RENT_STANDARD)
            )
        )

        assertThat(response.documents).hasSize(1)

        val searchCaptor = ArgumentCaptor.forClass(SearchRequest::class.java)
        verify(vectorStore).similaritySearch(searchCaptor.capture())
        assertThat(searchCaptor.value.filterExpression).isNotNull
    }

    @Test
    fun `search filters out documents with different product type`() {
        val storedDocument = Document(
            "doc-1",
            "hello world",
            mutableMapOf<String, Any>(
                LoanProductVectorPayload.KEY_PRODUCT_TYPE to LoanProductType.RENT_NEWLYWED.name,
                "source" to "test"
            )
        )
        Mockito.doReturn(listOf(storedDocument))
            .`when`(vectorStore)
            .similaritySearch(ArgumentMatchers.any(SearchRequest::class.java))

        val response = service.search(
            VectorSearchRequest(
                query = VectorQuery.Text("hello"),
                productTypes = setOf(LoanProductType.RENT_STANDARD)
            )
        )

        assertThat(response.documents).isEmpty()
    }

    @Test
    fun `search uses provided vector without embedding`() {
        val storedDocument = Document("doc-2", "vector doc", mutableMapOf<String, Any>())
        Mockito.doReturn(listOf(storedDocument))
            .`when`(vectorStore)
            .similaritySearch(ArgumentMatchers.any(SearchRequest::class.java))

        val response = service.search(
            VectorSearchRequest(
                query = VectorQuery.Vector(values = listOf(0.1f, 0.2f, 0.3f)),
                topK = 1
            )
        )

        assertThat(response.documents).isEmpty()
        assertThat(lastEmbedRequest).isNull()
    }
}
