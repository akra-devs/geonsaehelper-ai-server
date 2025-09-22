package com.akra.geonsaehelperaiserver

import org.junit.jupiter.api.Test
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest
class GeonsaehelperAiServerApplicationTests {

    @MockBean
    private lateinit var vectorStore: VectorStore

    @Test
    fun contextLoads() {
    }

}
