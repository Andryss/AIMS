package gov.mib.aims.backend.services.dbqueue.processor;

import gov.mib.aims.backend.services.dbqueue.QueuePayload;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload задачи уведомления аналитиков о готовности инцидента.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotifyAnalystsIncidentReadyPayload extends QueuePayload {

    public static final String QUEUE_NAME = "NOTIFY_ANALYSTS_INCIDENT_READY";

    private long incidentId;
}
