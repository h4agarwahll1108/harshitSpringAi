package ai.serviceImpl;

import ai.dto.ChunkedContent;
import ai.model.DocumentEntity;
import ai.serviceImpl.reader.DocumentReader;
import ai.serviceImpl.reader.DocumentReaderFactory;
import ai.utils.DocumentStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentEmbeddingService.class);

    private final DocumentReaderFactory readerFactory;
    private final TextChunkingService chunkingService;
    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;

    private static final int CHUNK_SIZE = 1000;
    private static final int CHUNK_OVERLAP = 100;

    /**
     * Complete pipeline: Extract → Chunk → Embed → Store
     */
    public void processAndIndexDocument(DocumentEntity document, Path filePath, String fileExtension) {
        try {
            log.info("Starting document processing pipeline for: {}", document.getFileName());

            // Step 1: Read document
            DocumentReader reader = readerFactory.getReader(fileExtension);
            List<DocumentReader.DocumentContent> extractedContents = reader.read(filePath);
            log.info("Extracted {} sections from document", extractedContents.size());

            // Step 2: Chunk and embed
            List<Document> documentsToStore = new ArrayList<>();

            for (DocumentReader.DocumentContent content : extractedContents) {
                Map<String, Object> baseMetadata = new HashMap<>(content.metadata());
                baseMetadata.put("documentId", document.getId());
                baseMetadata.put("userId", document.getUserId());
                baseMetadata.put("fileName", document.getFileName());
                baseMetadata.put("fileType", document.getFileType());

                List<ChunkedContent> chunks = chunkingService.chunkText(
                        content.content(),
                        CHUNK_SIZE,
                        CHUNK_OVERLAP,
                        baseMetadata
                );

                for (ChunkedContent chunk : chunks) {
                    Document doc = new Document(chunk.getContent(), chunk.getMetadata());
                    documentsToStore.add(doc);
                }
            }

            log.info("Created {} chunks from document", documentsToStore.size());

            // Step 3: Add to vector store
            if (!documentsToStore.isEmpty()) {
                vectorStore.add(documentsToStore);
                log.info("Successfully indexed {} chunks to vector store", documentsToStore.size());
            }

        } catch (Exception e) {
            log.error("Error processing document: {}", document.getFileName(), e);
            throw new RuntimeException("Document processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Delete all embeddings for a document from vector store
     */
    public void deleteDocumentEmbeddings(Long documentId) {
        // Note: PGVector doesn't have a direct delete by metadata feature
        // We'll handle this with a custom repository query or by marking documents
        log.info("Embeddings deletion requested for document: {}", documentId);
    }
}
