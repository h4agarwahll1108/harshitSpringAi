package ai.serviceImpl;

import ai.dto.RagResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomRagService {

    private static final Logger log = LoggerFactory.getLogger(CustomRagService.class);

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    private static final int TOP_K = 5;
    private static final double SIMILARITY_THRESHOLD = 0.5;

    /**
     * Answer a question using RAG with user-specific context filtering
     */
    public RagResponse answerQuestion(Long userId, String question) {
        log.info("Processing RAG question for userId: {}, question: {}", userId, question);

        try {
            // Step 1: Search for relevant documents
            List<Document> relevantDocs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(question)
                            .topK(TOP_K)
                            .build()
            );

            // Step 2: Filter by user ID from metadata
            List<Document> userDocuments = relevantDocs.stream()
                    .filter(doc -> userId.equals(doc.getMetadata().get("userId")))
                    .collect(Collectors.toList());

            log.info("Found {} relevant documents for user {}", userDocuments.size(), userId);

            if (userDocuments.isEmpty()) {
                return RagResponse.builder()
                        .answer("I could not find relevant information in your documents to answer this question.")
                        .sources(List.of())
                        .relevantChunks(0)
                        .build();
            }

            // Step 3: Build context from retrieved documents
            String context = buildContext(userDocuments);

            // Step 4: Generate answer using LLM with context
            String answer = generateAnswer(question, context);

            // Step 5: Extract sources
            List<RagResponse.Source> sources = extractSources(userDocuments);

            return RagResponse.builder()
                    .answer(answer)
                    .sources(sources)
                    .relevantChunks(userDocuments.size())
                    .build();

        } catch (Exception e) {
            log.error("Error in RAG question answering", e);
            return RagResponse.builder()
                    .answer("An error occurred while processing your question. Please try again.")
                    .sources(List.of())
                    .relevantChunks(0)
                    .build();
        }
    }

    private String buildContext(List<Document> documents) {
        StringBuilder context = new StringBuilder();
        context.append("Based on the following documents:\n\n");

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            context.append("Document ").append(i + 1).append(":\n");
            context.append(doc.getText()).append("\n\n");
        }

        return context.toString();
    }

    private String generateAnswer(String question, String context) {
        String prompt = String.format("""
                You are a helpful assistant. Answer the following question based ONLY on the provided context.
                If the answer is not in the context, clearly state that you don't have that information.
                
                Context:
                %s
                
                Question: %s
                
                Answer:
                """, context, question);

        try {
            String answer = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            return answer;
        } catch (Exception e) {
            log.error("Error generating answer with LLM", e);
            return "Unable to generate answer at this time.";
        }
    }

    private List<RagResponse.Source> extractSources(List<Document> documents) {
        return documents.stream()
                .map(doc -> RagResponse.Source.builder()
                        .documentId(((Number) doc.getMetadata().get("documentId")).longValue())
                        .fileName((String) doc.getMetadata().get("fileName"))
                        .pageNumber(doc.getMetadata().get("pageNumber") != null ?
                                ((Number) doc.getMetadata().get("pageNumber")).intValue() : 0)
                        .chunkIndex(doc.getMetadata().get("chunkIndex") != null ?
                                ((Number) doc.getMetadata().get("chunkIndex")).intValue() : 0)
                        .build())
                .distinct()
                .collect(Collectors.toList());
    }
}
