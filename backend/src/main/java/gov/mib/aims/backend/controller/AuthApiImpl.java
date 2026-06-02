package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.api.AuthApi;
import gov.mib.aims.backend.generated.model.AuthMeResponse;
import gov.mib.aims.backend.generated.model.SignInRequest;
import gov.mib.aims.backend.generated.model.SignInResponse;
import gov.mib.aims.backend.services.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-контроллер аутентификации.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AuthApiImpl implements AuthApi {

    private final AuthService authService;

    @Override
    public SignInResponse signIn(SignInRequest signInRequest) {
        log.info("POST /api/v1/auth/signin login={}", signInRequest.getLogin());
        return authService.signIn(signInRequest);
    }

    @Override
    public AuthMeResponse getAuthMe() {
        log.info("GET /api/v1/auth/me");
        return authService.getAuthMe();
    }
}
