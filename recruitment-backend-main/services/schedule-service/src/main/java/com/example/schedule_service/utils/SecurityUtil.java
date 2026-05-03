package com.example.schedule_service.utils;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class SecurityUtil {

    public static Optional<String> getCurrentUserLogin() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(extractPrincipal(securityContext.getAuthentication()));
    }

    private static String extractPrincipal(Authentication authentication) {
        if (authentication == null) {
            return null;
        } else if (authentication.getPrincipal() instanceof UserDetails springSecurityUser) {
            return springSecurityUser.getUsername();
        } else if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        } else if (authentication.getPrincipal() instanceof String s) {
            return s;
        }
        return null;
    }

    public static Optional<String> getCurrentUserJWT() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            // Trường hợp Resource Server JWT
            return Optional.of(jwtAuth.getToken().getTokenValue());
        } else if (authentication != null && authentication.getCredentials() instanceof String token) {
            // Trường hợp UsernamePasswordAuthenticationToken (ở login)
            return Optional.of(token);
        }
        return Optional.empty();
    }

    public static Long extractEmployeeId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null)
            return null;

        // Trường hợp phổ biến: JwtAuthenticationToken
        if (auth instanceof JwtAuthenticationToken token) {
            Object user = token.getTokenAttributes().get("user");
            if (user instanceof java.util.Map<?, ?> map) {
                // Token giờ lưu employeeId
                Object employeeId = map.get("employeeId");
                if (employeeId instanceof Number n)
                    return n.longValue();
            }
            return null;
        }

        // Một số cấu hình để principal là Jwt
        Object principal = auth.getPrincipal();
        if (principal instanceof Jwt jwt) {
            Object user = jwt.getClaim("user");
            if (user instanceof java.util.Map<?, ?> map) {
                // Token giờ lưu employeeId
                Object employeeId = map.get("employeeId");
                if (employeeId instanceof Number n)
                    return n.longValue();
            }
        }
        return null;
    }

    public static Long extractUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null)
            return null;

        // Trường hợp phổ biến: JwtAuthenticationToken
        if (auth instanceof JwtAuthenticationToken token) {
            Object user = token.getTokenAttributes().get("user");
            if (user instanceof java.util.Map<?, ?> map) {
                Object id = map.get("userId");
                if (id instanceof Number n)
                    return n.longValue();
            }
            return null;
        }

        // Một số cấu hình để principal là Jwt
        Object principal = auth.getPrincipal();
        if (principal instanceof Jwt jwt) {
            Object user = jwt.getClaim("user");
            if (user instanceof java.util.Map<?, ?> map) {
                Object id = map.get("userId");
                if (id instanceof Number n)
                    return n.longValue();
            }
        }
        return null;
    }
    // public static boolean isAuthenticated(){
    // Authentication authentication =
    // SecurityContextHolder.getContext().getAuthentication();
    // return authentication!= null &&
    // getAuthorities(authentication).noneMatch(AuthoritiesConstants.ANOMYMOUS::equal)
    // ;
    // }

    // public static boolean hasCurrentUserAnyOfAuthorities(String... authorities){
    // Authentication authentication =
    // SecurityContextHolder.getContext().getAuthentication();
    // return authentication!= null && ;
    // }

    // public static boolean hasCurrentUserNoneOfAuthorities(String... authorities)
    // {
    // return !hasCurrentUserAnyOfAuthorities(authorities);
    // }

    // public static boolean hasCurrentUserThisAuthority(String authority) {
    // return hasCurrentUserAnyOfAuthorities(authority);
    // }

    // public static Steam<String> getAuthorities(Authentication authentication) {
    // return
    // authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority);
    // }
}
