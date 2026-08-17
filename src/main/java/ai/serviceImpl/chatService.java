package ai.serviceImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class chatService {

    private final ChatClient chatClient;
    private final Logger log = LoggerFactory.getLogger(chatService.class);

    public chatService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String chat(String message) {
        return chatClient
                .prompt()
                .user(message)
                .call()
                .content();
    }

    public Flux<String> streamChat(String message) {
        return chatClient
                .prompt()
                .user(message)
                .stream()
                .content()
                .filter(token -> !token.isBlank())
//                .map(String::trim)
                .doOnNext(token -> log.debug("AI token received: {}", token))
                .doOnComplete(() -> log.info("AI streaming response completed"))
                .doOnError(error -> log.error("Error while streaming AI response", error)
                );
    }

    public String memoryChat(String userId, String conversationId, String message) {
        String conversation = userId + "-" + conversationId;
        return chatClient
                .prompt()
                .user(message)
                .advisors(a ->
                        a.param(
                                ChatMemory.CONVERSATION_ID,
                                conversation
                        ))
                .call()
                .content();
    }

    public Flux<String> streamChat(String userId, String conversationId, String message) {
        String conversation = userId + "-" + conversationId;
        return chatClient
                .prompt()
                .user(message)
                .advisors(a ->
                        a.param(
                                ChatMemory.CONVERSATION_ID,
                                conversation
                        ))
                .stream()
                .content();
    }
}
