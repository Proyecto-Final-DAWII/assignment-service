package pe.edu.cibertec.assignment.config.rabbitmq;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RabbitMqTemplateTest {

    private ConnectionFactory connectionFactory;
    private RabbitMqTemplate template;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class DummyPayload {
        private String orderId;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @NoArgsConstructor
    @SuperBuilder
    static class DummyOrderEvent extends RabbitMqDto<DummyPayload> {
    }

    @BeforeEach
    void setUp() {
        connectionFactory = mock(ConnectionFactory.class);
        template = spy(new RabbitMqTemplate(connectionFactory));
        RequestContextHolder.resetRequestAttributes();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldThrowExceptionWhenMessageDoesNotExtendRabbitMqDto() {
        String invalidMessage = "Hello World";

        assertThatThrownBy(() -> template.convertAndSend("exchange", "routingKey", invalidMessage))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El mensaje enviado a RabbitMQ debe heredar obligatoriamente de RabbitMqDto<?>");
    }

    @Test
    void shouldAutocompleteFieldsInRabbitMqDtoWhenSent() {
        DummyOrderEvent event = DummyOrderEvent.builder()
                .data(new DummyPayload("ORD-123"))
                .message("Test message")
                .build();

        doNothing().when(template).send(any(), any(), any(), any());

        template.convertAndSend("orders.exchange", "orders.success", event);

        assertThat(event.getTraceId()).isNotBlank();
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getOperation()).isEqualTo("orders.success");
        assertThat(event.getType()).isEqualTo("SUCCESS");
        assertThat(event.getMessage()).isEqualTo("Test message");
        assertThat(event.getData().getOrderId()).isEqualTo("ORD-123");
    }

    @Test
    void shouldExtractTraceIdFromHttpRequestHeader() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getHeader("X-Trace-Id")).thenReturn("http-trace-id-abc-123");

        ServletRequestAttributes attributes = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(attributes);

        DummyOrderEvent event = DummyOrderEvent.builder()
                .data(new DummyPayload("ORD-100"))
                .build();

        doNothing().when(template).send(any(), any(), any(), any());

        template.convertAndSend("orders.exchange", "orders.created", event);

        assertThat(event.getTraceId()).isEqualTo("http-trace-id-abc-123");
    }

    @Test
    void shouldExtractTraceIdFromTraceparentHeader() {
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getHeader("traceparent")).thenReturn("00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");

        ServletRequestAttributes attributes = new ServletRequestAttributes(mockRequest);
        RequestContextHolder.setRequestAttributes(attributes);

        DummyOrderEvent event = DummyOrderEvent.builder()
                .data(new DummyPayload("ORD-200"))
                .build();

        doNothing().when(template).send(any(), any(), any(), any());

        template.convertAndSend("orders.exchange", "orders.created", event);

        assertThat(event.getTraceId()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
    }

    @Test
    void shouldDetectErrorTypeFromRoutingKey() {
        DummyOrderEvent event = DummyOrderEvent.builder()
                .data(new DummyPayload("ORD-999"))
                .build();

        doNothing().when(template).send(any(), any(), any(), any());

        template.convertAndSend("orders.exchange", "orders.error", event);

        assertThat(event.getType()).isEqualTo("ERROR");
    }

    @Test
    void shouldAddCustomHeadersAndTraceIdHeaderWhenProvided() {
        DummyOrderEvent event = DummyOrderEvent.builder()
                .data(new DummyPayload("ORD-555"))
                .build();

        Map<String, Object> headers = Map.of(
                "X-Tenant", "tenant-1"
        );

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        doNothing().when(template).send(any(), any(), messageCaptor.capture(), any());

        template.convertAndSend("orders.exchange", "orders.created", event, headers);

        assertThat(messageCaptor.getValue()).isNotNull();
        assertThat(messageCaptor.getValue().getMessageProperties().getHeaders())
                .containsEntry("X-Tenant", "tenant-1")
                .containsKey("X-Trace-Id");
        String actualTraceId = messageCaptor.getValue().getMessageProperties().getHeader("X-Trace-Id");
        assertThat(actualTraceId).isEqualTo(event.getTraceId());
    }

    @Test
    void shouldSupportRouteObjectDirectly() {
        RabbitProperties.Route route = new RabbitProperties.Route(
                "orders.exchange",
                "orders.queue",
                "orders.created"
        );

        DummyOrderEvent event = DummyOrderEvent.builder()
                .data(new DummyPayload("ORD-777"))
                .build();

        doNothing().when(template).send(any(), any(), any(), any());

        template.convertAndSend(route, event);

        assertThat(event.getOperation()).isEqualTo("orders.created");
        assertThat(event.getType()).isEqualTo("EVENT");
    }
}
