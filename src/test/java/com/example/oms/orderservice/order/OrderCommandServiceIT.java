package com.example.oms.orderservice.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.oms.orderservice.common.idempotency.ProcessedCommandRepository;
import com.example.oms.orderservice.order.application.OrderCommandService;
import com.example.oms.orderservice.order.domain.OrderItem;
import com.example.oms.orderservice.order.infrastructure.outbox.OutboxEvent;
import com.example.oms.orderservice.order.infrastructure.outbox.OutboxEventRepository;
import com.example.oms.orderservice.order.infrastructure.repository.OrderRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

@SpringBootTest
@Testcontainers
@EnableScheduling
class OrderCommandServiceIT {

    @Autowired
    OrderCommandService orderCommandService;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    ProcessedCommandRepository processedCommandRepository;

    @Autowired
    OutboxEventRepository outboxEventRepository;

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15")
                    .withDatabaseName("order_test")
                    .withUsername("test")
                    .withPassword("test");

    @Container
    static KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0")
            );

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.kafka.bootstrap-servers",
                kafka::getBootstrapServers
        );
    }

    @BeforeEach
    void cleanDatabase() {
        outboxEventRepository.deleteAll();
        processedCommandRepository.deleteAll();
        orderRepository.deleteAll();
    }

    @Test
    void shouldCreateOrderAndWriteOutboxEvent() {

        UUID commandId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        List<OrderItem> items = List.of(
                new OrderItem(
                        UUID.randomUUID(),
                        2,
                        new BigDecimal("100.00")
                )
        );

        UUID orderId = orderCommandService.createOrder(commandId, userId, items);

        assertThat(orderRepository.findById(orderId)).isPresent();

        assertThat(processedCommandRepository.findById(commandId)).isPresent();

        assertThat(outboxEventRepository.findAll()).hasSize(1);

        OutboxEvent event = outboxEventRepository.findAll().get(0);

        assertThat(event.getAggregateId()).isEqualTo(orderId);
        assertThat(event.isPublished()).isFalse();
    }

    @Test
    void shouldBeIdempotentByCommandId() {

        UUID commandId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        List<OrderItem> items = List.of(
                new OrderItem(
                        UUID.randomUUID(),
                        1,
                        new BigDecimal("50.00")
                )
        );

        UUID firstOrderId =
                orderCommandService.createOrder(commandId, userId, items);

        UUID secondOrderId =
                orderCommandService.createOrder(commandId, userId, items);

        assertThat(secondOrderId).isEqualTo(firstOrderId);

        assertThat(orderRepository.findAll()).hasSize(1);

        assertThat(outboxEventRepository.findAll()).hasSize(1);
    }

    @Test
    void shouldPublishOrderCreatedEventToKafka() {

        KafkaConsumer<String, String> consumer = createConsumer();

        UUID commandId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        List<OrderItem> items = List.of(
                new OrderItem(
                        UUID.randomUUID(),
                        1,
                        new BigDecimal("100.00")
                )
        );

        orderCommandService.createOrder(commandId, userId, items);

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {

                    ConsumerRecords<String, String> records =
                            consumer.poll(Duration.ofMillis(500));

                    assertThat(records.count()).isGreaterThan(0);
                });
    }


    private KafkaConsumer<String, String> createConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        KafkaConsumer<String, String> consumer =
                new KafkaConsumer<>(props);

        consumer.subscribe(List.of("order-events"));
        return consumer;
    }
}