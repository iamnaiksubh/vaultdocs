package com.vaultdocs.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
    private final VectorStore vectorStore;
    private final QnAService qnAService;

    public IngestionService(VectorStore vectorStore, QnAService qnAService) {
        this.vectorStore = vectorStore;
        this.qnAService = qnAService;
    }

    public void ingest(MultipartFile file) throws IOException {
        var  resource = new InputStreamResource(file.getInputStream());
        var tikaReader = new TikaDocumentReader(resource);
        List<Document> docs = tikaReader.get();

        String fullText = docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));
        qnAService.setFullDocumentText(fullText);

        var splitter = new TokenTextSplitter(
                300,
                50,
                50,
                10000,
                true,
                List.of('.', '?', '!', '\n')
        );

        List<Document> chunks = splitter.apply(docs);

        vectorStore.add(chunks);

        log.info("Ingested {} chunks from: {}", docs.size(), file.getOriginalFilename());
    }

}
