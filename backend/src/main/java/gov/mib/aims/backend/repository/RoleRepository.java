package gov.mib.aims.backend.repository;

import gov.mib.aims.backend.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Репозиторий ролей.
 */
public interface RoleRepository extends JpaRepository<RoleEntity, Long> {

    /**
     * Возвращает все роли пользователя одним запросом через join-таблицу user_role.
     *
     * @param userId идентификатор пользователя
     * @return список ролей
     */
    @Query(value = """
            SELECT r.* FROM role r
            INNER JOIN user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = :userId
            """, nativeQuery = true)
    List<RoleEntity> findAllByUserId(@Param("userId") Long userId);
}
