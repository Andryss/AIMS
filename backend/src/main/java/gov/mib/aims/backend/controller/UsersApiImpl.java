package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.api.UsersApi;
import gov.mib.aims.backend.generated.model.BatchUsersRequest;
import gov.mib.aims.backend.generated.model.BatchUsersResponse;
import gov.mib.aims.backend.generated.model.UserSearchResponse;
import gov.mib.aims.backend.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-контроллер пользователей.
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class UsersApiImpl implements UsersApi {

    private final UserService userService;

    @Override
    @PreAuthorize("hasAuthority('USER_READ')")
    public UserSearchResponse searchUsers(String q, String role) {
        log.info("GET /api/v1/users/search q={} role={}", q, role);
        return userService.searchUsers(q, role);
    }

    @Override
    @PreAuthorize("hasAuthority('USER_READ')")
    public BatchUsersResponse batchUsers(@Valid BatchUsersRequest batchUsersRequest) {
        log.info("POST /api/v1/users/batch ids={}", batchUsersRequest.getIds() != null
                ? batchUsersRequest.getIds().size() : 0);
        return userService.batchUsers(batchUsersRequest);
    }
}
