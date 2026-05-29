package com.schoolsaas.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomPermissionEvaluator implements PermissionEvaluator {

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            return false;
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String permissionKey = (String) permission;

        if (principal.isAppAdmin()) {
            return true;
        }

        if (principal.isGeneralAdmin()) {
            return isGeneralAdminAllowed(permissionKey);
        }

        return principal.hasPermission(permissionKey);
    }

    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId,
                                 String targetType, Object permission) {
        return hasPermission(authentication, null, permission);
    }

    private boolean isGeneralAdminAllowed(String permissionKey) {
        return permissionKey.endsWith(".read") ||
               permissionKey.startsWith("analytics.") ||
               permissionKey.equals("school.read") ||
               permissionKey.startsWith("school.users.");
    }
}
