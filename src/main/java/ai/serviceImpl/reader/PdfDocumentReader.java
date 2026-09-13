package ai.serviceImpl.reader;

import ai.exception.ServiceProvisioningException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PdfDocumentReader implements DocumentReader {

    private static final Logger log = LoggerFactory.getLogger(PdfDocumentReader.class);

    @Override
    public List<DocumentContent> read(Path filePath) {
        List<DocumentContent> contents = new ArrayList<>();

        try {
            File file = filePath.toFile();
            PDDocument document = Loader.loadPDF(file);
            PDFTextStripper stripper = new PDFTextStripper();
            int pageCount = document.getNumberOfPages();

            for (int pageNum = 1; pageNum <= pageCount; pageNum++) {
                stripper.setStartPage(pageNum);
                stripper.setEndPage(pageNum);

                String pageText = stripper.getText(document);

                if (!pageText.trim().isEmpty()) {
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("pageNumber", pageNum);
                    metadata.put("totalPages", pageCount);
                    metadata.put("source", "pdf");

                    contents.add(new DocumentContent(pageText, metadata));
                }
            }

            document.close();
            log.info("Successfully extracted {} pages from PDF: {}", pageCount, filePath.getFileName());

        } catch (IOException e) {
            log.error("Error reading PDF file: {}", filePath, e);
            throw new ServiceProvisioningException("Failed to read PDF file: " + e.getMessage());
        }

        return contents;
    }
}
