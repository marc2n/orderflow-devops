package de.mtala.orderservice.controller;

import de.mtala.orderservice.dto.OrderError;
import de.mtala.orderservice.dto.OrderRequest;
import de.mtala.orderservice.dto.OrderResponse;
import de.mtala.orderservice.service.OrderService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create")
    @ResponseStatus(HttpStatus.CREATED)
    @CircuitBreaker(name = "orderServiceCircuitBreaker", fallbackMethod = "fallbackMethod")
    public ResponseEntity<OrderResponse> placeOrder(@RequestBody OrderRequest orderRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(orderRequest));
    }

    @GetMapping("/all")
    public ResponseEntity<List<OrderResponse>> getAllOrders() {
        return ResponseEntity.ok(orderService.getOrders());
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long orderNumber) {
        return ResponseEntity.ok(orderService.getOrderById(orderNumber));
    }

    public ResponseEntity<OrderError> fallbackMethod(OrderRequest orderRequest, RuntimeException runtimeException) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(OrderError.builder()
                        .message("Oops!!, something went wrong")
                        .build());
    }
}
