package com.vaultdocs.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class QnAService {
    private final ChatClient chatClient;
    private final VectorStore vectorStore;

    private String fullDocumentText = "";

    public QnAService(ChatClient.Builder builder, org.springframework.ai.vectorstore.VectorStore vectorStore) {
        this.chatClient = builder.build();
        this.vectorStore = vectorStore;
    }

    public void setFullDocumentText(String text) {
        this.fullDocumentText = text;
    }

    public String ask(String question, boolean hasQuestion) {
        if (hasQuestion)
            return handleQnA(question);

        return handleOperation(question);
    }

    private String handleOperation(String question) {
        if (fullDocumentText == null || fullDocumentText.isBlank()) {
            return "⚠️ No document loaded. Please upload a document first.";
        }

        return chatClient.prompt()
                .user(u -> u.text("""
                    You are VaultDocs, an intelligent document assistant.
                    You are given the full content of a document.
                    Perform the requested operation carefully and accurately.
                    
                    OPERATIONS YOU CAN PERFORM:
                    - MATH: sum, average, count, min, max of numeric data/tables
                    - SUMMARY: summarize the whole document clearly
                    - ATS SCORE: if user provides a job description, score the resume (0-100) with feedback
                    - EXTRACT: extract specific data like names, dates, emails, phone numbers
                    - TRANSLATE: translate document content to requested language
                    - ANALYZE: provide detailed analysis of the document
                    - COMPARE: compare sections or data points in the document
                    
                    RULES:
                    - Be precise and accurate
                    - For math operations, show your working/calculation
                    - For ATS score, give score + strengths + weaknesses + suggestions
                    - For summary, be concise but cover key points
                    - Never make up data not present in the document
                    
                    FULL DOCUMENT CONTENT:
                    {document}
                    
                    USER REQUEST: {question}
                    
                    YOUR RESPONSE:
                    """)
                        .param("document", fullDocumentText)
                        .param("question", question))
                .call()
                .content();
    }


    private String handleQnA(String question) {
        if (fullDocumentText == null || fullDocumentText.isBlank()) {
            return "⚠️ No document loaded. Please upload a document first.";
        }

        List<Document> relevantChunks = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(6)
                        .similarityThreshold(0.5)
                        .build()
        );

        String context = relevantChunks.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));

        return chatClient.prompt()
                .user(u -> u.text("""
                    You are VaultDocs, an intelligent document assistant.
                    Your job is to help users understand uploaded documents.
                    
                    INSTRUCTIONS:
                    - Single word or short phrase → explain from document context
                    - Question → answer directly from document
                    - Topic/concept → summarize what the document says about it
                    - Partial info found → share what you found, mention it may be incomplete
                    - Not found in context → say: "This topic is not covered in the document."
                    - Unclear input → say: "I didn't understand. Please rephrase."
                    - Never make up information or use outside knowledge
                    
                    CONTEXT FROM DOCUMENT:
                    {context}
                    
                    USER INPUT: {question}
                    
                    YOUR ANSWER:
                    """)
                        .param("context", context)
                        .param("question", question))
                .call()
                .content();
    }

}
