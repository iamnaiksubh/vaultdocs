package com.vaultdocs.controller;

import com.subhashish.aimoderationclient.model.ModerationResult;
import com.subhashish.aimoderationclient.validation.ModerationValidator;
import com.vaultdocs.service.IngestionService;
import com.vaultdocs.service.QnAService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VaultDocsController.class)
public class VaultDocsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IngestionService ingestionService;

    @MockitoBean
    private QnAService qnAService;

    @MockitoBean
    private ModerationValidator moderationValidator;

    @Test
    public void testUpload_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "dummy content".getBytes());
        
        mockMvc.perform(multipart("/vaultdocs/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(content().string("✅ Document uploaded and indexed successfully!"));
    }

    @Test
    public void testUpload_Failure() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "dummy content".getBytes());
        doThrow(new IOException("Disk space full")).when(ingestionService).ingest(any());
        
        mockMvc.perform(multipart("/vaultdocs/upload").file(file))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Failed to process file: Disk space full")));
    }

    @Test
    public void testAsk_BothQuestionAndOperationProvided_ShouldReturn400() throws Exception {
        mockMvc.perform(get("/vaultdocs/ask")
                        .param("question", "What is this?")
                        .param("operation", "SUMMARY"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Please provide either 'question' or 'operation', not both.")));
    }

    @Test
    public void testAsk_NeitherQuestionNorOperationProvided_ShouldReturn400() throws Exception {
        mockMvc.perform(get("/vaultdocs/ask"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Please provide either a 'question' or an 'operation'.")));
    }

    @Test
    public void testAsk_InputFlaggedByModeration_ShouldReturn400() throws Exception {
        ModerationResult mockResult = mock(ModerationResult.class);
        when(mockResult.flagged()).thenReturn(true);
        when(mockResult.violations()).thenReturn(List.of("Inappropriate content"));
        when(moderationValidator.validate(anyString())).thenReturn(mockResult);

        mockMvc.perform(get("/vaultdocs/ask").param("question", "bad question"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Input flagged by moderation: Inappropriate content")));
    }

    @Test
    public void testAsk_ValidQuestion_ShouldReturnAnswer() throws Exception {
        ModerationResult mockResult = mock(ModerationResult.class);
        when(mockResult.flagged()).thenReturn(false);
        when(moderationValidator.validate("What is the main topic?")).thenReturn(mockResult);
        when(qnAService.ask("What is the main topic?", true)).thenReturn("The main topic is AI.");

        mockMvc.perform(get("/vaultdocs/ask").param("question", "What is the main topic?"))
                .andExpect(status().isOk())
                .andExpect(content().string("The main topic is AI."));
    }

    @Test
    public void testAsk_ValidOperation_ShouldReturnAnswer() throws Exception {
        ModerationResult mockResult = mock(ModerationResult.class);
        when(mockResult.flagged()).thenReturn(false);
        when(moderationValidator.validate("SUMMARY")).thenReturn(mockResult);
        when(qnAService.ask("SUMMARY", false)).thenReturn("Document summary.");

        mockMvc.perform(get("/vaultdocs/ask").param("operation", "SUMMARY"))
                .andExpect(status().isOk())
                .andExpect(content().string("Document summary."));
    }

    @Test
    public void testAsk_WhenKeyOrUrlMissing_ShouldReturn500AndHandledError() throws Exception {
        when(moderationValidator.validate(anyString()))
                .thenThrow(new IllegalArgumentException("API key or URL must not be null"));

        mockMvc.perform(get("/vaultdocs/ask").param("question", "What is the document about?"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Service configuration error (check API keys/URLs)")));
    }
}