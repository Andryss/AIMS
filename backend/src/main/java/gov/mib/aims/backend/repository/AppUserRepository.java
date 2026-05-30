package gov.mib.aims.backend.repository;

import gov.mib.aims.backend.entity.AppUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Репозиторий пользователей.
 */
public interface AppUserRepository extends JpaRepository<AppUserEntity, Long> {

    /**
     * Находит пользователя по логину.
     *
     * @param login логин
     * @return пользователь, если найден
     */
    Optional<AppUserEntity> findByLogin(String login);
}
