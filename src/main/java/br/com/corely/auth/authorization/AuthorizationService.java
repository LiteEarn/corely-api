package br.com.corely.auth.authorization;

import br.com.corely.user.User;
import br.com.corely.user.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthorizationService {

    public boolean hasRole(UserRole... roles) {
        return Arrays.stream(roles).anyMatch(this::hasRole);
    }

    public boolean hasRole(UserRole role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return false;

        String expectedAuthority = "ROLE_" + role.name();
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(expectedAuthority::equals);
    }

    public boolean hasAnyRole(UserRole... roles) {
        return hasRole(roles);
    }

    public boolean hasPermission(Permission permission) {
        User user = getCurrentUser();
        if (user == null) return false;
        return RolePermissions.hasPermission(user.getRole(), permission);
    }

    public boolean hasAnyPermission(Permission... permissions) {
        User user = getCurrentUser();
        if (user == null) return false;
        Set<Permission> userPermissions = RolePermissions.getPermissions(user.getRole());
        return Arrays.stream(permissions).anyMatch(userPermissions::contains);
    }

    public Set<Permission> getCurrentUserPermissions() {
        User user = getCurrentUser();
        if (user == null) return Set.of();
        return RolePermissions.getPermissions(user.getRole());
    }

    public Set<String> getCurrentUserPermissionsAsString() {
        return getCurrentUserPermissions().stream()
                .map(Permission::name)
                .collect(Collectors.toSet());
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof User) return (User) principal;
        return null;
    }
}
