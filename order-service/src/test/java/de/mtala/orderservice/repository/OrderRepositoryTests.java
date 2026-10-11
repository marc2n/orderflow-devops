package de.mtala.orderservice.repository;

import de.mtala.orderservice.model.Order;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class OrderRepositoryTests {

    @Autowired
    private OrderRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void sameCustomerCanPlaceSeparateOrdersForTheSameProduct() {
        Order first = repository.saveAndFlush(newOrder());
        Order second = repository.saveAndFlush(newOrder());

        Long firstId = first.getId();
        Long secondId = second.getId();

        assertThat(firstId).isNotNull();
        assertThat(secondId).isNotNull().isNotEqualTo(firstId);

        // Force subsequent reads to come from the database.
        entityManager.clear();

        Order persistedFirst = repository.findById(firstId).orElseThrow();
        Order persistedSecond = repository.findById(secondId).orElseThrow();

        assertThat(persistedFirst.getProductName()).isEqualTo("Keyboard");
        assertThat(persistedSecond.getProductName()).isEqualTo("Keyboard");
        assertThat(persistedFirst.getCustomerName()).isEqualTo("Test Customer");
        assertThat(persistedSecond.getCustomerName()).isEqualTo("Test Customer");
        assertThat(persistedFirst.getQuantity()).isEqualTo(1);
        assertThat(persistedSecond.getQuantity()).isEqualTo(1);

        // Deleting one order must leave the other order intact.
        repository.deleteById(firstId);
        repository.flush();
        entityManager.clear();

        assertThat(repository.findById(firstId)).isEmpty();
        assertThat(repository.findById(secondId)).isPresent();
    }

    private Order newOrder() {
        return Order.builder()
                .customerName("Test Customer")
                .productName("Keyboard")
                .quantity(1)
                .build();
    }
}
