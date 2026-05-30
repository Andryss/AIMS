package gov.mib.aims.backend.security;

import gov.mib.aims.backend.entity.AppUserEntity;
import gov.mib.aims.backend.entity.PermissionEntity;
import gov.mib.aims.backend.repository.AppUserRepository;
import gov.mib.aims.backend.repository.PermissionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Фильтр аутентификации по JWT в заголовке Authorization.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AppUserRepository appUserRepository;
    private final PermissionRepository permissionRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null && jwtService.isTokenValid(token)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String login = jwtService.extractLogin(token);
            appUserRepository.findByLogin(login).ifPresent(user -> setAuthentication(user, token));
        }
        filterChain.doFilter(request, response);
    }

    private void setAuthentication(AppUserEntity user, String token) {
        List<PermissionEntity> permissions = permissionRepository.findAllByUserId(user.getId());
        List<SimpleGrantedAuthority> authorities = permissions.stream()
                .map(PermissionEntity::getCode)
                .map(SimpleGrantedAuthority::new)
                .toList();
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user.getLogin(),
                token,
                authorities
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }
}
