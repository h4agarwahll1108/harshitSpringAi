package ai.serviceImpl;

import ai.dto.ChunkedContent;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TextChunkingService {

    private static final Logger log = LoggerFactory.getLogger(TextChunkingService.class);

    /**
     * Split text into chunks with configurable overlap
     * @param text The text to chunk
     * @param chunkSize Target size of each chunk in characters
     * @param overlap Number of characters to overlap between chunks
     * @param metadata Original metadata to preserve
     * @return List of chunked content with metadata
     */
    public List<ChunkedContent> chunkText(String text, int chunkSize, int overlap, Map<String, Object> metadata) {
        List<ChunkedContent> chunks = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return chunks;
        }

        int chunkIndex = 0;
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());

            // Try to break at sentence boundary
            if (end < text.length()) {
                int lastPeriod = text.lastIndexOf(".", end);
                int lastNewline = text.lastIndexOf("\n", end);
                int breakPoint = Math.max(lastPeriod, lastNewline);

                if (breakPoint > start + chunkSize / 2) {
                    end = breakPoint + 1;
                }
            }

            String chunkContent = text.substring(start, end).trim();

            if (!chunkContent.isEmpty()) {
                Map<String, Object> chunkMetadata = new HashMap<>(metadata);
                chunkMetadata.put("chunkIndex", chunkIndex);
                chunkMetadata.put("chunkSize", chunkContent.length());

                ChunkedContent chunked = new ChunkedContent();
                chunked.setContent(chunkContent);
                chunked.setMetadata(chunkMetadata);
                chunks.add(chunked);
                chunkIndex++;
            }

            // Move start position with overlap
            start = Math.max(start + 1, end - overlap);
        }

        log.info("Split text into {} chunks with size={}, overlap={}", chunks.size(), chunkSize, overlap);

        return chunks;
    }

    /**
     * Chunk JSON documents - each JSON object becomes a chunk
     */
    public List<ChunkedContent> chunkJsonObjects(List<ChunkedContent> jsonContents) {
        // For JSON, we treat each object as an individual chunk
        // so we just return them as-is
        log.info("JSON objects will be treated as individual chunks: count={}", jsonContents.size());
        return jsonContents;
    }
}
