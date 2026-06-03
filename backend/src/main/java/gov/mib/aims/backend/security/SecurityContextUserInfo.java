package gov.mib.aims.backend.security;

import gov.mib.aims.backend.exception.Errors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Чтение {@link UserInfo} из SecurityContext.
 */
public final class SecurityContextUserInfo {

    private SecurityContextUserInfo() {
    }

    /**
     * Возвращает текущего пользователя из SecurityContext.
     *
     * @return id и login аутентифицированного пользователя
     */
    public static UserInfo requireCurrentUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw Errors.unauthorized();
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserInfo userInfo)) {
            throw Errors.unauthorized();
        }
        return userInfo;
    }
}
