package gov.mib.aims.backend.services.dbqueue.processor;

import gov.mib.aims.backend.services.dbqueue.QueuePayload;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Payload уведомления о завершении очистки.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NotifyCleanupCompletedPayload extends QueuePayload {

    public static final String QUEUE_NAME = "NOTIFY_CLEANUP_COMPLETED";

    private long incidentId;
}
