package ai.serviceImpl.reader;

import ai.exception.ServiceProvisioningException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
public class JsonDocumentReader implements DocumentReader {

    private static final Logger log = LoggerFactory.getLogger(JsonDocumentReader.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<DocumentContent> read(Path filePath) {
        List<DocumentContent> contents = new ArrayList<>();

        try {
            String fileContent = Files.readString(filePath);
            JsonNode rootNode = objectMapper.readTree(fileContent);

            if (rootNode.isArray()) {
                int index = 0;
                for (JsonNode item : rootNode) {
                    extractJsonObject(item, contents, index++);
                }
            } else if (rootNode.isObject()) {
                extractJsonObject(rootNode, contents, 0);
            }

            log.info("Successfully extracted {} JSON objects from file: {}", contents.size(), filePath.getFileName());

        } catch (IOException e) {
            log.error("Error reading JSON file: {}", filePath, e);
            throw new ServiceProvisioningException("Failed to read JSON file: " + e.getMessage());
        }

        return contents;
    }

    private void extractJsonObject(JsonNode node, List<DocumentContent> contents, int index) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "json");
        metadata.put("objectIndex", index);

        StringBuilder sb = new StringBuilder();

        Iterator<String> fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            JsonNode fieldValue = node.get(fieldName);

            sb.append(fieldName).append(": ");

            if (fieldValue.isTextual()) {
                sb.append(fieldValue.asText());
            } else if (fieldValue.isNumber()) {
                sb.append(fieldValue.asText());
            } else if (fieldValue.isBoolean()) {
                sb.append(fieldValue.asBoolean());
            } else {
                sb.append(fieldValue.asText());
            }

            sb.append(" | ");

            metadata.put(fieldName, fieldValue.asText());
        }

        if (sb.length() > 0) {
            contents.add(new DocumentContent(sb.toString(), metadata));
        }
    }
}
