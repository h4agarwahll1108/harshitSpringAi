package ai.controller;

import ai.serviceImpl.chatService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api")
public class chatController {
    
    
    private final chatService chatService;

    public chatController(chatService chatService){
        this.chatService = chatService;
    }

    @GetMapping("/chat")
    public ResponseEntity<String> chat(@RequestParam(name = "q") String query){
        return ResponseEntity.ok(chatService.chat(query));
    }

    @GetMapping("/stream-chat")
    public ResponseEntity<Flux<String>> streamChat(@RequestParam(name = "q") String query){
        return ResponseEntity.ok(chatService.streamChat(query));
    }
    
    
}
