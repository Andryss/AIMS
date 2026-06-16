package gov.mib.aims.backend.services;

import gov.mib.aims.backend.entity.AppUserEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.BatchUsersRequest;
import gov.mib.aims.backend.generated.model.BatchUsersResponse;
import gov.mib.aims.backend.generated.model.UserSearchResponse;
import gov.mib.aims.backend.generated.model.UserSummary;
import gov.mib.aims.backend.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Реализация {@link UserService}.
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final int SEARCH_LIMIT = 20;
    private static final int BATCH_MAX = 100;

    private final AppUserRepository appUserRepository;

    @Override
    @Transactional(readOnly = true)
    public UserSearchResponse searchUsers(String query, String roleName) {
        if (query == null || query.trim().length() < 2) {
            throw Errors.validationError("Search query must be at least 2 characters");
        }
        if (roleName == null || roleName.isBlank()) {
            throw Errors.validationError("Role is required");
        }
        List<AppUserEntity> users = appUserRepository.searchByLoginAndRoleName(
                query.trim(), roleName.trim(), SEARCH_LIMIT);
        return new UserSearchResponse()
                .items(users.stream().map(this::toSummary).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BatchUsersResponse batchUsers(BatchUsersRequest request) {
        List<Long> ids = request.getIds() != null ? request.getIds() : List.of();
        if (ids.size() > BATCH_MAX) {
            throw Errors.validationError("Too many user ids (max " + BATCH_MAX + ")");
        }
        if (ids.isEmpty()) {
            return new BatchUsersResponse().items(List.of());
        }
        Map<Long, AppUserEntity> byId = new HashMap<>();
        for (AppUserEntity user : appUserRepository.findAllById(ids)) {
            byId.put(user.getId(), user);
        }
        List<UserSummary> items = new ArrayList<>();
        for (Long id : ids) {
            AppUserEntity user = byId.get(id);
            if (user != null) {
                items.add(toSummary(user));
            }
        }
        return new BatchUsersResponse().items(items);
    }

    private UserSummary toSummary(AppUserEntity user) {
        return new UserSummary()
                .id(user.getId())
                .login(user.getLogin());
    }
}
