package com.vaultdocs.controller;

import com.subhashish.aimoderationclient.model.ModerationResult;
import com.subhashish.aimoderationclient.validation.ModerationValidator;
import com.vaultdocs.service.IngestionService;
import com.vaultdocs.service.QnAService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/vaultdocs")
public class VaultDocsController {
    
    private final IngestionService ingestionService;
    private final QnAService qnAService;
    private final ModerationValidator moderationValidator;

    public VaultDocsController(IngestionService ingestionService, QnAService qnAService, ModerationValidator moderationValidator) {
        this.ingestionService = ingestionService;
        this.qnAService = qnAService;
        this.moderationValidator = moderationValidator;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        try {
            ingestionService.ingest(file);
            return ResponseEntity.ok("✅ Document uploaded and indexed successfully!");
        } catch (IOException e) {
            return ResponseEntity.internalServerError()
                    .body("Failed to process file: " + e.getMessage());
        }
    }

    @GetMapping("/ask")
    public ResponseEntity<String> ask(
            @RequestParam(required = false) String question,
            @RequestParam(required = false) String operation) {

        boolean hasQuestion = question != null && !question.trim().isEmpty();
        boolean hasOperation = operation != null && !operation.trim().isEmpty();

        if (hasQuestion && hasOperation) {
            return ResponseEntity.badRequest().body("Please provide either 'question' or 'operation', not both.");
        }

        if (!hasQuestion && !hasOperation) {
            return ResponseEntity.badRequest().body("Please provide either a 'question' or an 'operation'.");
        }

        String input = hasQuestion ? question : operation;
        
        try {
            ModerationResult result = moderationValidator.validate(input);

            if (result.flagged()) {
                return ResponseEntity.badRequest().body("Input flagged by moderation: " + result.violations().get(0));
            }
            
            String answer = qnAService.ask(input, hasQuestion);
            return ResponseEntity.ok(answer);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Service configuration error (check API keys/URLs): " + e.getMessage());
        }
    }
}