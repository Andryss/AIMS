package gov.mib.aims.backend.services.mapping;

import gov.mib.aims.backend.entity.AppUserEntity;
import gov.mib.aims.backend.generated.model.UserSummary;
import org.springframework.stereotype.Component;

/**
 * Преобразование данных в представления API.
 */
@Component
public class UserMapper {

    /**
     * Создаёт представление, независимое от исходной сущности.
     */
    public UserSummary toSummary(AppUserEntity user) {
        return new UserSummary()
                .id(user.getId())
                .login(user.getLogin());
    }
}
