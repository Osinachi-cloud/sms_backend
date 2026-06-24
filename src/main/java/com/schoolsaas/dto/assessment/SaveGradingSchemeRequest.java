package com.schoolsaas.dto.assessment;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class SaveGradingSchemeRequest {
    private UUID classId;
    private UUID subjectId;
    private UUID termId;
    private List<Entry> entries;

    @Data
    public static class Entry {
        private String sourceType; // QUIZ or ASSESSMENT
        private UUID sourceId;
        private Integer weight;
        private Boolean active;
    }
}
