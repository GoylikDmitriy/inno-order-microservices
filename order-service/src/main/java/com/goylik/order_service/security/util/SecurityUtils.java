package com.goylik.order_service.security.util;

import com.goylik.order_service.exception.AccessDeniedException;
import com.goylik.order_service.security.UserPrincipal;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecurityUtils {
    public static Long getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            throw new AccessDeniedException("No authenticated user found");
        }
        return principal.userId();
    }
}
