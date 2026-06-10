package gov.mib.aims.backend.repository;

import gov.mib.aims.backend.entity.AlienEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Репозиторий справочника инопланетян.
 */
public interface AlienRepository extends JpaRepository<AlienEntity, Long> {

    /**
     * Поиск по названию и описанию (name-match выше description-match).
     *
     * @param pattern шаблон LIKE (уже с %)
     * @param limit максимум записей
     * @return список совпадений
     */
    @Query(value = """
            SELECT * FROM alien
            WHERE lower(name) LIKE :pattern OR lower(description) LIKE :pattern
            ORDER BY
                CASE WHEN lower(name) LIKE :pattern THEN 0 ELSE 1 END,
                name
            LIMIT :limit
            """, nativeQuery = true)
    List<AlienEntity> search(@Param("pattern") String pattern, @Param("limit") int limit);
}
