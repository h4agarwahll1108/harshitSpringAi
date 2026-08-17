package ai.controller;

import ai.dto.ChatRequest;
import ai.dto.CustomUserPrincipal;
import ai.security.CustomUserDetailsService;
import ai.serviceImpl.chatService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api")
public class chatController {


    private final chatService chatService;

    public chatController(chatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/chat")
    public ResponseEntity<String> chat(@RequestParam(name = "q") String query) {
        return ResponseEntity.ok(chatService.chat(query));
    }

    @GetMapping("/stream-chat")
    public ResponseEntity<Flux<String>> streamChat(@RequestParam(name = "q") String query) {
        return ResponseEntity.ok(chatService.streamChat(query));
    }

    @GetMapping("/memory-chat")
    public ResponseEntity<?> memoryChat(@RequestBody ChatRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
        String userId = principal.getUsername();
        return ResponseEntity.ok(chatService.memoryChat(userId, request.getConversationId(), request.getMessage()));
    }

    @GetMapping("/memory-chat-stream")
    public ResponseEntity<Flux<String>> memoryChatStream(@RequestBody ChatRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
        String userId = principal.getUsername();
        return ResponseEntity.ok(chatService.streamChat(userId, request.getConversationId(), request.getMessage()));
    }


}
