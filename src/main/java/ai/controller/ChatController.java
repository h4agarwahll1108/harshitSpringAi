package ai.controller;

import ai.dto.ChatRequest;
import ai.dto.ChatResponse;
import ai.serviceImpl.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatService.chat(request));
    }

    @PostMapping("/stream")
    public ResponseEntity<Flux<String>> streamChat(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatService.streamChat(request));
    }
}

