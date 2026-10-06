package de.mtala.notificationservice.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @KafkaListener(topics = "notificationTopic")
    public void listen(String message) {
        System.out.println("Received " + message);
    }
}
