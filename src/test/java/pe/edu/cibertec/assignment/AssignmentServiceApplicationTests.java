package pe.edu.cibertec.assignment;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static org.mockito.Mockito.mock;

@SpringBootTest
@Import(AssignmentServiceApplicationTests.TestConfig.class)
class AssignmentServiceApplicationTests {

    @TestConfiguration
    static class TestConfig {
        @Bean
        ConnectionFactory connectionFactory() {
            return mock(ConnectionFactory.class);
        }
    }

    @Test
    void contextLoads() {
    }

}
