package ai.serviceImpl.reader;

import ai.exception.ServiceProvisioningException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class DocxDocumentReader implements DocumentReader {

    private static final Logger log = LoggerFactory.getLogger(DocxDocumentReader.class);

    @Override
    public List<DocumentContent> read(Path filePath) {
        List<DocumentContent> contents = new ArrayList<>();

        try (XWPFDocument document = new XWPFDocument(filePath.toUri().toURL().openStream())) {
            StringBuilder documentText = new StringBuilder();
            int paragraphCount = 0;

            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String paragraphText = paragraph.getText();
                if (!paragraphText.trim().isEmpty()) {
                    documentText.append(paragraphText).append("\n");
                    paragraphCount++;
                }
            }

            if (documentText.length() > 0) {
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("paragraphCount", paragraphCount);
                metadata.put("source", "docx");

                contents.add(new DocumentContent(documentText.toString(), metadata));
            }

            log.info("Successfully extracted {} paragraphs from DOCX: {}", paragraphCount, filePath.getFileName());

        } catch (IOException e) {
            log.error("Error reading DOCX file: {}", filePath, e);
            throw new ServiceProvisioningException("Failed to read DOCX file: " + e.getMessage());
        }

        return contents;
    }
}
