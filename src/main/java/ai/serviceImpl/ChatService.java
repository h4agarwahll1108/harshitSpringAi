package ai.serviceImpl;

import ai.dto.ChatRequest;
import ai.dto.ChatResponse;
import ai.model.ChatMessage;
import ai.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient chatClient;
    private final ConversationMemoryService memoryService;
    private final CustomRagService ragService;
    private final SecurityUtils securityUtils;

    /**
     * Chat with conversation memory and optional RAG
     */
    public ChatResponse chat(ChatRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        String conversationId = request.getConversationId();

        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = memoryService.createConversation();
        }

        String finalConversationId = conversationId;

        try {
            // Get conversation context
            String memoryContext = memoryService.getConversationContext(finalConversationId);

            // Build prompt
            String prompt = buildPrompt(request.getMessage(), memoryContext, request.isUseRag() ? userId : null);

            // Call LLM
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            // Save messages to memory
            memoryService.addMessage(finalConversationId, request.getMessage(), ChatMessage.MessageRole.USER);
            memoryService.addMessage(finalConversationId, response, ChatMessage.MessageRole.ASSISTANT);

            log.info("Chat completed for user: {} in conversation: {}", userId, finalConversationId);

            return ChatResponse.builder()
                    .conversationId(finalConversationId)
                    .response(response)
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("Error in chat for user: {}", userId, e);
            throw new RuntimeException("Chat failed: " + e.getMessage(), e);
        }
    }

    /**
     * Stream chat response
     */
    public Flux<String> streamChat(ChatRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        String conversationId = request.getConversationId();

        if (conversationId == null || conversationId.isEmpty()) {
            conversationId = memoryService.createConversation();
        }

        String finalConversationId = conversationId;
        String memoryContext = memoryService.getConversationContext(finalConversationId);
        String prompt = buildPrompt(request.getMessage(), memoryContext, request.isUseRag() ? userId : null);

        // Save user message
        memoryService.addMessage(finalConversationId, request.getMessage(), ChatMessage.MessageRole.USER);

        return chatClient.prompt()
                .user(prompt)
                .stream()
                .content()
                .filter(token -> !token.isBlank())
                .doOnNext(token -> log.debug("Token streamed"))
                .doOnComplete(() -> {
                    log.info("Stream completed for user: {} in conversation: {}", userId, finalConversationId);
                })
                .doOnError(error -> log.error("Error during streaming", error));
    }

    private String buildPrompt(String userMessage, String memoryContext, Long userId) {
        StringBuilder prompt = new StringBuilder();

        if (!memoryContext.isEmpty()) {
            prompt.append(memoryContext).append("\n\n");
        }

        if (userId != null) {
            prompt.append("Use the user's documents for context if relevant.\n");
        }

        prompt.append("User: ").append(userMessage);

        return prompt.toString();
    }
}

