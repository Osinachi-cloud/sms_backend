package com.schoolsaas.service;

import com.schoolsaas.model.ContentItem;
import com.schoolsaas.repository.ContentItemRepository;
import com.schoolsaas.repository.LibraryBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final ContentItemRepository contentItemRepository;
    private final LibraryBookRepository libraryBookRepository;

    public List<Map<String, Object>> search(UUID schoolId, String query) {
        List<Map<String, Object>> results = new ArrayList<>();
        String q = query.toLowerCase();

        // Search CMS content
        List<ContentItem> contents = contentItemRepository.findBySchoolId(schoolId);
        for (ContentItem item : contents) {
            if (item.getTitle() != null && item.getTitle().toLowerCase().contains(q) ||
                item.getBody() != null && item.getBody().toLowerCase().contains(q)) {
                Map<String, Object> r = new HashMap<>();
                r.put("type", "CONTENT");
                r.put("id", item.getId());
                r.put("title", item.getTitle());
                r.put("contentType", item.getContentType());
                r.put("status", item.getStatus());
                results.add(r);
            }
        }

        // Search library books
        libraryBookRepository.findBySchoolIdAndTitleStartingWithIgnoreCaseAndIsActiveTrue(schoolId, query).forEach(book -> {
            Map<String, Object> r = new HashMap<>();
            r.put("type", "BOOK");
            r.put("id", book.getId());
            r.put("title", book.getTitle());
            r.put("author", book.getAuthor());
            r.put("fileType", book.getFileType());
            results.add(r);
        });

        return results.stream()
                .sorted((a, b) -> ((String) a.get("title")).compareTo((String) b.get("title")))
                .collect(Collectors.toList());
    }
}
