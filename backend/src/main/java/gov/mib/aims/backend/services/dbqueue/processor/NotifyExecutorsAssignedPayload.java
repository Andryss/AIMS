package gov.mib.aims.backend.services.dbqueue.processor;

import gov.mib.aims.backend.services.dbqueue.QueuePayload;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Payload уведомления новым исполнителям инцидента.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotifyExecutorsAssignedPayload extends QueuePayload {

    public static final String QUEUE_NAME = "NOTIFY_EXECUTORS_ASSIGNED";

    private long incidentId;
    private List<Long> executorUserIds = new ArrayList<>();
}
