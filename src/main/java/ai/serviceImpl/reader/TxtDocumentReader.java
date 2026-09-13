package ai.serviceImpl.reader;

import ai.exception.ServiceProvisioningException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TxtDocumentReader implements DocumentReader {

    private static final Logger log = LoggerFactory.getLogger(TxtDocumentReader.class);

    @Override
    public List<DocumentContent> read(Path filePath) {
        List<DocumentContent> contents = new ArrayList<>();

        try {
            String text = Files.readString(filePath, StandardCharsets.UTF_8);

            if (!text.trim().isEmpty()) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("source", "txt");
                metadata.put("lineCount", text.split("\n").length);

                contents.add(new DocumentContent(text, metadata));
            }

            log.info("Successfully read TXT file: {}", filePath.getFileName());

        } catch (IOException e) {
            log.error("Error reading TXT file: {}", filePath, e);
            throw new ServiceProvisioningException("Failed to read TXT file: " + e.getMessage());
        }

        return contents;
    }
}
