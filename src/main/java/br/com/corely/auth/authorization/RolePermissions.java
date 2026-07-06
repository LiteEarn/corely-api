package br.com.corely.auth.authorization;

import br.com.corely.user.UserRole;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class RolePermissions {

    private static final Map<UserRole, Set<Permission>> ROLE_PERMISSIONS = new EnumMap<>(UserRole.class);

    static {
        ROLE_PERMISSIONS.put(UserRole.OWNER, EnumSet.allOf(Permission.class));

        ROLE_PERMISSIONS.put(UserRole.ADMIN, EnumSet.of(
                Permission.DASHBOARD_VIEW,
                Permission.STUDENT_READ, Permission.STUDENT_WRITE,
                Permission.INSTRUCTOR_READ, Permission.INSTRUCTOR_WRITE,
                Permission.CLASS_GROUP_READ, Permission.CLASS_GROUP_WRITE,
                Permission.ENROLLMENT_READ, Permission.ENROLLMENT_WRITE,
                Permission.ATTENDANCE_READ, Permission.ATTENDANCE_WRITE,
                Permission.SESSION_READ, Permission.SESSION_WRITE,
                Permission.OBJECTIVE_READ, Permission.OBJECTIVE_WRITE,
                Permission.EVALUATION_READ, Permission.EVALUATION_WRITE,
                Permission.EVOLUTION_READ, Permission.EVOLUTION_WRITE,
                Permission.MAKEUP_REQUEST_READ, Permission.MAKEUP_REQUEST_WRITE,
                Permission.FINANCIAL_READ, Permission.FINANCIAL_WRITE,
                Permission.USER_READ, Permission.USER_WRITE,
                Permission.STUDIO_READ, Permission.STUDIO_WRITE
        ));

        ROLE_PERMISSIONS.put(UserRole.RECEPTIONIST, EnumSet.of(
                Permission.DASHBOARD_VIEW,
                Permission.STUDENT_READ, Permission.STUDENT_WRITE,
                Permission.ENROLLMENT_READ, Permission.ENROLLMENT_WRITE,
                Permission.ATTENDANCE_READ, Permission.ATTENDANCE_WRITE,
                Permission.SESSION_READ, Permission.SESSION_WRITE,
                Permission.CLASS_GROUP_READ,
                Permission.MAKEUP_REQUEST_READ, Permission.MAKEUP_REQUEST_WRITE
        ));

        ROLE_PERMISSIONS.put(UserRole.INSTRUCTOR, EnumSet.of(
                Permission.DASHBOARD_VIEW,
                Permission.OBJECTIVE_READ, Permission.OBJECTIVE_WRITE,
                Permission.EVALUATION_READ, Permission.EVALUATION_WRITE,
                Permission.EVOLUTION_READ, Permission.EVOLUTION_WRITE,
                Permission.STUDENT_READ,
                Permission.CLASS_GROUP_READ,
                Permission.SESSION_READ, Permission.SESSION_WRITE,
                Permission.ATTENDANCE_READ, Permission.ATTENDANCE_WRITE
        ));

        ROLE_PERMISSIONS.put(UserRole.FINANCIAL, EnumSet.of(
                Permission.DASHBOARD_VIEW,
                Permission.FINANCIAL_READ, Permission.FINANCIAL_WRITE,
                Permission.STUDENT_READ,
                Permission.ENROLLMENT_READ
        ));

        ROLE_PERMISSIONS.put(UserRole.STUDENT, EnumSet.of(
                Permission.OBJECTIVE_READ,
                Permission.EVALUATION_READ,
                Permission.EVOLUTION_READ
        ));
    }

    public static Set<Permission> getPermissions(UserRole role) {
        Set<Permission> permissions = ROLE_PERMISSIONS.get(role);
        return permissions != null ? EnumSet.copyOf(permissions) : EnumSet.noneOf(Permission.class);
    }

    public static boolean hasPermission(UserRole role, Permission permission) {
        Set<Permission> permissions = ROLE_PERMISSIONS.get(role);
        return permissions != null && permissions.contains(permission);
    }

    private RolePermissions() {
    }
}
