package gov.mib.aims.backend.services.tender;

import gov.mib.aims.backend.generated.model.ReceiveTenderRequest;
import gov.mib.aims.backend.generated.model.TenderReceiptResponse;

/**
 * Приёмка полной поставки оборудования по тендеру.
 */
public interface TenderReceiptService {

    /**
     * Регистрирует поставку на складе и завершает тендер.
     */
    TenderReceiptResponse receive(Long id, ReceiveTenderRequest request);
}
