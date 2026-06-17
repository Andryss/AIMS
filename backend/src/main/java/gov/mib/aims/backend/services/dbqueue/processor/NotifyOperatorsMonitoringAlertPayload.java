package gov.mib.aims.backend.services.dbqueue.processor;

import gov.mib.aims.backend.services.dbqueue.QueuePayload;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload задачи уведомления операторов о новом алерте мониторинга.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotifyOperatorsMonitoringAlertPayload extends QueuePayload {

    public static final String QUEUE_NAME = "NOTIFY_OPERATORS_MONITORING_ALERT";

    private long monitoringAlertId;
}
