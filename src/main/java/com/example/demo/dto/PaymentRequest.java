package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequest {
    @NotNull(message = "Booking ID is required")
    private Integer bookingId;

    @NotBlank(message = "Payment method is required")
    private String paymentMethod; // CASH, BANK_TRANSFER, CREDIT_CARD
}
