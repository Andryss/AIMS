package gov.mib.aims.backend.services.dbqueue.processor;

import gov.mib.aims.backend.services.dbqueue.QueuePayload;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload уведомления о готовности инцидента к выполнению (PREPARED).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotifyIncidentPreparedPayload extends QueuePayload {

    public static final String QUEUE_NAME = "NOTIFY_INCIDENT_PREPARED";

    private long incidentId;
}
