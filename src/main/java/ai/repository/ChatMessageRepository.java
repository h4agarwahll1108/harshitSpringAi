package ai.repository;

import ai.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT m FROM ChatMessage m WHERE m.conversationId = :conversationId AND m.userId = :userId ORDER BY m.createdAt ASC")
    List<ChatMessage> findByConversationIdAndUserId(@Param("conversationId") String conversationId, @Param("userId") Long userId);

    @Query("SELECT DISTINCT m.conversationId FROM ChatMessage m WHERE m.userId = :userId ORDER BY (SELECT MAX(cm.createdAt) FROM ChatMessage cm WHERE cm.conversationId = m.conversationId) DESC")
    List<String> findConversationsByUserId(@Param("userId") Long userId);

    void deleteByConversationIdAndUserId(String conversationId, Long userId);
}
