package pe.edu.cibertec.assignment.config.rabbitmq;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = {
        RabbitConfigurationTest.TestConfig.class,
        RabbitConfiguration.class
})
@TestPropertySource(properties = {
        "app.rabbitmq.routes.orders.exchange=orders.exchange",
        "app.rabbitmq.routes.orders.queue=orders.queue",
        "app.rabbitmq.routes.orders.routing-key=orders.created",

        "app.rabbitmq.routes.payments.exchange=payments.exchange",
        "app.rabbitmq.routes.payments.queue=payments.queue",
        "app.rabbitmq.routes.payments.routing-key=payments.created",

        "app.rabbitmq.routes.notifications.exchange=notifications.exchange",
        "app.rabbitmq.routes.notifications.queue=notifications.queue",
        "app.rabbitmq.routes.notifications.routing-key=notifications.#"
})
class RabbitConfigurationTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        ConnectionFactory connectionFactory() {
            return mock(ConnectionFactory.class);
        }
    }

    @Autowired
    private RabbitProperties rabbitProperties;

    @Autowired
    private Declarables declarables;

    @Autowired
    private MessageConverter messageConverter;

    // Inyección de Queues usando Qualifier específico
    @Autowired
    @Qualifier("ordersQueue")
    private Queue ordersQueue;

    @Autowired
    @Qualifier("paymentsQueue")
    private Queue paymentsQueue;

    @Autowired
    @Qualifier("notificationsQueue")
    private Queue notificationsQueue;

    // Inyección de TopicExchanges usando Qualifier
    @Autowired
    @Qualifier("ordersExchange")
    private TopicExchange ordersExchange;

    @Autowired
    @Qualifier("paymentsExchange")
    private TopicExchange paymentsExchange;

    // Inyección de Bindings usando Qualifier
    @Autowired
    @Qualifier("ordersBinding")
    private Binding ordersBinding;

    // Inyección de Route específica usando Qualifier
    @Autowired
    @Qualifier("ordersRoute")
    private RabbitProperties.Route ordersRouteBySuffix;

    @Autowired
    @Qualifier("orders")
    private RabbitProperties.Route ordersRouteByName;

    @Test
    void shouldLoadPropertiesCorrectly() {
        assertThat(rabbitProperties.getRoutes()).hasSize(3);

        RabbitProperties.Route orders = rabbitProperties.getRoute("orders");
        assertThat(orders.getExchange()).isEqualTo("orders.exchange");
        assertThat(orders.getQueue()).isEqualTo("orders.queue");
        assertThat(orders.getRoutingKey()).isEqualTo("orders.created");

        assertThat(rabbitProperties.getExchange("orders")).isEqualTo("orders.exchange");
        assertThat(rabbitProperties.getQueue("orders")).isEqualTo("orders.queue");
        assertThat(rabbitProperties.getRoutingKey("orders")).isEqualTo("orders.created");
    }

    @Test
    void shouldInjectQueuesUsingQualifiers() {
        assertThat(ordersQueue).isNotNull();
        assertThat(ordersQueue.getName()).isEqualTo("orders.queue");

        assertThat(paymentsQueue).isNotNull();
        assertThat(paymentsQueue.getName()).isEqualTo("payments.queue");

        assertThat(notificationsQueue).isNotNull();
        assertThat(notificationsQueue.getName()).isEqualTo("notifications.queue");
    }

    @Test
    void shouldInjectExchangesUsingQualifiers() {
        assertThat(ordersExchange).isNotNull();
        assertThat(ordersExchange.getName()).isEqualTo("orders.exchange");

        assertThat(paymentsExchange).isNotNull();
        assertThat(paymentsExchange.getName()).isEqualTo("payments.exchange");
    }

    @Test
    void shouldInjectBindingUsingQualifiers() {
        assertThat(ordersBinding).isNotNull();
        assertThat(ordersBinding.getDestination()).isEqualTo("orders.queue");
        assertThat(ordersBinding.getExchange()).isEqualTo("orders.exchange");
        assertThat(ordersBinding.getRoutingKey()).isEqualTo("orders.created");
    }

    @Test
    void shouldInjectRouteUsingQualifiers() {
        assertThat(ordersRouteBySuffix).isNotNull();
        assertThat(ordersRouteBySuffix.getExchange()).isEqualTo("orders.exchange");

        assertThat(ordersRouteByName).isNotNull();
        assertThat(ordersRouteByName.getQueue()).isEqualTo("orders.queue");
    }

    @Test
    void shouldProvideDeclarables() {
        assertThat(declarables).isNotNull();
        // 3 routes * 3 (exchange, queue, binding) = 9 declarables
        assertThat(declarables.getDeclarables()).hasSize(9);
    }

    @Test
    void shouldConfigureJsonMessageConverter() {
        assertThat(messageConverter).isInstanceOf(JacksonJsonMessageConverter.class);
    }
}
