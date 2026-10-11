package de.mtala.orderservice.service;

import de.mtala.orderservice.dto.OrderRequest;
import de.mtala.orderservice.dto.OrderResponse;
import de.mtala.orderservice.model.Order;
import de.mtala.orderservice.repository.OrderRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class OrderServiceEventTests {

    @Autowired
    private ObjectMapper objectMapper;

    private OrderRepository repository;
    private KafkaTemplate<String, String> kafkaTemplate;
    private OrderService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        repository = mock(OrderRepository.class);
        kafkaTemplate = mock(KafkaTemplate.class);
        service = new OrderService(repository, kafkaTemplate, objectMapper);
    }

    @Test
    void creatingOrderPublishesVersionedJsonWithOrderIdAsKey() {
        Instant createdAt = Instant.parse("2026-10-06T12:00:00Z");

        // Return a separate entity to verify that the service uses save()'s result.
        Order savedOrder = Order.builder()
                .id(42L)
                .customerName("Test Customer")
                .productName("Test Product")
                .quantity(2)
                .createdAt(createdAt)
                .build();

        when(repository.save(any(Order.class))).thenReturn(savedOrder);
        stubPendingPublication();

        OrderRequest request = OrderRequest.builder()
                .customerName("Test Customer")
                .productName("Test Product")
                .quantity(2)
                .build();

        Instant before = Instant.now();
        OrderResponse response = service.placeOrder(request);
        Instant after = Instant.now();

        ArgumentCaptor<Order> submittedOrder =
                ArgumentCaptor.forClass(Order.class);
        verify(repository).save(submittedOrder.capture());

        assertThat(submittedOrder.getValue().getId()).isNull();
        assertThat(submittedOrder.getValue().getCustomerName())
                .isEqualTo("Test Customer");
        assertThat(submittedOrder.getValue().getProductName())
                .isEqualTo("Test Product");
        assertThat(submittedOrder.getValue().getQuantity()).isEqualTo(2);

        assertThat(response.getId()).isEqualTo(42L);
        assertThat(response.getCustomerName()).isEqualTo("Test Customer");
        assertThat(response.getProductName()).isEqualTo("Test Product");
        assertThat(response.getQuantity()).isEqualTo(2);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);

        assertPublishedEvent("ORDER_CREATED", 42L, before, after);
    }

    @Test
    void deletingExistingOrderPublishesDeletedEvent() {
        Order order = Order.builder().id(42L).build();
        when(repository.findById(42L)).thenReturn(Optional.of(order));
        stubPendingPublication();

        Instant before = Instant.now();
        service.deleteOrder(42L);
        Instant after = Instant.now();

        verify(repository).delete(order);
        assertPublishedEvent("ORDER_DELETED", 42L, before, after);
    }

    @Test
    void deletingMissingOrderDoesNotPublishAnEvent() {
        when(repository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteOrder(42L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Order not found");

        verify(repository).findById(42L);
        verify(repository, never()).delete(any(Order.class));
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void savingFailureDoesNotPublishAnEvent() {
        OrderRequest request = OrderRequest.builder()
                .customerName("Test Customer")
                .productName("Test Product")
                .quantity(2)
                .build();

        RuntimeException failure = new RuntimeException("Database unavailable");

        when(repository.save(any(Order.class))).thenThrow(failure);

        assertThatThrownBy(() -> service.placeOrder(request)).isSameAs(failure);

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void deletingFailureDoesNotPublishAnEvent() {
        Order order = Order.builder().id(42L).build();
        when(repository.findById(42L)).thenReturn(Optional.of(order));

        RuntimeException failure = new RuntimeException("Database unavailable");

        doThrow(failure).when(repository).delete(order);

        assertThatThrownBy(() -> service.deleteOrder(42L)).isSameAs(failure);

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void gettingOrderReturnsItsIdAndDetailsWithoutPublishingAnEvent() {
        Instant createdAt = Instant.parse("2026-10-06T12:00:00Z");

        Order order = Order.builder()
                .id(42L)
                .customerName("Test Customer")
                .productName("Test Product")
                .quantity(2)
                .createdAt(createdAt)
                .build();

        when(repository.findById(42L)).thenReturn(Optional.of(order));

        OrderResponse response = service.getOrderById(42L);

        assertThat(response.getId()).isEqualTo(42L);
        assertThat(response.getCustomerName()).isEqualTo("Test Customer");
        assertThat(response.getProductName()).isEqualTo("Test Product");
        assertThat(response.getQuantity()).isEqualTo(2);
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);

        verify(repository).findById(42L);
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void listingOrdersReturnsEachOrderIdWithoutPublishingEvents() {
        Order first = Order.builder()
                .id(42L)
                .customerName("Customer A")
                .productName("Product A")
                .quantity(1)
                .build();

        Order second = Order.builder()
                .id(43L)
                .customerName("Customer B")
                .productName("Product B")
                .quantity(3)
                .build();

        when(repository.findAll()).thenReturn(List.of(first, second));

        List<OrderResponse> responses = service.getOrders();

        assertThat(responses)
                .extracting(OrderResponse::getId)
                .containsExactlyInAnyOrder(42L, 43L);

        verify(repository).findAll();
        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void listingOrdersReturnsEmptyListWhenNoOrdersExist() {
        when(repository.findAll()).thenReturn(List.of());

        assertThat(service.getOrders()).isEmpty();

        verifyNoInteractions(kafkaTemplate);
    }

    private void stubPendingPublication() {
        CompletableFuture<SendResult<String, String>> pending = new CompletableFuture<>();

        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(pending);
    }

    private void assertPublishedEvent(String expectedType, Long expectedOrderId, Instant before, Instant after) {
        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);

        verify(kafkaTemplate).send(
                eq("order-events-v1"),
                eq(expectedOrderId.toString()),
                payload.capture()
        );
        verifyNoMoreInteractions(kafkaTemplate);

        var json = objectMapper.readTree(payload.getValue());

        // Verify the wire contract independently of the producer's record.
        assertThat(json.isObject()).isTrue();
        assertThat(json.size()).isEqualTo(5);

        assertThat(json.path("eventType").isString()).isTrue();
        assertThat(json.path("eventType").asText()).isEqualTo(expectedType);

        assertThat(json.path("schemaVersion").isIntegralNumber()).isTrue();
        assertThat(json.path("schemaVersion").asInt()).isEqualTo(1);

        assertThat(json.path("orderId").isIntegralNumber()).isTrue();
        assertThat(json.path("orderId").asLong()).isEqualTo(expectedOrderId.longValue());

        assertThat(json.path("eventId").isString()).isTrue();
        UUID eventId = UUID.fromString(json.path("eventId").asText());
        assertThat(eventId.version()).isEqualTo(4);

        assertThat(json.path("occurredAt").isString()).isTrue();
        Instant occurredAt = Instant.parse(json.path("occurredAt").asText());
        assertThat(occurredAt).isBetween(before, after);
    }
}
