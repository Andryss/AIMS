package gov.mib.aims.backend.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * Аутентификация machine-клиента внешней системы мониторинга по API key.
 */
public class IntegrationAuthentication extends AbstractAuthenticationToken {

    public static final String PRINCIPAL_NAME = "external-monitoring";
    public static final String INGEST_AUTHORITY = "MONITORING_EVENT_INGEST";

    public IntegrationAuthentication() {
        super(List.of(new SimpleGrantedAuthority(INGEST_AUTHORITY)));
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return PRINCIPAL_NAME;
    }
}
