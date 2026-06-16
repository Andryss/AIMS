package gov.mib.aims.backend.services.dbqueue.processor;

import gov.mib.aims.backend.services.dbqueue.QueuePayload;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload уведомления создателя инцидента о необходимости уточнения.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotifyOperatorClarificationRequiredPayload extends QueuePayload {

    public static final String QUEUE_NAME = "NOTIFY_OPERATOR_CLARIFICATION_REQUIRED";

    private long incidentId;
    private String commentExcerpt;
}
