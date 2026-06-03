package gov.mib.aims.backend.repository;

import gov.mib.aims.backend.entity.AppUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    /**
     * Возвращает всех пользователей с указанной ролью.
     *
     * @param roleName имя роли
     * @return список пользователей
     */
    @Query(value = """
            SELECT DISTINCT u.* FROM app_user u
            INNER JOIN user_role ur ON ur.user_id = u.id
            INNER JOIN role r ON r.id = ur.role_id
            WHERE r.name = :roleName
            """, nativeQuery = true)
    List<AppUserEntity> findAllByRoleName(@Param("roleName") String roleName);
}
