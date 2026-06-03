package gov.mib.aims.backend.services;

import gov.mib.aims.backend.security.UserInfo;
import gov.mib.aims.backend.security.SecurityContextUserInfo;
import org.springframework.stereotype.Service;

/**
 * Сервис текущего аутентифицированного пользователя.
 */
@Service
public class CurrentUserService {

    /**
     * Возвращает данные текущего пользователя из SecurityContext.
     *
     * @return id и login
     */
    public UserInfo getCurrentUserInfo() {
        return SecurityContextUserInfo.requireCurrentUserInfo();
    }

    /**
     * Возвращает id текущего пользователя.
     *
     * @return идентификатор пользователя
     */
    public Long getCurrentUserId() {
        return getCurrentUserInfo().id();
    }

    /**
     * Возвращает login текущего пользователя.
     *
     * @return логин
     */
    public String getCurrentLogin() {
        return getCurrentUserInfo().login();
    }
}
