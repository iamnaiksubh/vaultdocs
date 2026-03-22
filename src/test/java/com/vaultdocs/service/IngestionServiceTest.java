package com.vaultdocs.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IngestionServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private QnAService qnAService;

    @InjectMocks
    private IngestionService ingestionService;

    @Test
    void ingest_withEmptyFile_throwsException() {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", new byte[0]);
        assertThrows(RuntimeException.class, () -> ingestionService.ingest(file));
    }

    @Test
    void ingest_withValidFile_processesAndStores() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "This is a test document content.".getBytes()
        );

        ingestionService.ingest(file);

        verify(qnAService).setFullDocumentText(anyString());
        verify(vectorStore).add(anyList());
    }
}
