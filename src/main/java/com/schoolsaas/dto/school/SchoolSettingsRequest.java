package com.schoolsaas.dto.school;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SchoolSettingsRequest {
    private String schoolName;
    private String email;
    private String phone;
    private String address;
    private String logoUrl;
    private String primaryColor;
    private String secondaryColor;
    private String accentColor;
    private String currency;
    private String timezone;
    private List<Map<String, Object>> gradingScale;
    private List<Map<String, Object>> paymentAccounts;
    private List<Map<String, Object>> feeItems;
}
