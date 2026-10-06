package de.mtala.orderservice.service;

import de.mtala.orderservice.dto.OrderRequest;
import de.mtala.orderservice.dto.OrderResponse;
import de.mtala.orderservice.event.OrderEvent;
import de.mtala.orderservice.model.Order;
import de.mtala.orderservice.repository.OrderRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrderService {

    private static final String TOPIC = "order-events-v1";
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderResponse placeOrder(OrderRequest orderRequest) {
        log.info("orderRequest: {}", orderRequest);
        Order order = Order.builder()
                .customerName(orderRequest.getCustomerName())
                .productName(orderRequest.getProductName())
                .quantity(orderRequest.getQuantity())
                .build();

        orderRepository.save(order);
        publishOrderEvent(OrderEvent.ORDER_CREATED, order.getId());

        return OrderResponse.builder()
                .customerName(order.getCustomerName())
                .productName(order.getProductName())
                .quantity(order.getQuantity())
                .createdAt(order.getCreatedAt())
                .build();
    }

    public List<OrderResponse> getOrders() {
        return orderRepository.findAll().stream()
                .map(order -> OrderResponse.builder()
                        .customerName(order.getCustomerName())
                        .productName(order.getProductName())
                        .quantity(order.getQuantity())
                        .createdAt(order.getCreatedAt())
                        .build())
                .toList();
    }

    public OrderResponse getOrderById(Long orderNumber) {
        Order order = orderRepository.findById(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return OrderResponse.builder()
                .customerName(order.getCustomerName())
                .productName(order.getProductName())
                .quantity(order.getQuantity())
                .createdAt(order.getCreatedAt())
                .build();
    }

    @Transactional
    public void deleteOrder(Long orderNumber) {
        Order order = orderRepository.findById(orderNumber)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        long deletedOrderNumber = order.getId();
        orderRepository.delete(order);
        publishOrderEvent(OrderEvent.ORDER_DELETED, deletedOrderNumber);
    }

    private void publishOrderEvent(String eventType, Long orderId) {
        OrderEvent event = new OrderEvent(
                UUID.randomUUID(),
                eventType,
                OrderEvent.CURRENT_SCHEMA_VERSION,
                Instant.now(),
                orderId
        );

        String payload = objectMapper.writeValueAsString(event);

        kafkaTemplate.send(
                TOPIC,
                orderId.toString(),
                payload
        ).whenComplete((result, failure) -> {
            if (failure != null) {
                log.error(
                        "Order event publication failed: eventId={}, eventType={}, orderId={}",
                        event.eventId(),
                        event.eventType(),
                        event.orderId(),
                        failure
                );
            } else {
                log.info(
                        "Order event published: eventId={}, eventType={}, orderId={}",
                        event.eventId(),
                        event.eventType(),
                        event.orderId()
                );
            }
        });
    }
}
