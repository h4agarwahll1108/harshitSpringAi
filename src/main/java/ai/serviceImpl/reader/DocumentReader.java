package ai.serviceImpl.reader;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface DocumentReader {
    /**
     * Extract text and metadata from a document
     * @param filePath Path to the document file
     * @return List of extracted content with metadata
     */
    List<DocumentContent> read(Path filePath);

    record DocumentContent(String content, Map<String, Object> metadata) {}
}
