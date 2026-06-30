package sm.selflearn.samskrtam.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.user.model.UserRole;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class RoleResolver {

    public Set<UserRole> fromRoleNames(List<String> roleNames) {
        Set<UserRole> roles = new HashSet<>();
        if (roleNames != null) {
            if (roleNames.contains("ADMIN")) {
                roles.add(UserRole.ADMIN);
            }
            roles.add(UserRole.STUDENT);
        } else {
            roles.add(UserRole.STUDENT);
        }
        log.debug("Resolved roles from role names: {} -> {}", roleNames, roles);
        return roles;
    }

    public Set<UserRole> fromKeycloakMap(Map<String, Object> keycloakUser) {
        @SuppressWarnings("unchecked")
        List<String> realmRoles = (List<String>) keycloakUser.get("realmRoles");
        log.debug("Resolving roles from Keycloak realmRoles: {}", realmRoles);
        return fromRoleNames(realmRoles);
    }

    public Set<UserRole> fromJwt(Map<String, Object> realmAccess) {
        if (realmAccess != null && realmAccess.containsKey("roles")) {
            @SuppressWarnings("unchecked")
            List<String> jwtRoles = (List<String>) realmAccess.get("roles");
            log.debug("Resolving roles from JWT realm_access roles: {}", jwtRoles);
            return fromRoleNames(jwtRoles);
        }
        log.debug("No realm_access in JWT, resolving default roles");
        return fromRoleNames(null);
    }
}