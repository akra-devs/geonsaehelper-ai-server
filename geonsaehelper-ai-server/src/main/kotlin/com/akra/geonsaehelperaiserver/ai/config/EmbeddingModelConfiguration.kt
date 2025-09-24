package com.akra.geonsaehelperaiserver.ai.config

import com.akra.geonsaehelperaiserver.vector.VectorStoreProperties
import org.slf4j.LoggerFactory
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class EmbeddingModelConfiguration(
    private val applicationContext: ApplicationContext,
    private val vectorStoreProperties: VectorStoreProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    @Primary
    fun primaryEmbeddingModel(): EmbeddingModel {
        val beanName = vectorStoreProperties.embeddingBean
        if (!applicationContext.containsBean(beanName)) {
            val available = applicationContext.getBeansOfType(EmbeddingModel::class.java).keys.joinToString()
            error(
                "EmbeddingModel bean '$beanName' is not available. Configure app.ai.vectorstore.embedding-bean to one of: $available"
            )
        }

        val embeddingModel = applicationContext.getBean(beanName, EmbeddingModel::class.java)
        logger.info("Using '{}' as primary EmbeddingModel for vector store integration", beanName)
        return embeddingModel
    }
}
