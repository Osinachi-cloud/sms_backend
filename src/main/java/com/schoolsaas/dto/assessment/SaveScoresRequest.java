package com.schoolsaas.dto.assessment;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
public class SaveScoresRequest {
    private List<ScoreEntry> scores;

    @Data
    public static class ScoreEntry {
        private UUID studentId;
        private BigDecimal score;
        private String remarks;
    }
}
