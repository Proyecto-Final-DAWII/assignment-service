package pe.edu.cibertec.assignment.messaging.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import pe.edu.cibertec.assignment.config.rabbitmq.RabbitMqDto;

import java.time.LocalDateTime;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AuditEvent extends RabbitMqDto<Void> {

    private String assetId;
    private String userId;
    private String actionType;
    private String previousState;
    private String newState;
    private String details;
    private LocalDateTime eventDate;
}

