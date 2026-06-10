package gov.mib.aims.backend.services;

import gov.mib.aims.backend.generated.model.AlienResponse;
import gov.mib.aims.backend.generated.model.AlienSearchResponse;

/**
 * Сервис справочника инопланетян.
 */
public interface AlienService {

    /**
     * Поиск записей по подстроке в названии или описании.
     *
     * @param query поисковый запрос
     * @return список совпадений
     */
    AlienSearchResponse search(String query);

    /**
     * Возвращает запись по id.
     *
     * @param id идентификатор
     * @return запись справочника
     */
    AlienResponse getById(Long id);
}
