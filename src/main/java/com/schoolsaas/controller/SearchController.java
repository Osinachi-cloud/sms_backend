package com.schoolsaas.controller;

import com.schoolsaas.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/schools/{schoolId}/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> search(@PathVariable UUID schoolId, @RequestParam String q) {
        return ResponseEntity.ok(searchService.search(schoolId, q));
    }
}
