package com.vaultdocs.config;

import com.subhashish.aimoderationclient.validation.ModerationValidator;
import com.subhashish.aimoderationclient.validation.ModerationValidatorImpl;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfig {

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    @Bean
    public ModerationValidator moderationValidator() {
        return new ModerationValidatorImpl();
    }
}