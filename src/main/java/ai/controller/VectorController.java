package ai.controller;

import ai.serviceImpl.VectorService;
import lombok.RequiredArgsConstructor;

import org.springframework.ai.document.Document;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vector")
@RequiredArgsConstructor
public class VectorController {

    private final VectorService vectorService;

    @PostMapping("/add")
    public ResponseEntity<String> addDocument() {
        vectorService.addDocument();
        vectorService.loadKnowledge();
        return ResponseEntity.ok("Document added");
    }

    @GetMapping("/search")
    public List<Document> search(
            @RequestParam(name = "q") String query) {

        return vectorService.search(query);
    }
}