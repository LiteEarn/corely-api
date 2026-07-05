package br.com.corely.auth.authorization;

import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import br.com.corely.user.User;
import br.com.corely.user.UserRepository;
import br.com.corely.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthorizationServiceTest {

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudioRepository studioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Studio studio;

    @BeforeEach
    void setUp() {
        studio = new Studio();
        studio.setName("Test Studio");
        studio.setActive(true);
        studio = studioRepository.save(studio);
    }

    private void authenticateAs(UserRole role) {
        User user = new User();
        user.setName(role.name() + " User");
        user.setEmail(role.name().toLowerCase() + "@test.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setRole(role);
        user.setActive(true);
        user.setStudio(studio);
        user = userRepository.save(user);

        var authorities = user.getAuthorities();
        var auth = new UsernamePasswordAuthenticationToken(user, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void hasRole_shouldReturnTrue_whenAdminRole() {
        authenticateAs(UserRole.ADMIN);
        assertTrue(authorizationService.hasRole(UserRole.ADMIN));
    }

    @Test
    void hasRole_shouldReturnFalse_whenWrongRole() {
        authenticateAs(UserRole.ADMIN);
        assertFalse(authorizationService.hasRole(UserRole.INSTRUCTOR));
    }

    @Test
    void hasAnyRole_shouldReturnTrue_whenOneMatches() {
        authenticateAs(UserRole.RECEPTIONIST);
        assertTrue(authorizationService.hasAnyRole(UserRole.ADMIN, UserRole.RECEPTIONIST));
    }

    @Test
    void hasAnyRole_shouldReturnFalse_whenNoneMatch() {
        authenticateAs(UserRole.STUDENT);
        assertFalse(authorizationService.hasAnyRole(UserRole.ADMIN, UserRole.INSTRUCTOR));
    }

    @Test
    void hasPermission_shouldReturnTrue_whenAdminHasStudentPermission() {
        authenticateAs(UserRole.ADMIN);
        assertTrue(authorizationService.hasPermission(Permission.STUDENT_READ));
    }

    @Test
    void hasPermission_shouldReturnFalse_whenStudentDoesNotHaveWritePermission() {
        authenticateAs(UserRole.STUDENT);
        assertFalse(authorizationService.hasPermission(Permission.STUDENT_WRITE));
    }

    @Test
    void getCurrentUserPermissions_shouldReturnAdminPermissions() {
        authenticateAs(UserRole.ADMIN);
        var permissions = authorizationService.getCurrentUserPermissions();
        assertTrue(permissions.contains(Permission.STUDENT_READ));
        assertTrue(permissions.contains(Permission.INSTRUCTOR_READ));
        assertTrue(permissions.contains(Permission.FINANCIAL_READ));
    }

    @Test
    void getCurrentUserPermissions_shouldReturnStudentPermissions() {
        authenticateAs(UserRole.STUDENT);
        var permissions = authorizationService.getCurrentUserPermissions();
        assertTrue(permissions.contains(Permission.OBJECTIVE_READ));
        assertFalse(permissions.contains(Permission.STUDENT_WRITE));
    }

    @Test
    void hasRole_shouldReturnFalse_whenNotAuthenticated() {
        SecurityContextHolder.clearContext();
        assertFalse(authorizationService.hasRole(UserRole.ADMIN));
    }

    @Test
    void hasPermission_shouldReturnFalse_whenNotAuthenticated() {
        SecurityContextHolder.clearContext();
        assertFalse(authorizationService.hasPermission(Permission.DASHBOARD_VIEW));
    }

    @Test
    void permissionMapping_shouldBeCorrect_forOwner() {
        var permissions = RolePermissions.getPermissions(UserRole.OWNER);
        assertTrue(permissions.containsAll(EnumSet.allOf(Permission.class)));
    }

    @Test
    void permissionMapping_shouldBeCorrect_forReceptionist() {
        var permissions = RolePermissions.getPermissions(UserRole.RECEPTIONIST);
        assertTrue(permissions.contains(Permission.STUDENT_READ));
        assertTrue(permissions.contains(Permission.STUDENT_WRITE));
        assertTrue(permissions.contains(Permission.ENROLLMENT_READ));
        assertFalse(permissions.contains(Permission.INSTRUCTOR_READ));
        assertFalse(permissions.contains(Permission.FINANCIAL_READ));
    }

    @Test
    void permissionMapping_shouldBeCorrect_forInstructor() {
        var permissions = RolePermissions.getPermissions(UserRole.INSTRUCTOR);
        assertTrue(permissions.contains(Permission.OBJECTIVE_READ));
        assertTrue(permissions.contains(Permission.OBJECTIVE_WRITE));
        assertTrue(permissions.contains(Permission.STUDENT_READ));
        assertFalse(permissions.contains(Permission.STUDENT_WRITE));
    }

    @Test
    void hasAnyPermission_shouldReturnTrue_whenAnyMatches() {
        authenticateAs(UserRole.FINANCIAL);
        assertTrue(authorizationService.hasAnyPermission(Permission.FINANCIAL_READ, Permission.STUDENT_READ));
    }

    @Test
    void hasAnyPermission_shouldReturnFalse_whenNoneMatch() {
        authenticateAs(UserRole.FINANCIAL);
        assertFalse(authorizationService.hasAnyPermission(Permission.INSTRUCTOR_WRITE, Permission.CLASS_GROUP_WRITE));
    }
}
