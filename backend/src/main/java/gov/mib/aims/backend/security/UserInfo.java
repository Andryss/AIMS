package gov.mib.aims.backend.security;

/**
 * Идентификация аутентифицированного пользователя (principal в SecurityContext).
 */
public record UserInfo(Long id, String login) {
}
