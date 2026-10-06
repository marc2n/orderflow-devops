package de.mtala.notificationservice.service;

import de.mtala.notificationservice.event.OrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-events-v1")
    public void listen(String payload) {
        OrderEvent event = objectMapper.readValue(payload, OrderEvent.class);

        log.info(
                "Notification: eventId={}, eventType={}, orderId={}, occurredAt={}",
                event.eventId(),
                event.eventType(),
                event.orderId(),
                event.occurredAt()
        );
    }
}
