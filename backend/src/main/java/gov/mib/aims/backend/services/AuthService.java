package gov.mib.aims.backend.services;

import gov.mib.aims.backend.entity.AppUserEntity;
import gov.mib.aims.backend.entity.PermissionEntity;
import gov.mib.aims.backend.entity.RoleEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.AuthMeResponse;
import gov.mib.aims.backend.generated.model.SignInRequest;
import gov.mib.aims.backend.generated.model.SignInResponse;
import gov.mib.aims.backend.repository.AppUserRepository;
import gov.mib.aims.backend.repository.PermissionRepository;
import gov.mib.aims.backend.repository.RoleRepository;
import gov.mib.aims.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Сервис аутентификации и профиля текущего пользователя.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Выполняет вход по логину и паролю.
     *
     * @param request данные входа
     * @return JWT access token
     */
    @Transactional(readOnly = true)
    public SignInResponse signIn(SignInRequest request) {
        AppUserEntity user = appUserRepository.findByLogin(request.getLogin())
                .orElseThrow(() -> Errors.invalidCredentials());
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw Errors.invalidCredentials();
        }
        String token = jwtService.generateToken(user.getLogin());
        return new SignInResponse().accessToken(token);
    }

    /**
     * Возвращает профиль текущего аутентифицированного пользователя.
     *
     * @return логин, роли и разрешения
     */
    @Transactional(readOnly = true)
    public AuthMeResponse getAuthMe() {
        String login = getCurrentLogin();
        AppUserEntity user = appUserRepository.findByLogin(login)
                .orElseThrow(() -> Errors.userNotFound(login));
        List<String> roles = roleRepository.findAllByUserId(user.getId()).stream()
                .map(RoleEntity::getName)
                .toList();
        List<String> permissions = permissionRepository.findAllByUserId(user.getId()).stream()
                .map(PermissionEntity::getCode)
                .toList();
        return new AuthMeResponse()
                .login(login)
                .roles(roles)
                .permissions(permissions);
    }

    private String getCurrentLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw Errors.unauthorized();
        }
        return authentication.getName();
    }
}
