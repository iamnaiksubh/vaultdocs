package com.vaultdocs.controller;

import com.subhashish.aimoderationclient.model.ModerationResult;
import com.subhashish.aimoderationclient.validation.ModerationValidator;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.ai.ollama.init.pull-model-strategy=never")
@AutoConfigureMockMvc
public class VaultDocsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VectorStore vectorStore;

    @MockitoBean
    private ChatModel chatModel;

    @MockitoBean
    private EmbeddingModel embeddingModel;

    @MockitoBean
    private ModerationValidator moderationValidator;

    @Test
    public void testFullFlow_UploadAndAsk() throws Exception {
        ModerationResult mockResult = mock(ModerationResult.class);
        when(mockResult.flagged()).thenReturn(false);
        when(moderationValidator.validate(anyString())).thenReturn(mockResult);

        ChatResponse mockChatResponse = new ChatResponse(List.of(new Generation(new AssistantMessage("This is a mock answer from LLM"))));
        when(chatModel.call(any(Prompt.class))).thenReturn(mockChatResponse);

        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(new org.springframework.ai.document.Document("Mocked document content")));

        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "This is a test document about VaultDocs integration.".getBytes());
        mockMvc.perform(multipart("/vaultdocs/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("✅ Document uploaded and indexed successfully!")));

        mockMvc.perform(get("/vaultdocs/ask").param("question", "What is this document about?"))
                .andExpect(status().isOk())
                .andExpect(content().string("This is a mock answer from LLM"));

        mockMvc.perform(get("/vaultdocs/ask").param("operation", "SUMMARY"))
                .andExpect(status().isOk())
                .andExpect(content().string("This is a mock answer from LLM"));
    }

    @Test
    public void testAsk_InputFlaggedByModeration() throws Exception {
        ModerationResult mockResult = mock(ModerationResult.class);
        when(mockResult.flagged()).thenReturn(true);
        when(mockResult.violations()).thenReturn(List.of("Inappropriate content"));
        when(moderationValidator.validate(anyString())).thenReturn(mockResult);

        mockMvc.perform(get("/vaultdocs/ask").param("question", "bad words"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Input flagged by moderation: Inappropriate content")));
    }
}