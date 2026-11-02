package gov.mib.aims.backend.services;

import gov.mib.aims.backend.generated.model.AlienResponse;
import gov.mib.aims.backend.generated.model.AlienSearchResponse;
import gov.mib.aims.backend.generated.model.AlienListResponse;
import gov.mib.aims.backend.generated.model.SaveAlienRequest;

/**
 * Сервис справочника инопланетян.
 */
public interface AlienService {

    /**
     * Возвращает страницу базы знаний.
     *
     * @param q поисковая строка или null для всего списка
     * @param page номер страницы с нуля
     * @param size размер страницы
     * @return страница видов
     */
    AlienListResponse list(String q, int page, int size);

    /**
     * Создаёт запись вида.
     *
     * @param request сведения о виде
     * @return созданная запись
     */
    AlienResponse create(SaveAlienRequest request);

    /**
     * Изменяет сведения, сохраняя идентификатор вида.
     *
     * @param id идентификатор вида
     * @param request сведения о виде
     * @return обновлённая запись
     */
    AlienResponse update(Long id, SaveAlienRequest request);

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
