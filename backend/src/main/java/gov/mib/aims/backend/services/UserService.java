package gov.mib.aims.backend.services;

import gov.mib.aims.backend.generated.model.BatchUsersRequest;
import gov.mib.aims.backend.generated.model.BatchUsersResponse;
import gov.mib.aims.backend.generated.model.UserSearchResponse;

/**
 * Сервис поиска и batch-загрузки пользователей.
 */
public interface UserService {

    /**
     * Поиск пользователей по login среди пользователей с указанной ролью.
     *
     * @param query строка поиска (min 2 символа)
     * @param roleName имя роли
     * @return результаты поиска
     */
    UserSearchResponse searchUsers(String query, String roleName);

    /**
     * Batch-загрузка пользователей по id (пропускает отсутствующие).
     *
     * @param request список id
     * @return найденные пользователи в порядке запроса
     */
    BatchUsersResponse batchUsers(BatchUsersRequest request);
}
