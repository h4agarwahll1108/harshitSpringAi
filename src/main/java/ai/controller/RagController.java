package ai.controller;

import ai.dto.RagResponse;
import ai.serviceImpl.CustomRagService;
import ai.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final CustomRagService ragService;
    private final SecurityUtils securityUtils;

    @PostMapping("/ask")
    public ResponseEntity<RagResponse> askQuestion(@RequestParam String question) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(ragService.answerQuestion(userId, question));
    }
}
