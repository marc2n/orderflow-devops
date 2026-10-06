package de.mtala.orderservice.event;

import java.time.Instant;
import java.util.UUID;

public record OrderEvent(
        UUID eventId,
        String eventType,
        int schemaVersion,
        Instant occurredAt,
        Long orderId
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;
    public static final String ORDER_CREATED = "ORDER_CREATED";
    public static final String ORDER_DELETED = "ORDER_DELETED";

    public OrderEvent {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId is required");
        }
        if (!ORDER_CREATED.equals(eventType)
                && !ORDER_DELETED.equals(eventType)) {
            throw new IllegalArgumentException("Unsupported eventType: " + eventType);
        }
        if (schemaVersion != CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException(
                    "Unsupported schemaVersion: " + schemaVersion);
        }
        if (occurredAt == null) {
            throw new IllegalArgumentException("occurredAt is required");
        }
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("orderId must be positive");
        }
    }
}
