package de.mtala.orderservice.service;

import de.mtala.orderservice.dto.OrderRequest;
import de.mtala.orderservice.dto.OrderResponse;
import de.mtala.orderservice.model.Order;
import de.mtala.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private static final String TOPIC = "notificationTopic";
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderResponse placeOrder(OrderRequest orderRequest) {
        log.info("orderRequest: {}", orderRequest);
        Order order = Order.builder()
                .customerName(orderRequest.getCustomerName())
                .productName(orderRequest.getProductName())
                .quantity(orderRequest.getQuantity())
                .build();

        orderRepository.save(order);

        log.info("Order: {}", order);
        String orderNumberAsString = order.getId().toString();

        log.info("Order placed with order number: {}", orderNumberAsString);
        sendOrderNotification(orderNumberAsString);
        return OrderResponse.builder()
                .customerName(order.getCustomerName())
                .productName(order.getProductName())
                .quantity(order.getQuantity())
                .createdAt(order.getCreatedAt())
                .build();
    }

    public void sendOrderNotification(String orderEvent) {
        log.info("Sending order notification for order number: {}", orderEvent);
        kafkaTemplate.send(TOPIC, orderEvent);
    }
}
