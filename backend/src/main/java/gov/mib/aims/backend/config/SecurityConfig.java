package gov.mib.aims.backend.config;

import gov.mib.aims.backend.exception.BaseException;
import gov.mib.aims.backend.services.ObjectMapperWrapper;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.ErrorObject;
import gov.mib.aims.backend.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

/**
 * Конфигурация Spring Security.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Обработчик 401 — пользователь не аутентифицирован.
     *
     * @param objectMapper сериализатор JSON
     * @return точка входа аутентификации
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint(ObjectMapperWrapper objectMapper) {
        return (request, response, authException) ->
                writeErrorObject(response, Errors.unauthorized(), objectMapper);
    }

    /**
     * Обработчик 403 — доступ запрещён.
     *
     * @param objectMapper сериализатор JSON
     * @return обработчик отказа в доступе
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler(ObjectMapperWrapper objectMapper) {
        return (request, response, accessDeniedException) ->
                writeErrorObject(response, Errors.accessDenied(), objectMapper);
    }

    /**
     * Настраивает цепочку фильтров безопасности.
     *
     * @param http HTTP security
     * @param jwtAuthenticationFilter фильтр JWT
     * @param authenticationEntryPoint обработчик 401
     * @param accessDeniedHandler обработчик 403
     * @return цепочка фильтров
     * @throws Exception при ошибке конфигурации
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            AuthenticationEntryPoint authenticationEntryPoint,
            AccessDeniedHandler accessDeniedHandler
    ) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/signin").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()
                        .requestMatchers("/api/v1/files", "/api/v1/files/**").authenticated()
                        .requestMatchers("/api/v1/notifications", "/api/v1/notifications/**").authenticated()
                        .requestMatchers("/api/v1/**").denyAll())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Кодировщик паролей BCrypt.
     *
     * @return BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private static void writeErrorObject(
            HttpServletResponse response,
            BaseException error,
            ObjectMapperWrapper objectMapper
    ) throws IOException {
        ErrorObject body = new ErrorObject()
                .code(error.getCode())
                .message(error.getMessage())
                .humanMessage(error.getHumanMessage());
        response.setStatus(error.getCode());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
