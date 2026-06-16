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
     * Ищет пользователей по фрагменту login среди пользователей с указанной ролью.
     *
     * @param query подстрока login (без учёта регистра)
     * @param roleName имя роли
     * @param limit максимум результатов
     * @return список пользователей
     */
    @Query(value = """
            SELECT DISTINCT u.* FROM app_user u
            INNER JOIN user_role ur ON ur.user_id = u.id
            INNER JOIN role r ON r.id = ur.role_id
            WHERE LOWER(u.login) LIKE LOWER(CONCAT('%', CAST(:query AS text), '%'))
              AND r.name = :roleName
            ORDER BY u.login
            LIMIT :limit
            """, nativeQuery = true)
    List<AppUserEntity> searchByLoginAndRoleName(
            @Param("query") String query,
            @Param("roleName") String roleName,
            @Param("limit") int limit
    );

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

    /**
     * Проверяет, есть ли у пользователя указанная роль.
     *
     * @param userId идентификатор пользователя
     * @param roleName имя роли
     * @return true, если роль назначена
     */
    @Query(value = """
            SELECT COUNT(*) > 0 FROM user_role ur
            INNER JOIN role r ON r.id = ur.role_id
            WHERE ur.user_id = :userId AND r.name = :roleName
            """, nativeQuery = true)
    boolean hasRole(@Param("userId") Long userId, @Param("roleName") String roleName);
}
