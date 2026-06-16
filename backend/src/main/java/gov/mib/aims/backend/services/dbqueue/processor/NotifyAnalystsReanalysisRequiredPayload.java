package gov.mib.aims.backend.services.dbqueue.processor;

import gov.mib.aims.backend.services.dbqueue.QueuePayload;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload уведомления аналитиков о необходимости повторного анализа.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotifyAnalystsReanalysisRequiredPayload extends QueuePayload {

    public static final String QUEUE_NAME = "NOTIFY_ANALYSTS_REANALYSIS_REQUIRED";

    private long incidentId;
    private String commentExcerpt;
}
