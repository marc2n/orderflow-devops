package de.mtala.orderservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRequest {
    @NotBlank(message = "customerName must not be blank")
    @Size(max = 255, message = "customerName must not exceed 255 characters")
    private String customerName;

    @NotBlank(message = "productName must not be blank")
    @Size(max = 255, message = "productName must not exceed 255 characters")
    private String productName;

    @NotNull(message = "quantity is required")
    @Positive(message = "quantity must be greater than zero")
    private Integer quantity;
}
