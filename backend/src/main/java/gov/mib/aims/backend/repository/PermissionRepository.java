package gov.mib.aims.backend.repository;

import gov.mib.aims.backend.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Репозиторий разрешений.
 */
public interface PermissionRepository extends JpaRepository<PermissionEntity, Long> {

    /**
     * Возвращает все разрешения пользователя одним запросом через join-таблицы.
     *
     * @param userId идентификатор пользователя
     * @return список разрешений
     */
    @Query(value = """
            SELECT DISTINCT p.* FROM permission p
            INNER JOIN role_permission rp ON rp.permission_id = p.id
            INNER JOIN user_role ur ON ur.role_id = rp.role_id
            WHERE ur.user_id = :userId
            """, nativeQuery = true)
    List<PermissionEntity> findAllByUserId(@Param("userId") Long userId);
}
