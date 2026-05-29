package com.schoolsaas.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<UserPrincipal> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return Optional.of((UserPrincipal) authentication.getPrincipal());
        }
        return Optional.empty();
    }

    public static UUID getCurrentUserId() {
        return getCurrentUser()
                .map(UserPrincipal::getId)
                .orElseThrow(() -> new IllegalStateException("No authenticated user found"));
    }

    public static UUID getCurrentSchoolId() {
        return getCurrentUser()
                .map(UserPrincipal::getCurrentSchoolId)
                .orElse(null);
    }

    public static boolean isPlatformAdmin() {
        return getCurrentUser()
                .map(UserPrincipal::isPlatformAdmin)
                .orElse(false);
    }

    public static boolean isAppAdmin() {
        return getCurrentUser()
                .map(UserPrincipal::isAppAdmin)
                .orElse(false);
    }

    public static boolean isGeneralAdmin() {
        return getCurrentUser()
                .map(UserPrincipal::isGeneralAdmin)
                .orElse(false);
    }

    public static boolean hasPermission(String permissionKey) {
        return getCurrentUser()
                .map(user -> user.hasPermission(permissionKey))
                .orElse(false);
    }
}
