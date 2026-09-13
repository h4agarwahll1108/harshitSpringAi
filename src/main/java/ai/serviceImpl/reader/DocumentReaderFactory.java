package ai.serviceImpl.reader;

import ai.exception.ServiceProvisioningException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentReaderFactory {

    private final PdfDocumentReader pdfReader;
    private final DocxDocumentReader docxReader;
    private final TxtDocumentReader txtReader;
    private final JsonDocumentReader jsonReader;

    public DocumentReader getReader(String fileExtension) {
        return switch (fileExtension.toLowerCase()) {
            case "pdf" -> pdfReader;
            case "docx" -> docxReader;
            case "txt" -> txtReader;
            case "json" -> jsonReader;
            default -> throw new ServiceProvisioningException("Unsupported file type: " + fileExtension);
        };
    }
}
