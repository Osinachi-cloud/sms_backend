package com.schoolsaas.dto.holiday;

import com.schoolsaas.model.Holiday;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayBulkPreviewDto {
    private int rowNumber;
    private String name;
    private String date;
    private String holidayType;
    private String description;
    private boolean valid;
    private String error;

    public Holiday toHoliday() {
        if (!valid || name == null || date == null) return null;
        return Holiday.builder()
                .name(name.trim())
                .date(java.time.LocalDate.parse(date))
                .holidayType(holidayType != null && !holidayType.isBlank() ? holidayType.trim().toUpperCase() : "PUBLIC_HOLIDAY")
                .description(description)
                .build();
    }
}
