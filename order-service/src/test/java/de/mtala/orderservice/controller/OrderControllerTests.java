package de.mtala.orderservice.controller;

import de.mtala.orderservice.dto.OrderRequest;
import de.mtala.orderservice.dto.OrderResponse;
import de.mtala.orderservice.exception.OrderNotFoundException;
import de.mtala.orderservice.service.OrderService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    static Stream<Arguments> invalidRequests() {
        Stream.Builder<Arguments> cases = Stream.builder();

        for (String field : List.of("customerName", "productName")) {
            Map<String, Object> missing = validRequest();
            missing.remove(field);
            cases.add(Arguments.of(field + " missing", missing));

            Map<String, Object> nullValue = validRequest();
            nullValue.put(field, null);
            cases.add(Arguments.of(field + " null", nullValue));

            Map<String, Object> empty = validRequest();
            empty.put(field, "");
            cases.add(Arguments.of(field + " empty", empty));

            Map<String, Object> blank = validRequest();
            blank.put(field, " \t\n ");
            cases.add(Arguments.of(field + " whitespace only", blank));

            Map<String, Object> tooLong = validRequest();
            tooLong.put(field, "x".repeat(256));
            cases.add(Arguments.of(field + " exceeds 255 characters", tooLong));
        }

        Map<String, Object> missingQuantity = validRequest();
        missingQuantity.remove("quantity");
        cases.add(Arguments.of("quantity missing", missingQuantity));

        Map<String, Object> nullQuantity = validRequest();
        nullQuantity.put("quantity", null);
        cases.add(Arguments.of("quantity null", nullQuantity));

        for (int quantity : List.of(0, -1)) {
            Map<String, Object> invalidQuantity = validRequest();
            invalidQuantity.put("quantity", quantity);
            cases.add(Arguments.of(
                    "quantity is " + quantity, invalidQuantity));
        }

        return cases.build();
    }

    private static Map<String, Object> validRequest() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("customerName", "Test Customer");
        request.put("productName", "Test Product");
        request.put("quantity", 2);
        return request;
    }

    private static OrderResponse response(long id) {
        return OrderResponse.builder()
                .id(id)
                .customerName("Test Customer")
                .productName("Test Product")
                .quantity(2)
                .createdAt(Instant.parse("2026-10-06T12:00:00Z"))
                .build();
    }

    @Test
    void creatingValidOrderReturns201AndOrderId() throws Exception {
        when(orderService.placeOrder(any(OrderRequest.class)))
                .thenReturn(response(42L));

        mockMvc.perform(post("/api/order/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.customerName").value("Test Customer"))
                .andExpect(jsonPath("$.productName").value("Test Product"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.createdAt")
                        .value("2026-10-06T12:00:00Z"));

        verify(orderService).placeOrder(argThat(request ->
                "Test Customer".equals(request.getCustomerName())
                        && "Test Product".equals(request.getProductName())
                        && Integer.valueOf(2).equals(request.getQuantity())));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRequests")
    void invalidRequestReturns400WithoutCallingService(
            String description,
            Map<String, Object> request) throws Exception {

        mockMvc.perform(post("/api/order/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    void maximumNameLengthsAndQuantityOneAreAccepted() throws Exception {
        Map<String, Object> request = validRequest();
        request.put("customerName", "c".repeat(255));
        request.put("productName", "p".repeat(255));
        request.put("quantity", 1);

        when(orderService.placeOrder(any(OrderRequest.class)))
                .thenReturn(response(42L));

        mockMvc.perform(post("/api/order/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(orderService).placeOrder(argThat(submitted ->
                submitted.getCustomerName().length() == 255
                        && submitted.getProductName().length() == 255
                        && Integer.valueOf(1).equals(submitted.getQuantity())));
    }

    @Test
    void malformedJsonReturns400WithoutCallingService() throws Exception {
        mockMvc.perform(post("/api/order/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(orderService);
    }

    @Test
    void missingBodyReturns400WithoutCallingService() throws Exception {
        mockMvc.perform(post("/api/order/create")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    void gettingOrderIncludesIdInJsonResponse() throws Exception {
        when(orderService.getOrderById(42L)).thenReturn(response(42L));

        mockMvc.perform(get("/api/order/42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42));

        verify(orderService).getOrderById(42L);
    }

    @Test
    void listingOrdersIncludesEachIdInJsonResponse() throws Exception {
        when(orderService.getOrders())
                .thenReturn(List.of(response(42L), response(43L)));

        mockMvc.perform(get("/api/order/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(42))
                .andExpect(jsonPath("$[1].id").value(43));

        verify(orderService).getOrders();
    }

    @Test
    void gettingMissingOrderReturns404ProblemDetail() throws Exception {
        when(orderService.getOrderById(42L))
                .thenThrow(new OrderNotFoundException(42L));

        mockMvc.perform(get("/api/order/42"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail")
                        .value("Order 42 was not found."))
                .andExpect(jsonPath("$.instance").value("/api/order/42"));
    }

    @Test
    void deletingMissingOrderReturns404ProblemDetail() throws Exception {
        doThrow(new OrderNotFoundException(42L))
                .when(orderService).deleteOrder(42L);

        mockMvc.perform(delete("/api/order/delete/42"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail")
                        .value("Order 42 was not found."));
    }

    @Test
    void deletingExistingOrderReturns204WithoutBody() throws Exception {
        mockMvc.perform(delete("/api/order/delete/42"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(orderService).deleteOrder(42L);
    }

    @Test
    void invalidQuantityReturnsFieldValidationDetails() throws Exception {
        Map<String, Object> request = validRequest();
        request.put("quantity", 0);

        mockMvc.perform(post("/api/order/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail")
                        .value("One or more request fields are invalid."))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("quantity"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("quantity must be greater than zero"))
                .andExpect(jsonPath("$.errors[0].rejectedValue")
                        .doesNotExist());

        verifyNoInteractions(orderService);
    }

    @Test
    void nonNumericOrderIdReturns400WithoutCallingService() throws Exception {
        mockMvc.perform(get("/api/order/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(orderService);
    }


    @Test
    void unexpectedFailureReturnsSafe500ProblemDetail() throws Exception {
        when(orderService.placeOrder(any(OrderRequest.class)))
                .thenThrow(new IllegalStateException(
                        "Internal database connection details"));

        mockMvc.perform(post("/api/order/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail")
                        .value("An unexpected error occurred."))
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(content().string(
                        org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString(
                                        "Internal database connection details"))));
    }
}
