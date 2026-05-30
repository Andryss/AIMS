package gov.mib.aims.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.ErrorObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Возвращает JSON ErrorObject при ошибке аутентификации (401).
 */
@Component
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        var error = Errors.unauthorized();
        ErrorObject body = new ErrorObject()
                .code(error.getCode())
                .message(error.getMessage())
                .humanMessage(error.getHumanMessage());
        response.setStatus(error.getCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
