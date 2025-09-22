package com.akra.geonsaehelperaiserver.vector

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(VectorStoreController::class)
class VectorStoreControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var vectorStoreService: VectorStoreService

    @Test
    fun `upsert forwards request to service`() {
        val request = VectorUpsertRequest(
            documents = listOf(
                VectorDocumentPayload(
                    id = "doc-1",
                    content = "hello world",
                    metadata = mapOf("source" to "test")
                )
            )
        )

        mockMvc.perform(
            post("/api/vectors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNoContent)

        verify(vectorStoreService).upsert(request)
    }

    @Test
    fun `search returns response from service`() {
        val request = VectorSearchRequest(query = "hello", topK = 2)
        val response = VectorSearchResponse(
            documents = listOf(
                VectorDocumentResponse(
                    id = "doc-1",
                    content = "hello text",
                    score = 0.9,
                    metadata = mapOf("source" to "test")
                )
            )
        )
        `when`(vectorStoreService.search(request)).thenReturn(response)

        mockMvc.perform(
            post("/api/vectors/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.documents[0].id").value("doc-1"))
            .andExpect(jsonPath("$.documents[0].score").value(0.9))
            .andExpect(jsonPath("$.documents[0].metadata.source").value("test"))

        verify(vectorStoreService).search(request)
    }
}
