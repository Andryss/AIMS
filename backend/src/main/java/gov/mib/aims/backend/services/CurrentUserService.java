package gov.mib.aims.backend.services;

import gov.mib.aims.backend.entity.RoleEntity;
import gov.mib.aims.backend.model.Role;
import gov.mib.aims.backend.repository.RoleRepository;
import gov.mib.aims.backend.security.UserInfo;
import gov.mib.aims.backend.security.SecurityContextUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Сервис текущего аутентифицированного пользователя.
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final RoleRepository roleRepository;

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

    /**
     * Проверяет, есть ли у текущего пользователя хотя бы одна из указанных ролей.
     *
     * @param roles роли
     * @return true, если роль найдена
     */
    public boolean hasAnyRole(Role... roles) {
        return hasAnyRole(Arrays.stream(roles).map(Role::getCode).toArray(String[]::new));
    }

    /**
     * Проверяет, есть ли у текущего пользователя хотя бы одна из указанных ролей.
     *
     * @param roleNames имена ролей
     * @return true, если роль найдена
     */
    public boolean hasAnyRole(String... roleNames) {
        Set<String> allowed = Arrays.stream(roleNames).collect(Collectors.toSet());
        return roleRepository.findAllByUserId(getCurrentUserId()).stream()
                .map(RoleEntity::getName)
                .anyMatch(allowed::contains);
    }
}
