package ai.controller;

import ai.dto.DocumentUploadResponse;
import ai.model.DocumentEntity;
import ai.serviceImpl.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<DocumentUploadResponse> uploadDocument(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(documentService.uploadDocument(file));
    }

    @GetMapping
    public List<DocumentEntity> getDocuments() {
        return documentService.getMyDocuments();
    }

    @GetMapping("/{documentId}")
    public DocumentEntity getDocument(@PathVariable Long documentId) {
        return documentService.getDocument(documentId);
    }

    @DeleteMapping("/{documentId}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long documentId) {
        documentService.deleteDocument(documentId);
        return ResponseEntity.noContent().build();
    }
}