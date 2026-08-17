package ai.serviceImpl;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VectorService {

    private final VectorStore vectorStore;

    public void addDocument() {

        Document document = new Document("Spring Boot is a framework built on top of Spring. " + "It simplifies configuration and application development.", Map.of("source", "spring-boot-guide", "category", "spring"));

        vectorStore.add(List.of(document));
    }

    public void loadKnowledge() {

        List<Document> documents = List.of(

                new Document("""
                        Spring Boot provides auto-configuration,
                        dependency injection, embedded servers,
                        and production-ready features.
                        """, Map.of("source", "spring", "topic", "spring-boot")),

                new Document("""
                        Spring Security provides authentication
                        and authorization for Spring applications.
                        JWT can be used for stateless authentication.
                        """, Map.of("source", "spring-security", "topic", "security")),

                new Document("""
                        Spring Data JPA provides repository abstractions
                        for working with relational databases using JPA.
                        """, Map.of("source", "spring-data", "topic", "jpa")));

        vectorStore.add(documents);
    }

    public List<Document> search(String query) {

        return vectorStore.similaritySearch(SearchRequest.builder().query(query).topK(3).build());
    }
}
