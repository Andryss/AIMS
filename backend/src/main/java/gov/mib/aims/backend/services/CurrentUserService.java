package gov.mib.aims.backend.services;

import gov.mib.aims.backend.entity.AppUserEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Сервис текущего аутентифицированного пользователя.
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final AppUserRepository appUserRepository;

    /**
     * Возвращает id текущего пользователя.
     *
     * @return идентификатор пользователя
     */
    public Long getCurrentUserId() {
        String login = getCurrentLogin();
        AppUserEntity user = appUserRepository.findByLogin(login)
                .orElseThrow(() -> Errors.userNotFound(login));
        return user.getId();
    }

    private String getCurrentLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw Errors.unauthorized();
        }
        return authentication.getName();
    }
}
