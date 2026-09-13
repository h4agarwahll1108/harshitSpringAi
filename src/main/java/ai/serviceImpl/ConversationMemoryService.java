package ai.serviceImpl;

import ai.model.ChatMessage;
import ai.repository.ChatMessageRepository;
import ai.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationMemoryService {

    private static final Logger log = LoggerFactory.getLogger(ConversationMemoryService.class);
    private static final int MAX_MEMORY_MESSAGES = 20;
    private static final int CONTEXT_WINDOW_MESSAGES = 10;
    private final ChatMessageRepository chatMessageRepository;
    private final SecurityUtils securityUtils;

    /**
     * Create a new conversation
     */
    public String createConversation() {
        return UUID.randomUUID().toString();
    }

    /**
     * Add a message to conversation
     */
    public void addMessage(String conversationId, String content, ChatMessage.MessageRole role) {
        Long userId = securityUtils.getCurrentUserId();

        ChatMessage message = ChatMessage.builder()
                .conversationId(conversationId)
                .userId(userId)
                .role(role)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();

        chatMessageRepository.save(message);
        log.info("Message added to conversation: {} for user: {}", conversationId, userId);

        // Implement message retention policy
        pruneOldMessages(conversationId, userId);
    }

    /**
     * Get conversation history for context
     * Returns recent messages for including in LLM context
     */
    public String getConversationContext(String conversationId) {
        Long userId = securityUtils.getCurrentUserId();

        List<ChatMessage> messages = chatMessageRepository.findByConversationIdAndUserId(conversationId, userId);

        if (messages.isEmpty()) {
            return "";
        }

        // Get last CONTEXT_WINDOW_MESSAGES messages
        List<ChatMessage> recentMessages = messages.stream()
                .skip(Math.max(0, messages.size() - CONTEXT_WINDOW_MESSAGES))
                .collect(Collectors.toList());

        StringBuilder context = new StringBuilder();
        context.append("Previous conversation:\n");

        for (ChatMessage msg : recentMessages) {
            context.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }

        return context.toString();
    }

    /**
     * Get full conversation history
     */
    public List<ChatMessage> getConversationHistory(String conversationId) {
        Long userId = securityUtils.getCurrentUserId();
        return chatMessageRepository.findByConversationIdAndUserId(conversationId, userId);
    }

    /**
     * Delete conversation
     */
    public void deleteConversation(String conversationId) {
        Long userId = securityUtils.getCurrentUserId();
        chatMessageRepository.deleteByConversationIdAndUserId(conversationId, userId);
        log.info("Conversation deleted: {} for user: {}", conversationId, userId);
    }

    /**
     * Prune old messages to prevent memory overflow
     */
    private void pruneOldMessages(String conversationId, Long userId) {
        List<ChatMessage> messages = chatMessageRepository.findByConversationIdAndUserId(conversationId, userId);

        if (messages.size() > MAX_MEMORY_MESSAGES) {
            // Delete oldest messages
            int toDelete = messages.size() - MAX_MEMORY_MESSAGES;
            List<ChatMessage> oldestMessages = messages.stream()
                    .limit(toDelete)
                    .collect(Collectors.toList());

            chatMessageRepository.deleteAll(oldestMessages);
            log.info("Pruned {} old messages from conversation: {}", toDelete, conversationId);
        }
    }
}
