package com.goylik.user_service.util.security;

import com.goylik.user_service.exception.user.AccessDeniedException;
import com.goylik.user_service.security.UserPrincipal;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecurityUtils {
    public static UserPrincipal getCurrentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AccessDeniedException("No authenticated user found");
        }

        return principal;
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().userId();
    }
}
