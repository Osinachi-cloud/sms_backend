package com.schoolsaas.controller;

import com.schoolsaas.dto.onboarding.OnboardingStepDto;
import com.schoolsaas.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    @GetMapping("/steps")
    public ResponseEntity<List<OnboardingStepDto>> getSteps(
            @RequestParam String page,
            @RequestParam(defaultValue = "STUDENT") String role) {
        return ResponseEntity.ok(onboardingService.getStepsForPage(page, role));
    }

    @PostMapping("/steps/{stepKey}/complete")
    public ResponseEntity<Void> completeStep(@PathVariable String stepKey) {
        onboardingService.completeStep(stepKey);
        return ResponseEntity.ok().build();
    }
}
