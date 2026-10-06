package de.mtala.notificationservice.service;

import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class NotificationServiceTest {

    private static final String EVENT_ID = "2f1f13b7-e04c-4f77-bc71-4ab655c136df";

    private static final String VALID_JSON = """
            {
              "eventId": "2f1f13b7-e04c-4f77-bc71-4ab655c136df",
              "eventType": "ORDER_CREATED",
              "schemaVersion": 1,
              "occurredAt": "2026-01-01T12:00:00Z",
              "orderId": 42
            }
            """;

    @Autowired
    private NotificationService service;

    static Stream<String> invalidPayloads() {
        return Stream.of(
                "not-json",
                VALID_JSON.replace("\"" + EVENT_ID + "\"", "null"),
                VALID_JSON.replace("\"" + EVENT_ID + "\"", "\"invalid-uuid\""),
                VALID_JSON.replace("\"ORDER_CREATED\"", "\"ORDER_UPDATED\""),
                VALID_JSON.replace("\"schemaVersion\": 1", "\"schemaVersion\": 2"),
                VALID_JSON.replace("\"schemaVersion\": 1,", ""),
                VALID_JSON.replace("\"2026-01-01T12:00:00Z\"", "null"),
                VALID_JSON.replace("\"orderId\": 42", "\"orderId\": 0"),
                VALID_JSON.replace("\"orderId\": 42", "\"orderId\": -1"),
                VALID_JSON.replace("\"orderId\": 42", "\"orderId\": null")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"ORDER_CREATED", "ORDER_DELETED"})
    void logsValidEvents(String eventType, CapturedOutput output) {
        String payload = VALID_JSON.replace("ORDER_CREATED", eventType);

        service.listen(payload);

        assertThat(output.getOut())
                .contains("Notification:")
                .contains("eventId=" + EVENT_ID)
                .contains("eventType=" + eventType)
                .contains("orderId=42")
                .contains("occurredAt=2026-01-01T12:00:00Z");
    }

    @ParameterizedTest
    @MethodSource("invalidPayloads")
    void rejectsInvalidEvents(String payload, CapturedOutput output) {
        assertThatThrownBy(() -> service.listen(payload)).isInstanceOf(RuntimeException.class);

        assertThat(output.getOut()).doesNotContain("Notification:");
    }
}
