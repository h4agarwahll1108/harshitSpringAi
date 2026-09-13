package ai.serviceImpl;

import ai.dto.DocumentUploadResponse;
import ai.exception.ServiceProvisioningException;
import ai.model.DocumentEntity;
import ai.repository.DocumentRepository;
import ai.utils.DocumentStatus;
import ai.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final SecurityUtils securityUtils;
    private final FileUploadService fileUploadService;
    private final DocumentEmbeddingService embeddingService;

    public DocumentUploadResponse uploadDocument(MultipartFile file) {
        Long userId = securityUtils.getCurrentUserId();
        String fileExtension = getFileExtension(file.getOriginalFilename());

        try {
            // Step 1: Save file
            String savedFilename = fileUploadService.saveFile(file);
            log.info("File saved: {} for userId: {}", savedFilename, userId);

            // Step 2: Create document metadata
            DocumentEntity document = DocumentEntity.builder()
                    .userId(userId)
                    .fileName(file.getOriginalFilename())
                    .fileType(fileExtension)
                    .fileSize(file.getSize())
                    .status(DocumentStatus.UPLOADED)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            document = documentRepository.save(document);
            log.info("Document metadata saved with id: {}", document.getId());

            // Step 3: Process and index document
            try {
                Path filePath = fileUploadService.getFilePath(savedFilename);
                embeddingService.processAndIndexDocument(document, filePath, fileExtension);

                // Update status to INDEXED
                document.setStatus(DocumentStatus.INDEXED);
                document.setUpdatedAt(LocalDateTime.now());
                documentRepository.save(document);
                log.info("Document indexed successfully: {}", document.getId());

            } catch (Exception e) {
                // Update status to FAILED
                document.setStatus(DocumentStatus.FAILED);
                document.setUpdatedAt(LocalDateTime.now());
                documentRepository.save(document);
                log.error("Document indexing failed: {}", document.getId(), e);
                throw e;
            }

            return DocumentUploadResponse.builder()
                    .documentId(document.getId())
                    .fileName(document.getFileName())
                    .fileType(document.getFileType())
                    .fileSize(document.getFileSize())
                    .status(document.getStatus().toString())
                    .createdAt(document.getCreatedAt())
                    .message("Document uploaded and indexed successfully")
                    .build();

        } catch (Exception e) {
            log.error("Document upload failed for userId: {}", userId, e);
            throw new ServiceProvisioningException("Document upload failed: " + e.getMessage());
        }
    }

    public DocumentEntity getDocument(Long documentId) {
        Long userId = securityUtils.getCurrentUserId();

        return documentRepository.findByIdAndUserId(documentId, userId).orElseThrow(() ->
                new ServiceProvisioningException("Document not found"));
    }

    public List<DocumentEntity> getMyDocuments() {
        Long userId = securityUtils.getCurrentUserId();
        return documentRepository.findAllByUserId(userId);
    }

    public void deleteDocument(Long documentId) {
        Long userId = securityUtils.getCurrentUserId();

        DocumentEntity document = documentRepository
                .findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new ServiceProvisioningException("Document not found"));

        // Delete embeddings from vector store
        embeddingService.deleteDocumentEmbeddings(documentId);

        // Delete from database
        documentRepository.delete(document);

        log.info("Document deleted: {} for userId: {}", documentId, userId);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
}

