//package com.akra.geonsaehelperaiserver.chunk
//
//import com.fasterxml.jackson.databind.ObjectMapper
//import org.assertj.core.api.Assertions.assertThat
//import org.junit.jupiter.api.Test
//import org.mockito.Mockito
//import org.springframework.ai.chat.messages.AssistantMessage
//import org.springframework.ai.chat.messages.UserMessage
//import org.springframework.ai.chat.model.ChatResponse
//import org.springframework.ai.chat.model.Generation
//import org.springframework.ai.chat.model.ChatModel
//import org.springframework.ai.chat.prompt.Prompt
//
//class SemanticChunkServiceTest {
//
//    private val chatModel: ChatModel = Mockito.mock(ChatModel::class.java)
//    private val objectMapper = ObjectMapper()
//    private val service = SemanticChunkService(chatModel, objectMapper)
//
//    @Test
//    fun `returns empty list for blank text`() {
//        val chunks = service.chunkText("    ")
//        assertThat(chunks).isEmpty()
//    }
//
//    @Test
//    fun `parses chat response into chunks`() {
//        val text = "paragraph one. paragraph two."
//        val returnedJson = "[ \"chunk1\", \"chunk2\" ]"
//
//        Mockito.`when`(chatModel.call(Mockito.any(Prompt::class.java))).thenAnswer {
//            val prompt = it.arguments[0] as Prompt
//            val messages = prompt.messages
//            val userMessage = messages.last() as UserMessage
//            assertThat(userMessage.content).contains("chunk_size_hint")
//
//            ChatResponse(listOf(Generation(AssistantMessage(returnedJson))))
//        }
//
//        val result = service.chunkText(text)
//
//        assertThat(result).containsExactly("chunk1", "chunk2")
//    }
//
//    @Test
//    fun `throws when response is not json`() {
//        val text = "some text"
//
//        Mockito.`when`(chatModel.call(Mockito.any(Prompt::class.java))).thenReturn(
//            ChatResponse(listOf(Generation(AssistantMessage("not json"))))
//        )
//
//        org.junit.jupiter.api.assertThrows<IllegalStateException> {
//            service.chunkText(text)
//        }
//    }
//}
