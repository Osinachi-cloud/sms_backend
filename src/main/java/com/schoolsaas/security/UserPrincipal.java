package com.schoolsaas.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@Data
@Builder
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private UUID id;
    private String email;
    private String password;
    private String fullName;
    private String platformRole;
    private UUID currentSchoolId;
    private UUID currentRoleId;
    private Set<String> permissions;
    private boolean active;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>();

        if (platformRole != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + platformRole));
        }

        if (permissions != null) {
            permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
        }

        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }

    public boolean isPlatformAdmin() {
        return "APP_ADMIN".equals(platformRole) || "GENERAL_ADMIN".equals(platformRole);
    }

    public boolean isAppAdmin() {
        return "APP_ADMIN".equals(platformRole);
    }

    public boolean isGeneralAdmin() {
        return "GENERAL_ADMIN".equals(platformRole);
    }

    public boolean hasPermission(String permissionKey) {
        if (isPlatformAdmin()) {
            return true;
        }
        return permissions != null && permissions.contains(permissionKey);
    }
}
