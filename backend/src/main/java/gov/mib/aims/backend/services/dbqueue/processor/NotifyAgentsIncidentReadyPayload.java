package gov.mib.aims.backend.services.dbqueue.processor;

import gov.mib.aims.backend.services.dbqueue.QueuePayload;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload уведомления оперативных агентов о готовности инцидента к выполнению.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotifyAgentsIncidentReadyPayload extends QueuePayload {

    public static final String QUEUE_NAME = "NOTIFY_AGENTS_INCIDENT_READY";

    private long incidentId;
}
