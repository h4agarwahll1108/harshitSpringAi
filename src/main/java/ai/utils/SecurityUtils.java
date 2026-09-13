package ai.utils;

import ai.dto.CustomUserPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    public Long getCurrentUserId() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof CustomUserPrincipal)) {
            throw new AccessDeniedException("Invalid authenticated user");
        }

        CustomUserPrincipal userPrincipal = (CustomUserPrincipal) principal;
        return userPrincipal.getUserId();
    }
}
