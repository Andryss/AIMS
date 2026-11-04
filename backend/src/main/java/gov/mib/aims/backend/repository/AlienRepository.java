package gov.mib.aims.backend.repository;

import gov.mib.aims.backend.entity.AlienEntity;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Репозиторий справочника инопланетян.
 */
public interface AlienRepository extends JpaRepository<AlienEntity, Long> {

    /**
     * Находит вид по внутреннему ключу обученной модели.
     */
    Optional<AlienEntity> findByClassificationKey(String classificationKey);

    /**
     * Поиск по названию и описанию (name-match выше description-match).
     *
     * @param query поисковая подстрока в нижнем регистре
     * @param pageable параметры страницы
     * @return страница справочника
     */
    @Query(value = """
            SELECT * FROM alien
            WHERE strpos(lower(name), :query) > 0 OR strpos(lower(description), :query) > 0
            ORDER BY CASE WHEN strpos(lower(name), :query) > 0 THEN 0 ELSE 1 END, lower(name), id
            """, nativeQuery = true)
    Page<AlienEntity> search(@Param("query") String query, Pageable pageable);
}
