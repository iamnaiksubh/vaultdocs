package com.vaultdocs.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.Builder;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QnAServiceTest {

    @Mock
    private Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private VectorStore vectorStore;

    private QnAService qnAService;

    @BeforeEach
    void setUp() {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        qnAService = new QnAService(chatClientBuilder, vectorStore);
    }

    @Test
    void ask_withNoDocument_returnsErrorMessage() {
        String result = qnAService.ask("test question", true);
        assertEquals("⚠️ No document loaded. Please upload a document first.", result);
    }

    @Test
    void ask_withQuestion_usesVectorStoreAndChatClient() {
        qnAService.setFullDocumentText("Document content");
        
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(new Document("Relevant chunk")));

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Mocked answer");

        String result = qnAService.ask("test question", true);

        assertEquals("Mocked answer", result);
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
        verify(chatClient).prompt();
    }

    @Test
    void ask_withOperation_usesChatClientWithoutVectorStore() {
        qnAService.setFullDocumentText("Document content");

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(any(Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn("Mocked operation result");

        String result = qnAService.ask("test operation", false);

        assertEquals("Mocked operation result", result);
        verify(vectorStore, never()).similaritySearch(any(SearchRequest.class));
        verify(chatClient).prompt();
    }
}
