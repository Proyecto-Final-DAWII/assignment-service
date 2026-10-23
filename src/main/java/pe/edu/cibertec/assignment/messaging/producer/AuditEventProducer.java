package pe.edu.cibertec.assignment.messaging.producer;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import pe.edu.cibertec.assignment.config.rabbitmq.RabbitProperties;
import pe.edu.cibertec.assignment.messaging.event.AuditEvent;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AuditEventProducer {

    private final RabbitTemplate auditEventRabbitTemplate;
    private final RabbitProperties rabbitProperties;

    public void publish(AuditEvent event) {
        if (event.getEventDate() == null) {
            event.setEventDate(LocalDateTime.now());
        }

        RabbitProperties.Route route =
                rabbitProperties.getRoutes().get("audit");

        if (route == null) {
            throw new IllegalStateException(
                    "No existe la ruta de auditoría en RabbitMQ"
            );
        }

        auditEventRabbitTemplate.convertAndSend(
                route.getExchange(),
                route.getRoutingKey(),
                event
        );
    }
}
