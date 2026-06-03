package gov.mib.aims.backend.services.dbqueue.processor;

import gov.mib.aims.backend.services.dbqueue.QueuePayload;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Payload для тестовой очереди DUMMY_TASK.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DummyTaskPayload extends QueuePayload {

    public static final String QUEUE_NAME = "DUMMY_TASK";

    private String message;
}
